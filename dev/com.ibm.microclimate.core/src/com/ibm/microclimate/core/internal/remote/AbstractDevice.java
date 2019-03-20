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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;

public abstract class AbstractDevice {
	
	public static final String LOCALHOST = "127.0.0.1";
	
	protected static final String DEVICES_KEY = "devices";
	protected static final String DEVICE_ID_KEY = "deviceID";
	protected static final String DEVICE_NAME_KEY = "name";
	protected static final String DEVICE_ADDRESSES_KEY = "addresses";
	protected static final String DEVICE_DYNAMIC_VALUE = "dynamic";
	
	protected static final String FOLDERS_KEY = "folders";
	protected static final String FOLDER_ID_KEY = "id";
	
	protected static final String ADDR_PREFIX = "tcp://";

	// Template for adding devices
	protected static JSONObject deviceTemplate = new JSONObject();
	static {
		try {
			deviceTemplate.put("deviceID", "REPLACE");
			deviceTemplate.put("name", "REPLACE");
			deviceTemplate.put("compression", "metadata");
			deviceTemplate.put("introducer", false);
			deviceTemplate.put("skipIntroductionRemovals", false);
			deviceTemplate.put("introducedBy", "");
			deviceTemplate.put("addresses", new JSONArray());
			deviceTemplate.put("paused", false);
			deviceTemplate.put("autoAcceptFolders", false);
			deviceTemplate.put("maxSendKbps", 0);
			deviceTemplate.put("maxRecvKbps", 0);
			deviceTemplate.put("maxRequestKiB", 0);
		} catch (Exception e) {
			MCLogger.logError("Failed to create the device template", e);
		}
	};

	// Template for adding folders.
	protected static JSONObject folderTemplate = new JSONObject();
	static {
		try {
			folderTemplate.put("id", "REPLACE");
			folderTemplate.put("label", "REPLACE");
			folderTemplate.put("path", "REPLACE");
			folderTemplate.put("type", "sendreceive");
			// Can set to 0 to disable
			folderTemplate.put("rescanIntervalS", 3600);
			folderTemplate.put("fsWatcherEnabled", true);
			folderTemplate.put("fsWatcherDelayS", 10);
			folderTemplate.put("ignorePerms", false);
			folderTemplate.put("autoNormalize", true);
			folderTemplate.put("filesystemType", "basic");
			// Array of devices - one for each device sharing this folder.  Device is an object
			// with a "deviceID" and an optional "introducedBy" (which device introduced the folder).
			folderTemplate.put("devices", new JSONArray());
			JSONObject minDiskFree = new JSONObject();
			minDiskFree.put("unit", "%");
			minDiskFree.put("value", 1);
			folderTemplate.put("minDiskFree", minDiskFree);
			folderTemplate.put("versioning", new JSONObject());
			folderTemplate.put("copiers", 0);
			folderTemplate.put("pullerMaxPendingKiB", 0);
			folderTemplate.put("hashers", 0);
			folderTemplate.put("order", "random");
			folderTemplate.put("ignoreDelete", false);
			folderTemplate.put("scanProgressIntervalS", 0);
			folderTemplate.put("pullerPauseS", 0);
			folderTemplate.put("maxConflicts", 10);
			folderTemplate.put("disableSparseFiles", false);
			folderTemplate.put("disableTempIndexes", false);
			folderTemplate.put("paused", false);
			folderTemplate.put("weakHashThresholdPct", 25);
			folderTemplate.put("markerName", ".stfolder");
			folderTemplate.put("useLargeBlocks", false);
		} catch (Exception e) {
			MCLogger.logError("Failed to create the folder template", e);
		}
	};
	
	protected ConfigInfo configInfo;
	protected String host;
	protected int guiPort;
	protected int connectionPort;
	protected URI guiBaseUri;
	protected String connectionAddress;
	
