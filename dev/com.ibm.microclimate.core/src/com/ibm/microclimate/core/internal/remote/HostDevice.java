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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;

public class HostDevice extends AbstractDevice {
	
	private static final String DEVICE_ID_REPLACE = "MC_REPLACE_DEVICE_ID";
	private static final String DEVICE_NAME_REPLACE = "MC_REPLACE_DEVICE_NAME";
	private static final String DEVICE_ADDRESS_REPLACE = "MC_REPLACE_DEVICE_ADDRESS";
	private static final String API_KEY_REPLACE = "MC_REPLACE_API_KEY";
	private static final String DEFAULT_FOLDER_REPLACE = "MC_REPLACE_DEFAULT_FOLDER_PATH";
	
	private final String folderDir;
	
	// Keep track of the active folders.  Access to this object should be synchronized.
	private final Map<String, SyncthingFolder> folders = new HashMap<String, SyncthingFolder>();
	
	public HostDevice(ConfigInfo configInfo, String host, int guiPort, int connectionPort, String folderDir) throws IOException {
		super(configInfo, host, guiPort, connectionPort);
		this.folderDir = folderDir;
	}
	
	public HostDevice(String configContent, String host, int guiPort, int connectionPort, String folderDir) throws IOException, JSONException {
		super(configContent, host, guiPort, connectionPort);
		this.folderDir = folderDir;
	}
	
	// Read in the information from the generated default config file and then use it to construct
	// a configuration from the template.
	public static String setupHostDevice(File configFile, String folderDir, File configBaseFile) throws IOException {
		ConfigInfo info = getConfigInfo(configFile);
		InputStream in = null;
		FileWriter out = null;
		try {
			in = new FileInputStream(configBaseFile);
			String content = MCUtil.readAllFromStream(in);
			content = content.replace(DEVICE_ID_REPLACE, info.deviceId);
			content = content.replace(DEVICE_NAME_REPLACE, info.deviceName);
			content = content.replace(DEVICE_ADDRESS_REPLACE, info.deviceAddress);
			content = content.replace(API_KEY_REPLACE, info.apiKey);
			content = content.replace(DEFAULT_FOLDER_REPLACE, folderDir);
			out = new FileWriter(configFile);
			out.write(content);
			return content;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					MCLogger.logError("Could not close the config base file: " + configBaseFile);
				}
			}
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					MCLogger.logError("Could not close the config file: " + configFile);
				}
			}
		}
	}
	
	@Override
	public JSONObject createDeviceEntry(AbstractDevice device) throws JSONException {
		JSONObject entry = super.createDeviceEntry(device);
		entry.put("autoAcceptFolders", true);
		return entry;
	}
	
	// Get the path for the local copy of the project
	public String getLocalFolder(String projectName) {
		return folderDir + File.separator + projectName;
	}
	
	public synchronized void addFolder(String projectName, AbstractDevice device) {
		SyncthingFolder folder = new SyncthingFolder(projectName, device);
		folders.put(projectName, folder);
	}
	
	public synchronized SyncthingFolder getFolder(String projectName) {
		return folders.get(projectName);
	}
	
	public synchronized void removeFolder(String projectName) throws IOException, JSONException {
		SyncthingFolder folder = folders.remove(projectName);
		if (folder != null) {
			removeFolder(projectName, folder.getShareDevice());
		}
	}
	
	public synchronized int getSharedFolderCount() {
		return folders.size();
	}
}
