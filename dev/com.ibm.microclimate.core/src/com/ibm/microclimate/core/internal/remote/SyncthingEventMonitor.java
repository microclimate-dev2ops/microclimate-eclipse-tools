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

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.remote.AbstractDevice.ConfigInfo;

/**
 * To be notified of a particular event, register with this class
 * before performing the action that will lead to the event as this
 * class does not store events. If sharing a new folder, for example,
 * and it is necessary to know when the initial sharing is complete,
 * then register before actually sharing the folder.
 * 
 * Only listens to events if there is an event listener registered and
 * only listens to event types that are registered with the listeners.
 * 
 * Keeps track of the last event id it has seen so that it only requests
 * events it has not seen yet.
 */
public class SyncthingEventMonitor implements Runnable {
	
	public static final String FOLDER_COMPLETION_TYPE = "FolderCompletion";
	public static final String DEVICE_KEY = "device";
	public static final String FOLDER_KEY = "folder";
	public static final String COMPLETION_KEY = "completion";
	
	private static final int EVENT_TIMEOUT = 60000;
	private static final int REQUEST_DELAY = 1000;
	private static final int LAST_ID_TIMEOUT = 5000;
	
	private ConfigInfo configInfo;
	private URI guiBaseUri;
	
	private int lastSeenId = 0;
	private boolean stopMonitor = false;
	
	private Map<String, List<SyncthingEventListener>> eventListeners = new HashMap<String, List<SyncthingEventListener>>();
	
	public SyncthingEventMonitor(ConfigInfo configInfo, URI guiBaseUri) {
		this.configInfo = configInfo;
		this.guiBaseUri = guiBaseUri;
	}
	
	public void stopMonitor() {
		stopMonitor = true;
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
	
	private synchronized boolean hasEventListeners() {
		return !eventListeners.isEmpty();
	}

	@Override
	public void run() {
		// First find out the latest event id
//		try {
//			JSONArray events = APIUtils.getEvents(guiBaseUri, configInfo.apiKey, 0, null, 1, LAST_ID_TIMEOUT);
//			if (events.length() > 0) {
//				SyncthingEvent event = new SyncthingEvent(events.getJSONObject(0));
//				lastSeenId = event.id;
//			}
//		} catch (Exception e) {
//			MCLogger.logError("Exception trying to determine the last event seen", e);
//		}
		
		while (!stopMonitor) {
			if (!hasEventListeners()) {
				try {
					Thread.sleep(REQUEST_DELAY);
				} catch (InterruptedException e) {
					// Ignore
				}
				continue;
			}
			try {
				JSONArray events = APIUtils.getEvents(guiBaseUri, configInfo.apiKey, lastSeenId, eventListeners.keySet(), EVENT_TIMEOUT);
				if (events != null) {
					for (int i = 0; i < events.length(); i++) {
						JSONObject eventJson = events.getJSONObject(i);
						SyncthingEvent event = new SyncthingEvent(eventJson);
						if (eventListeners.containsKey(event.type)) {
							synchronized(this) {
								for (SyncthingEventListener listener : eventListeners.get(event.type)) {
									listener.eventNotify(event);
								}
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
}