	public AbstractDevice(ConfigInfo configInfo, String host, int guiPort, int connectionPort) throws IOException {
		this.configInfo = configInfo;
		this.host = host;
		this.guiPort = guiPort;
		this.connectionPort = connectionPort;
		this.connectionAddress = ADDR_PREFIX + host + ":" + connectionPort;
		
		try {
			this.guiBaseUri = new URI("http", null, getGUIHost(), guiPort, null, null, null);
		} catch (URISyntaxException e1) {
			throw new IOException("Failed to create the URI for the device: " + e1.getMessage());
		}
	}
	
	public AbstractDevice(String configContent, String host, int guiPort, int connectionPort) throws IOException, JSONException {
		this.host = host;
		this.guiPort = guiPort;
		this.connectionPort = connectionPort;
		this.connectionAddress = ADDR_PREFIX + host + ":" + connectionPort;
		
		URI baseUri = null;
		try {
			baseUri = new URI("http", null, getGUIHost(), guiPort, null, null, null);
			this.guiBaseUri = baseUri;
		} catch (URISyntaxException e1) {
			throw new IOException("Failed to create the URI for the device: " + e1.getMessage());
		}
		
		String apiKey = ConfigInfo.getAPIKey(configContent);
		this.configInfo = getConfigInfo(baseUri, apiKey);
		
		if (this.configInfo == null) {
			throw new IOException("Could not get the configuration info for this device");
		}
	}
	
	public String getGUIHost() {
		// In general, the gui host will be the local host.  This is on purpose so that
		// the gui cannot be accessed from elsewhere.  This can be overridden if necessary.
		return LOCALHOST;
	}

	// Test connection to the API, used for checking that Syncthing is up and running
	public boolean testConnection(int connectTimeout) {
		return APIUtils.ping(guiBaseUri, configInfo.apiKey, connectTimeout);
	}
	
	public String getConnectionAddress() {
		return connectionAddress;
	}
	
	public void stop() throws IOException {
		APIUtils.shutdown(guiBaseUri, configInfo.apiKey);
	}
	
	// Add an entry to this device's configuration for the given device
	public void addDeviceEntry(AbstractDevice device) throws IOException, JSONException {
		JSONObject config = APIUtils.getConfig(guiBaseUri, configInfo.apiKey);
		JSONObject jsonDevice = getDevice(config, device.getDeviceId());
		if (jsonDevice != null) {
			// If the connection address has changed then update it
			JSONArray addresses = jsonDevice.getJSONArray(DEVICE_ADDRESSES_KEY);
			JSONArray newAddresses = new JSONArray();
			boolean modified = false;
			for (int addrIndex = 0; addrIndex < addresses.length(); addrIndex++) {
				String addr = addresses.getString(addrIndex);
				if (addr.contains(device.host) && !addr.equals(device.connectionAddress)) {
					// Update
					newAddresses.put(device.connectionAddress);
					modified = true;
				} else {
					newAddresses.put(addr);
				}
			}
			if (modified) {
				jsonDevice.put(DEVICE_ADDRESSES_KEY, newAddresses);
				APIUtils.putConfig(guiBaseUri, configInfo.apiKey, config);
			}
			return;
		}
		JSONObject deviceEntry = createDeviceEntry(device);
		config.append(DEVICES_KEY, deviceEntry);
		APIUtils.putConfig(guiBaseUri, configInfo.apiKey, config);
	}
	
	// Create a device entry for the given device
	public JSONObject createDeviceEntry(AbstractDevice device) throws JSONException {
		JSONObject entry = new JSONObject(deviceTemplate.toString());
		entry.put(DEVICE_ID_KEY, device.configInfo.deviceId);
		entry.put(DEVICE_NAME_KEY, device.configInfo.deviceName);
		entry.append(DEVICE_ADDRESSES_KEY, device.getConnectionAddress());
		return entry;
	}
	
	// Start a folder scan
	public void scanFolder(String name) throws IOException {
		APIUtils.rescan(guiBaseUri, configInfo.apiKey, name);
	}
	
