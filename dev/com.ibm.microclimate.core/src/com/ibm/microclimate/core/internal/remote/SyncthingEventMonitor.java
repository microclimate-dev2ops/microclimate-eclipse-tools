/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.core.internal.remote;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.ibm.microclimate.core.internal.HttpUtil;
import com.ibm.microclimate.core.internal.HttpUtil.HttpResult;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.remote.AbstractDevice.ConfigInfo;

/**
 * To be notified of a particular event, register with this class
 * before performing the action that will lead to the event as this
 * class does not store events. If sharing a new folder, for example,
 * and it is necessary to know when the initial sharing is complete,
 * then register before actually sharing the folder.
 */
public class SyncthingEventMonitor implements Runnable {
	
	public static final String FOLDER_COMPLETION_TYPE = "FolderCompletion";
	public static final String DEVICE_KEY = "device";
	public static final String FOLDER_KEY = "folder";
	public static final String COMPLETION_KEY = "completion";
	
	private static final String EVENT_REST_PATH = "/rest/events";
	private static final String SINCE_PARAM = "since";
	private static final String EVENTS_PARAM = "events";
	private static final String TIMEOUT_PARAM = "timeout";
	private static final String LIMIT_PARAM = "limit";
	
	private static final int EVENT_TIMEOUT = 60000;
	private static final int REQUEST_DELAY = 1000;
	
	private ConfigInfo configInfo;
	private URI guiBaseUri;
	
	private int lastSeenId = 0;
	private boolean stopListener = false;
	
	private Map<String, List<SyncthingEventListener>> eventListeners = new HashMap<String, List<SyncthingEventListener>>();
	
	public SyncthingEventMonitor(ConfigInfo configInfo, URI guiBaseUri) {
		this.configInfo = configInfo;
		this.guiBaseUri = guiBaseUri;
	}
	
	public void stopListener() {
		stopListener = true;
	}
	
	public synchronized void addListener(SyncthingEventListener listener, String... eventTypes) {
		for (String type : eventTypes) {
			List<SyncthingEventListener> listeners = eventListeners.get(type);
			if (listeners == null) {
				listeners = new ArrayList<SyncthingEventListener>();
				eventListeners.put(type, listeners);
			}
			listeners.add(listener);
		}
	}
	
	public synchronized void removeListener(SyncthingEventListener listener, String... eventTypes) {
		for (String type : eventTypes) {
			List<SyncthingEventListener> listeners = eventListeners.get(type);
			if (listeners != null) {
				listeners.remove(listener);
				if (listeners.isEmpty()) {
					eventListeners.remove(type);
				}
			}
		}
	}

	@Override
	public void run() {
		// First find out the latest event id
		final URI uri = guiBaseUri.resolve(EVENT_REST_PATH);
//		try {
//			Map<String, Object> params = new HashMap<String, Object>();
//			params.put(SINCE_PARAM, "0");
//			params.put(LIMIT_PARAM, "1");
//			params.put(TIMEOUT_PARAM, "5000");
//			HttpResult result = HttpUtil.get(uri, AbstractDevice.getRequestProperties(configInfo), params, 10000);
//			if (result.isGoodResponse) {
//				if (result.response != null && !result.response.isEmpty()) {
//					JSONArray events = new JSONArray(result.response);
//					if (events.length() > 0) {
//						SyncthingEvent event = new SyncthingEvent(events.getJSONObject(0));
//						lastSeenId = event.id;
//					}
//				}
//			}
//		} catch (Exception e) {
//			MCLogger.logError("Exception trying to determine the last event seen", e);
//		}
		
		while (!stopListener) {
			if (eventListeners.isEmpty()) {
				try {
					Thread.sleep(REQUEST_DELAY);
				} catch (InterruptedException e) {
					// Ignore
				}
				continue;
			}
			try {
				Map<String, Object> params = new HashMap<String, Object>();
				params.put(SINCE_PARAM, String.valueOf(lastSeenId));
				params.put(EVENTS_PARAM, getTypeList());
				params.put(TIMEOUT_PARAM, String.valueOf(EVENT_TIMEOUT));
				
				HttpResult result = HttpUtil.get(uri, AbstractDevice.getRequestProperties(configInfo), params, EVENT_TIMEOUT);
				if (!result.isGoodResponse) {
					MCLogger.logError("Received a bad response when getting events: " + result.response);
					continue;
				}
				if (result.response != null && !result.response.isEmpty()) {
					JSONArray events = new JSONArray(result.response);
					for (int i = 0; i < events.length(); i++) {
						JSONObject eventJson = events.getJSONObject(i);
						SyncthingEvent event = new SyncthingEvent(eventJson);
						if (eventListeners.containsKey(event.type)) {
							for (SyncthingEventListener listener : eventListeners.get(event.type)) {
								listener.eventNotify(event);
							}
						}
						lastSeenId = event.id;
					}
				}
			} catch (Exception e) {
				MCLogger.logError("Exception while getting events for " + configInfo.deviceName, e);
			}
		}
	}
	
	private String getTypeList() {
		StringBuilder builder = new StringBuilder();
		boolean start = true;
		for (String type : eventListeners.keySet()) {
			if (start) {
				start = false;
			} else {
				builder.append(",");
			}
			builder.append(type);
		}
		return builder.toString();
	}

}
