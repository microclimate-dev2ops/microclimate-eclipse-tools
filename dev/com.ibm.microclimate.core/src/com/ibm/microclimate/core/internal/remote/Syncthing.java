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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.json.JSONException;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.remote.AbstractDevice.ConfigInfo;
import com.ibm.microclimate.core.internal.remote.ProcessHelper.ProcessResult;

public class Syncthing {
	
	private static final String CONFIG_DIR = "config";
	private static final String CONFIG_FILE = "config.xml";
	private static final String FOLDER_DIR = "folders";
	private static final String TMP_DIR = "tmp";
	private static final int START_TIMEOUT = 30000;
	
	// TODO: use these if they are free, if not, get a free port and modify the config.xml with the new port
	// Would also need to modify any connection addresses in ICP devices
	private static final int GUI_PORT = 8384;
	private static final int CONNECTION_PORT = 22000;
	
	private final String host;
	private final File hostConfigBase;
	private final String execPath;
	private final String syncthingDir;
	private final String configDir;
	private final String folderDir;
	private HostDevice hostDevice = null;
	private Set<AbstractDevice> devices = Collections.synchronizedSet(new HashSet<AbstractDevice>());
	private SyncthingEventMonitor eventMonitor;
	
	public Syncthing(String host, String execPath, String syncthingDir, File hostConfigBase) throws Exception {
		this.host = host;
		this.hostConfigBase = hostConfigBase;
		this.execPath = execPath;
		this.syncthingDir = syncthingDir;
		this.configDir = syncthingDir + File.separator + CONFIG_DIR;
		this.folderDir = syncthingDir + File.separator + FOLDER_DIR;
	}
	
	// If the configuration is not set up then create the configuration using the template
	private String setupHostConfig() throws IOException, TimeoutException {
		String configContent;
		File configFile = new File(configDir + File.separator + CONFIG_FILE);
		if (configFile.exists()) {
			configContent = AbstractDevice.getConfigContent(configFile);
			String defaultFolder = ConfigInfo.getDefaultFolderPath(configContent);
			if (folderDir.equals(defaultFolder)) {
				// The config file has already been set up
				return configContent;
			}
		}
		// The config file is not set up so generate the default configuration
		generateConfig();
		// Replace the default configuration with the template (filled in with the device id, name, etc. from
		// the default configuration).
		configContent = HostDevice.setupHostDevice(configFile, folderDir, hostConfigBase);
		
		// Make sure the folders directory is created
		File file = new File(folderDir);
		if (!file.mkdirs()) {
			throw new IOException("Could not create the folder directory: " + folderDir);
		}
		
		return configContent;
	}
	
	// Generate the default configuration.  This will not actually start Syncthing.
	private void generateConfig() throws IOException, TimeoutException {
		String[] command = {execPath, "-generate=" + configDir};
		ProcessBuilder builder = new ProcessBuilder(command);
		ProcessResult result = ProcessHelper.waitForProcess(builder.start(), 500, 5);
		if (result.getExitValue() != 0) {
			MCLogger.logError("Generating the initial syncthing configuration failed with a " + result.getExitValue() + " return code and error: " + result.getError());
			throw new RuntimeException(result.getError());
		}
	}
	