	// Remove the device from the folder.  If the folder is left with only its own host device
	// then remove the folder as well.  If the device is no longer used by any folders then
	// remove the device.
	public void removeFolder(String name, AbstractDevice device) throws IOException, JSONException {
		JSONObject config = APIUtils.getConfig(guiBaseUri, configInfo.apiKey);
		JSONArray folders = config.getJSONArray(FOLDERS_KEY);
		JSONArray newFolders = new JSONArray();
		boolean deviceIsUsed = false;
		for (int folderIndex = 0; folderIndex < folders.length(); folderIndex++) {
			JSONObject folder = folders.getJSONObject(folderIndex);
			if (name.equals(folder.getString(FOLDER_ID_KEY))) {
				JSONArray devices = folder.getJSONArray(DEVICES_KEY);
				JSONArray newDevices = new JSONArray();
				for (int devIndex = 0; devIndex < devices.length(); devIndex++) {
					JSONObject dev = devices.getJSONObject(devIndex);
					if (!device.getDeviceId().equals(dev.getString(DEVICE_ID_KEY))) {
						newDevices.put(dev);
					}
				}
				if (newDevices.length() > 0 && !(newDevices.length() == 1 && getDeviceId().equals(newDevices.getJSONObject(0).getString(DEVICE_ID_KEY)))) {
					// Only include the folder if there are still devices in the new list (other than this device)
					folder.put(DEVICES_KEY, newDevices);
					newFolders.put(folder);
				}
			} else {
				newFolders.put(folder);
				if (!deviceIsUsed) {
					JSONArray devices = folder.getJSONArray(DEVICES_KEY);
					for (int devIndex = 0; devIndex < devices.length(); devIndex++) {
						JSONObject dev = devices.getJSONObject(devIndex);
						if (device.getDeviceId().equals(dev.getString(DEVICE_ID_KEY))) {
							deviceIsUsed = true;
							break;
						}
					}
				}
			}
		}
		config.put(FOLDERS_KEY, newFolders);
		if (!deviceIsUsed) {
			removeDevice(config, device);
		}
		APIUtils.putConfig(guiBaseUri, configInfo.apiKey, config);
	}
	
	// Remove a device from the configuration
	public void removeDevice(AbstractDevice device) throws IOException, JSONException {
		JSONObject config = APIUtils.getConfig(guiBaseUri, configInfo.apiKey);
		removeDevice(config, device);
		APIUtils.putConfig(guiBaseUri, configInfo.apiKey, config);
	}
	
	private void removeDevice(JSONObject config, AbstractDevice device) throws JSONException {
		JSONArray devices = config.getJSONArray(DEVICES_KEY);
		JSONArray newDevices = new JSONArray();
		for (int devIndex = 0; devIndex < devices.length(); devIndex++) {
			JSONObject dev = devices.getJSONObject(devIndex);
			if (!device.getDeviceId().equals(dev.getString(DEVICE_ID_KEY))) {
				newDevices.put(dev);
			}
		}
		config.put(DEVICES_KEY, newDevices);
	}
	
	public String getDeviceId() {
		return configInfo.deviceId;
	}

	public void dispose() {
		// Override as needed
	}

	// Get the configuration info given the API connection information.  This method only works
	// if Syncthing is up and running.
	public static ConfigInfo getConfigInfo(URI baseUri, String apiKey) throws IOException, JSONException {
		String deviceId = APIUtils.getDeviceId(baseUri, apiKey);
		JSONObject config = APIUtils.getConfig(baseUri, apiKey);
		JSONObject device = getDevice(config, deviceId);
		if (device != null) {
			JSONArray addresses = device.getJSONArray(DEVICE_ADDRESSES_KEY);
			String address = addresses.getString(0);
			return new ConfigInfo(deviceId, device.getString(DEVICE_NAME_KEY), address, apiKey);
		}
		return null;
	}	

	private static JSONObject getDevice(JSONObject config, String deviceId) throws JSONException {
		JSONArray devices = config.getJSONArray(DEVICES_KEY);
		if (devices != null) {
			for (int i = 0; i < devices.length(); i++) {
				JSONObject device = devices.getJSONObject(i);
				if (deviceId.equals(device.getString(DEVICE_ID_KEY))) {
					return device;
				}
			}
		}
		return null;
	}
	
