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

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ibm.microclimate.core.internal.PlatformUtil;

public class ICPDevice extends AbstractDevice {
	
	private static final String GUI_PORT = "8384";
	private static final String CONNECTION_PORT = "22000";
	private static final String CONFIG_DIR = "/root/.config/syncthing";
	private static final String CONFIG_FILE = "config.xml";
	
	private static final String EDITOR_POD_SELECTOR = "app=microclimate-ibm-microclimate-admin-editor";
	private static final String BEACON_CONTAINER = "microclimate-beacon";
	
	private static final String FOLDER_DIR = "/microclimate-workspace";
	
	private final ICPConnection icp;
	
	private ICPDevice(ICPConnection icp, ConfigInfo configInfo, String host, int guiPort, int connectionPort) throws IOException {
		super(configInfo, host, guiPort, connectionPort);
		this.icp = icp;
	}

	public void addFolderEntry(String name, AbstractDevice shareDevice) throws IOException, JSONException {
		JSONObject config = APIUtils.getConfig(guiBaseUri, configInfo.apiKey);
		JSONObject folder = getFolder(config, name);
		if (folder == null) {
			folder = createFolderEntry(name);
			config.append("folders", folder);
		} else if (isShared(folder, shareDevice.configInfo.deviceId)) {
			return;
		}
		JSONObject device = new JSONObject();
		device.put("deviceID", shareDevice.configInfo.deviceId);
		folder.append("devices", device);
		APIUtils.putConfig(guiBaseUri, configInfo.apiKey, config);
	}
	
	public boolean isThisDevice(String host, String namespace) {
		return host.equals(host) && icp.isThisConnection(namespace);
	}
	
	private JSONObject getFolder(JSONObject config, String name) throws JSONException {
		JSONArray folders = config.getJSONArray("folders");
		if (folders != null) {
			for (int i = 0; i < folders.length(); i++) {
				JSONObject folder = folders.getJSONObject(i);
				if (name.equals(folder.getString("id"))) {
					return folder;
				}
			}
		}
		return null;
	}
	
	private JSONObject createFolderEntry(String name) throws JSONException {
		JSONObject folder = new JSONObject(folderTemplate.toString());
		folder.put("id", name);
		folder.put("label", name);
		folder.put("path", FOLDER_DIR + "/" + name);
		JSONObject device = new JSONObject();
		device.put("deviceID", configInfo.deviceId);
		folder.append("devices", device);
		return folder;
	}
	
	private boolean isShared(JSONObject folder, String deviceId) throws JSONException {
		JSONArray devices = folder.getJSONArray("devices");
		if (devices != null) {
			for (int i = 0; i < devices.length(); i++) {
				JSONObject device = devices.getJSONObject(i);
				if (deviceId.equals(device.get("deviceID"))) {
					return true;
				}
			}
		}
		return false;
	}

	public static ICPDevice createICPDevice(String hostIP, String namespace, String tmpPath) throws Exception {
		ICPConnection icp = null;
		
		try {
			icp = new ICPConnection(namespace);
			
			// Get the pod name
			String podName = icp.getPodName(EDITOR_POD_SELECTOR);
			
			// Copy over the syncthing config file and get the config info
			String content = icp.getFileFromContainer(podName, BEACON_CONTAINER, CONFIG_DIR, CONFIG_FILE, tmpPath);
			ConfigInfo info = new ConfigInfo(content);
			
			// Forward the gui and connection ports
			int guiPort = PlatformUtil.findFreePort();
			icp.portForward(podName, String.valueOf(guiPort), GUI_PORT);
			System.out.println("GUI port: " + guiPort);
			int connectionPort = PlatformUtil.findFreePort();
			icp.portForward(podName, String.valueOf(connectionPort), CONNECTION_PORT);
			System.out.println("Connection port: " + connectionPort);
			
			return new ICPDevice(icp, info, LOCALHOST, guiPort, connectionPort);
		} catch (Exception e) {
			if (icp != null) {
				icp.dispose();
			}
			throw e;
		}
	}

	@Override
	public void dispose() {
		if (icp != null) {
			icp.dispose();
		}
	}
}