	// Set up the host device and start Syncthing and the event monitor
	public void start() throws IOException, TimeoutException, URISyntaxException, JSONException {
		String apiKey;
		URI baseUri;
		String configContent = null;
		if (hostDevice == null) {
			configContent = setupHostConfig();
			apiKey = ConfigInfo.getAPIKey(configContent);
			baseUri = new URI("http", null, AbstractDevice.LOCALHOST, GUI_PORT, null, null, null);
		} else {
			apiKey = hostDevice.configInfo.apiKey;
			baseUri = hostDevice.guiBaseUri;
		}
		String[] command = {execPath, "-home=" + configDir};
		ProcessBuilder builder = new ProcessBuilder(command);
		Process process = builder.start();
		
		boolean started = false;
		for (int i = 0; i < START_TIMEOUT/5000; i++) {
			if (APIUtils.ping(baseUri, apiKey, 5000)) {
				started = true;
				break;
			}
		}
		
		if (!started) {
			process.destroy();
			throw new IOException("Syncthing failed to start.");
		}
		
		if (hostDevice == null) {
			hostDevice = new HostDevice(configContent, host, GUI_PORT, CONNECTION_PORT, folderDir);
		}
		
		eventMonitor = new SyncthingEventMonitor(hostDevice.configInfo, hostDevice.guiBaseUri);
		Thread thread = new Thread(eventMonitor);
		thread.setDaemon(true);
		thread.start();
	}
	
	// Stop Syncthing and the event monitor
	public void stop() throws IOException {
		if (eventMonitor != null) {
			eventMonitor.stopMonitor();
		}
		
		if (hostDevice == null) {
			MCLogger.logError("Trying to stop syncthing when host device is not set up");
			return;
		}
		hostDevice.stop();
		for (AbstractDevice device : devices) {
			device.dispose();
		}
	}
	
	// Check if Syncthing is running
	public boolean isRunning() {
		if (hostDevice == null) {
			return false;
		}
		return hostDevice.testConnection(5000);
	}
	
	public void addEventListener(SyncthingEventListener listener, String... eventTypes) {
		eventMonitor.addListener(listener, eventTypes);
	}
	
	public void removeEventListener(SyncthingEventListener listener, String... eventTypes) {
		eventMonitor.removeListener(listener, eventTypes);
	}

	// Shares an ICP folder with the host.
	public String shareICPFolder(String host, String namespace, String projectName) throws IOException, JSONException {
		final ICPDevice icpDevice = addICPDevice(host, namespace);
		
		// Add the folder to the ICP device
		icpDevice.addFolderEntry(projectName, hostDevice);
		
		// Keep track of the folder in the host device (state, errors);
		hostDevice.addFolder(projectName, icpDevice);
		
		// Return the local path for the folder
		return hostDevice.getLocalFolder(projectName);
	}

	// Start folder scan
	public void scanFolder(String projectName) throws IOException {
		hostDevice.scanFolder(projectName);
	}
	
	// Stop sharing the given folder
	public String stopSharingFolder(String projectName) throws IOException, JSONException {
		SyncthingFolder folder = hostDevice.getFolder(projectName);
		if (folder != null) {
			folder.getShareDevice().removeFolder(projectName, hostDevice);
			hostDevice.removeFolder(projectName);
			return hostDevice.getLocalFolder(projectName);
		}
		return null;
	}
	
	// Checks if there are currently any shared folders
	public boolean hasSharedFolders() {
		return hostDevice.getSharedFolderCount() > 0;
	}
	
	// Remove an ICP device
	public void removeICPDevice(String host, String namespace) throws IOException, JSONException {
		ICPDevice device = getICPDevice(host, namespace);
		if (device != null) {
			device.removeDevice(hostDevice);
			hostDevice.removeDevice(device);
			device.dispose();
		}
	}
	
	// Add an ICP device
	private ICPDevice addICPDevice(String host, String namespace) throws IOException, JSONException {
		ICPDevice icpDevice = getICPDevice(host, namespace);
		if (icpDevice == null) {
			icpDevice = ICPDevice.createICPDevice(host, namespace, syncthingDir + File.separator + TMP_DIR);
			hostDevice.addDeviceEntry(icpDevice);
			icpDevice.addDeviceEntry(hostDevice);
			devices.add(icpDevice);
		}
		return icpDevice;
	}
	
	private ICPDevice getICPDevice(String host, String namespace) {
		for (AbstractDevice device : devices) {
			if (device instanceof ICPDevice && ((ICPDevice)device).isThisDevice(host, namespace)) {
				return (ICPDevice)device;
			}
		}
		return null;
	}

}