	// Get the configuration info given the config file.  This method can be used when Syncthing
	// is not running.
	public static ConfigInfo getConfigInfo(File configFile) throws IOException, FileNotFoundException {
		String content = getConfigContent(configFile);
		return new ConfigInfo(content);
	}
	
	public static String getConfigContent(File configFile) throws IOException {
		FileInputStream in = null;
		try {
			in = new FileInputStream(configFile);
			return MCUtil.readAllFromStream(in);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					MCLogger.logError("Could not close the config file: " + configFile);
				}
			}
		}
	}
	
	public static class ConfigInfo {
		private static final String API_KEY_START = "<apikey>";
		private static final String API_KEY_END = "</apikey>";
		private static final String DEVICE_KEY_START = "<device";
		private static final String ID_KEY = "id";
		private static final String NAME_KEY = "name";
		private static final String ADDRESS_START = "<address>";
		private static final String ADDRESS_END = "</address>";
		private static final String DEFAULT_FOLDER_START = "<defaultFolderPath>";
		private static final String DEFAULT_FOLDER_END = "</defaultFolderPath>";
		
		public final String deviceId;
		public final String deviceName;
		public final String deviceAddress;
		public final String apiKey;
		
		public ConfigInfo(String deviceId, String deviceName, String deviceAddress, String apiKey) {
			this.deviceId = deviceId;
			this.deviceName = deviceName;
			this.deviceAddress = deviceAddress;
			this.apiKey = apiKey;
		}

		// This is called when the config has been generated and syncthing is not started yet.
		// Allows the configuration to be set up with proper security before syncthing is started
		// by reading in the generated values and using them to construct a new configuration.
		public ConfigInfo(String configContent) throws IOException {
			// Get the device id
			int deviceIndex = configContent.indexOf(DEVICE_KEY_START);
			int idIndex = configContent.indexOf(ID_KEY, deviceIndex);
			int startIndex = configContent.indexOf('"', idIndex) + 1;
			int endIndex = configContent.indexOf('"', startIndex);
			deviceId = getSubstring(configContent, startIndex, endIndex, "device id");
			
			// Get the device name
			int nameIndex = configContent.indexOf(NAME_KEY, deviceIndex);
			startIndex = configContent.indexOf('"', nameIndex) + 1;
			endIndex = configContent.indexOf('"', startIndex);
			deviceName = getSubstring(configContent, startIndex, endIndex, "device name");

			// Get the device address
			startIndex = configContent.indexOf(ADDRESS_START, deviceIndex) + ADDRESS_START.length();
			endIndex = configContent.indexOf(ADDRESS_END, startIndex);
			deviceAddress = getSubstring(configContent, startIndex, endIndex, "device address");
			
			apiKey = getAPIKey(configContent);
		}
		
		public static String getAPIKey(String configContent) throws IOException {
			int startIndex = configContent.indexOf(API_KEY_START) + API_KEY_START.length();
			int endIndex = configContent.indexOf(API_KEY_END, startIndex);
			return getSubstring(configContent, startIndex, endIndex, "api key");
		}
		
		public static String getDefaultFolderPath(String configContent) throws IOException {
			int startIndex = configContent.indexOf(DEFAULT_FOLDER_START) + DEFAULT_FOLDER_START.length();
			int endIndex = configContent.indexOf(DEFAULT_FOLDER_END, startIndex);
			return getSubstring(configContent, startIndex, endIndex, "default folder");
		}
	}
	
	private static String getSubstring(String configContent, int startIndex, int endIndex, String item) throws IOException {
		if (startIndex < 0 || endIndex < 0 || endIndex >= configContent.length() || endIndex <= startIndex) {
			MCLogger.logError("Invalid generated host configuration file: could not extract the " + item);
			throw new IOException("Failed to get the " + item + " from the generated host configuration file");
		}
		return configContent.substring(startIndex, endIndex).trim();
	}
	
}
