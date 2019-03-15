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
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ibm.microclimate.core.internal.HttpUtil;
import com.ibm.microclimate.core.internal.HttpUtil.HttpResult;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;

public abstract class AbstractDevice {
	
	protected static final int DEFAULT_TIMEOUT = 5000;
	protected static final String LOCALHOST = "127.0.0.1";
	protected static final String API_KEY_PROP = "X-API-Key";
	protected static final String GET_PING_REST_PATH = "/rest/system/ping";
	protected static final String POST_SHUTDOWN_REST_PATH = "/rest/system/shutdown";
	protected static final String CONFIG_REST_PATH = "/rest/system/config";

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
		this.connectionAddress = "tcp://" + host + ":" + connectionPort;
		
		try {
			this.guiBaseUri = new URI("http", null, getGUIHost(), guiPort, null, null, null);
		} catch (URISyntaxException e1) {
			throw new IOException("Failed to create the URI for the device: " + e1.getMessage());
		}
	}
	
	public String getGUIHost() {
		// In general, the gui host will be the local host.  This is on purpose so that
		// the gui cannot be accessed from elsewhere.  This can be overridden if necessary.
		return LOCALHOST;
	}

	public boolean testConnection(int connectTimeout) {
		// Try to connect
		final URI uri = guiBaseUri.resolve(GET_PING_REST_PATH);
		try {
			HttpResult result = HttpUtil.get(uri, getRequestProperties(configInfo), connectTimeout);
			checkResult(uri, result);
		} catch (IOException e) {
			MCLogger.logError("Exception while testing the connection for " + configInfo.deviceName, e);
			return false;
		}

		return true;
	}
	
	public String getConnectionAddress() {
		return connectionAddress;
	}
	
	public void stop() throws IOException {
		final URI uri = guiBaseUri.resolve(POST_SHUTDOWN_REST_PATH);
		HttpResult result = HttpUtil.post(uri, getRequestProperties(configInfo), new JSONObject());
		
		if (!result.isGoodResponse) {
			MCLogger.logError(String.format("Received bad response from server %d with error message %s", //$NON-NLS-1$
					result.responseCode, result.error));
			throw new IOException("Failed to stop the syncthing host device.");
		}
	}
	
	public void addDeviceEntry(AbstractDevice device) throws IOException, JSONException {
		JSONObject config = getConfig();
		JSONObject jsonDevice = getDevice(config, device.configInfo.deviceId);
		if (jsonDevice != null) {
			return;
		}
		JSONObject deviceEntry = createDeviceEntry(device);
		config.append("devices", deviceEntry);
		putConfig(config);
	}
	
	public JSONObject createDeviceEntry(AbstractDevice device) throws JSONException {
		JSONObject entry = new JSONObject(deviceTemplate.toString());
		entry.put("deviceID", device.configInfo.deviceId);
		entry.put("name", device.configInfo.deviceName);
		entry.append("addresses", device.getConnectionAddress());
		return entry;
	}
	
	public String getDeviceId() {
		return configInfo.deviceId;
	}
	
	protected JSONObject getDevice(JSONObject config, String deviceId) throws JSONException {
		JSONArray devices = config.getJSONArray("devices");
		if (devices != null) {
			for (int i = 0; i < devices.length(); i++) {
				JSONObject device = devices.getJSONObject(i);
				if (deviceId.equals(device.getString("deviceID"))) {
					return device;
				}
			}
		}
		return null;
	}
	
	public void dispose() {
		// Override as needed
	}

	protected JSONObject getConfig() throws IOException, JSONException {
		final URI uri = guiBaseUri.resolve(CONFIG_REST_PATH);
		HttpResult result = HttpUtil.get(uri, getRequestProperties(configInfo), DEFAULT_TIMEOUT);
		checkResult(uri, result);
		JSONObject config = new JSONObject(result.response);
		return config;
	}
	
	protected void putConfig(JSONObject config) throws IOException {
		final URI uri = guiBaseUri.resolve(CONFIG_REST_PATH);
		HttpResult result = HttpUtil.post(uri, getRequestProperties(configInfo), config);
		checkResult(uri, result);
	}
	
	protected void checkResult(URI uri, HttpResult result) throws IOException {
		if (!result.isGoodResponse) {
			String msg = String.format("Received bad response %d with error message %s for request: %s", //$NON-NLS-1$
					result.responseCode, result.error, uri);
			MCLogger.logError(msg);
			throw new IOException(msg);
		}
	}
	
	public static Map<String, String> getRequestProperties(ConfigInfo configInfo) {
		Map<String, String> requestProperties = new HashMap<String, String>();
		requestProperties.put(API_KEY_PROP, configInfo.apiKey);
		return requestProperties;
	}
	
	public static ConfigInfo getConfigInfo(File configFile) throws IOException, FileNotFoundException {
		FileInputStream in = null;
		try {
			in = new FileInputStream(configFile);
			String content = MCUtil.readAllFromStream(in);
			return new ConfigInfo(content);
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
		
		// Should really use XPath here
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
			
			// Get the API key
			startIndex = configContent.indexOf(API_KEY_START) + API_KEY_START.length();
			endIndex = configContent.indexOf(API_KEY_END, startIndex);
			apiKey = getSubstring(configContent, startIndex, endIndex, "api key");
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
