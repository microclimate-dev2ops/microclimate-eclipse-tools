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

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.PlatformUtil;
import com.ibm.microclimate.kubeclient.util.ICPUtil;
import com.ibm.microclimate.kubeclient.util.ICPUtil.ICPPortForward;

public class ICPDevice extends AbstractDevice {
	
	private static final int GUI_PORT = 8384;
	private static final int CONNECTION_PORT = 22000;
	private static final String CONFIG_DIR = "/root/.config/syncthing";
	private static final String CONFIG_FILE = "config.xml";
	
	private static final String EDITOR_POD_SELECTOR = "app=microclimate-ibm-microclimate-admin-editor";
	private static final String BEACON_CONTAINER = "microclimate-beacon";
	
	private static final String FOLDER_DIR = "/microclimate-workspace";
	
	private final ICPConnection icp;
	private final String icpHost;
	private final ICPPortForward guiPortForward;
	private final ICPPortForward connPortForward;

	private ICPDevice(ICPConnection icp, ConfigInfo configInfo, String icpHost, String host, ICPPortForward guiPortForward, ICPPortForward connPortForward) throws IOException {
		super(configInfo, host, guiPortForward.getLocalPort(), connPortForward.getLocalPort());
		this.icp = icp;
		this.icpHost = icpHost;
		this.guiPortForward = guiPortForward;
		this.connPortForward = connPortForward;
	}

	// Start synchronization for the given folder and device.
	public void addFolderEntry(String name, AbstractDevice shareDevice) throws IOException, JSONException {
		JSONObject config = APIUtils.getConfig(guiBaseUri, configInfo.apiKey);
		JSONObject folder = getFolder(config, name);
		if (folder == null) {
			folder = createFolderEntry(name);
			config.append(FOLDERS_KEY, folder);
		} else if (isShared(folder, shareDevice.getDeviceId())) {
			return;
		}
		
		// Add the share device to the device list
		JSONObject device = new JSONObject();
		device.put(DEVICE_ID_KEY, shareDevice.getDeviceId());
		folder.append(DEVICES_KEY, device);
		
		// Save the configuration
		APIUtils.putConfig(guiBaseUri, configInfo.apiKey, config);
	}
	
	public boolean isThisDevice(String host, String namespace) {
		return icpHost.equals(host) && icp.isThisConnection(namespace);
	}
	
	// Get the folder for the given name from the list of folders
	private JSONObject getFolder(JSONObject config, String name) throws JSONException {
		JSONArray folders = config.getJSONArray("folders");
		for (int i = 0; i < folders.length(); i++) {
			JSONObject folder = folders.getJSONObject(i);
			if (name.equals(folder.getString("id"))) {
				return folder;
			}
		}
		return null;
	}
	
	// Create a folder entry based on the template
	private JSONObject createFolderEntry(String name) throws JSONException {
		JSONObject folder = new JSONObject(folderTemplate.toString());
		folder.put("id", name);
		folder.put("label", name);
		folder.put("path", FOLDER_DIR + "/" + name);
		JSONObject device = new JSONObject();
		device.put("deviceID", getDeviceId());
		folder.append("devices", device);
		return folder;
	}
	
	// Returns true if the folder is shared with the device
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

	// Create a new ICP device.  This includes setting up the port forwarding from ICP to the local host.
	public static ICPDevice createICPDevice(String host, String namespace, String tmpPath) throws IOException {
		ICPConnection icp = null;
		
		try {
			icp = new ICPConnection(namespace);
			
			// Get the pod name
			String podName = icp.getPodName(EDITOR_POD_SELECTOR);
			
			// Copy over the syncthing config file and get the config info
			String content = icp.getFileFromContainer(podName, BEACON_CONTAINER, CONFIG_DIR, CONFIG_FILE, tmpPath);
			ConfigInfo info = new ConfigInfo(content);

			// Forward the gui and connection ports
			ICPPortForward guiPortForward = ICPUtil.forwardPort(GUI_PORT);
			System.out.println("GUI port: " + guiPortForward.getLocalPort());
			ICPPortForward connPortForward = ICPUtil.forwardPort(CONNECTION_PORT);
			System.out.println("Connection port: " + connPortForward.getLocalPort());
			
			return new ICPDevice(icp, info, host, LOCALHOST, guiPortForward, connPortForward);
		} catch (Exception e) {
			if (icp != null) {
				icp.dispose();
			}
			throw new IOException("Failed to create ICP device for host: " + host, e);
		}
	}

	@Override
	public void dispose() {
		if (icp != null) {
			icp.dispose();
		}
	    disposePortForward(guiPortForward);
	    disposePortForward(connPortForward);
	}
	
	private void disposePortForward(ICPPortForward portForward) {
		if (portForward != null) {
	    	try {
				portForward.close();
			} catch (IOException e) {
				MCLogger.logError("Stopping port forwarding was unsuccessful for port: " + portForward.getLocalPort());
			}
	    }
	}
}
