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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.remote.ProcessHelper.ProcessResult;

public class Syncthing {
	
	private static final String CONFIG_DIR = "config";
	private static final String CONFIG_FILE = "config.xml";
	private static final String FOLDER_DIR = "folders";
	private static final String TMP_DIR = "tmp";
	private static final int START_TIMEOUT = 10000;
	
	private final String execPath;
	private final String syncthingDir;
	private final String configDir;
	private final String folderDir;
	private HostDevice hostDevice = null;
	private Set<AbstractDevice> devices = Collections.synchronizedSet(new HashSet<AbstractDevice>());
	private SyncthingEventMonitor eventListener;
	
	public Syncthing(String host, String execPath, String syncthingDir, File hostConfigBase) throws Exception {
		this.execPath = execPath;
		this.syncthingDir = syncthingDir;
		this.configDir = syncthingDir + File.separator + CONFIG_DIR;
		this.folderDir = syncthingDir + File.separator + FOLDER_DIR;
		initialize(host, hostConfigBase);
	}
	
	private void initialize(String host, File hostConfigBase) throws Exception {
		File configFile = new File(configDir + File.separator + CONFIG_FILE);
		if (!configFile.exists()) {
			generateConfig();
			hostDevice = HostDevice.createHostDevice(host, new File(configDir + File.separator + CONFIG_FILE), folderDir, hostConfigBase);
		} else {
			hostDevice = HostDevice.createHostDevice(host, new File(configDir + File.separator + CONFIG_FILE), folderDir);
		}
	}
	
	private void generateConfig() throws Exception {
		String[] command = {execPath, "-generate=" + configDir};
		ProcessBuilder builder = new ProcessBuilder(command);
		ProcessResult result = ProcessHelper.waitForProcess(builder.start(), 500, 5);
		if (result.getExitValue() != 0) {
			MCLogger.logError("Generating the initial syncthing configuration failed with a " + result.getExitValue() + " return code and error: " + result.getError());
			throw new RuntimeException(result.getError());
		}
	}
	
	public void start() throws IOException {
		String[] command = {execPath, "-home=" + configDir};
		ProcessBuilder builder = new ProcessBuilder(command);
		Process process = builder.start();
		
		boolean started = false;
		for (int i = 0; i < START_TIMEOUT/1000; i++) {
			if (hostDevice.testConnection(1000)) {
				started = true;
				break;
			}
		}
		
		if (!started) {
			process.destroy();
			throw new IOException("Syncthing failed to start.");
		}
		
		eventListener = new SyncthingEventMonitor(hostDevice.configInfo, hostDevice.guiBaseUri);
		Thread thread = new Thread(eventListener);
		thread.setDaemon(true);
		thread.start();
		
//		try {
//			shareICPFolder("xxx.xxx.xxx.xxx", "mcc", "node14a");
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}
	
	public void stop() throws IOException {
		if (eventListener != null) {
			eventListener.stopListener();
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
	
	public boolean isRunning() {
		if (hostDevice == null) {
			return false;
		}
		return hostDevice.testConnection(5000);
	}
	
	public String shareICPFolder(String host, String namespace, String projectName) throws Exception {
		final ICPDevice icpDevice = addICPDevice(host, namespace);
		final boolean[] upToDate = new boolean[] {false};
		SyncthingEventListener folderCompletionListener = new SyncthingEventListener() {
			@Override
			public void eventNotify(SyncthingEvent event) {
				if (icpDevice.getDeviceId().equals(event.data.get(SyncthingEventMonitor.DEVICE_KEY))
						&& projectName.equals(event.data.get(SyncthingEventMonitor.FOLDER_KEY))) {
					Object completion = event.data.get(SyncthingEventMonitor.COMPLETION_KEY);
					if (completion != null && completion instanceof Integer && ((Integer)completion).intValue() == 100) {
						upToDate[0] = true;
					}
				}
			}
		};
		eventListener.addListener(folderCompletionListener, SyncthingEventMonitor.FOLDER_COMPLETION_TYPE);
		
		try {
			icpDevice.addFolderEntry(projectName, hostDevice);
			
			// Wait for the folder to be updated before returning
			for (int i = 0; i < 60 && !upToDate[0]; i++) {
				try {
					Thread.sleep(500);
				} catch (Exception e) {
					// Ignore
				}
			}
		} finally {
			eventListener.removeListener(folderCompletionListener, SyncthingEventMonitor.FOLDER_COMPLETION_TYPE);
		}
		
		if (!upToDate[0]) {
			String msg = "The synchronization did not complete in the expected time for folder: " + projectName;
			MCLogger.logError(msg);
			throw new IOException(msg);
		}
		
		// Keep track of the folder in the host device (state, errors);
		hostDevice.addFolder(projectName);
		
		// Return the local path for the folder
		return hostDevice.getLocalFolder(projectName);
	}
	
	public boolean hasSharedFolders() {
		return hostDevice.getSharedFolderCount() > 0;
	}
	
	public void restore() throws Exception {
		
	}
	
	private ICPDevice addICPDevice(String host, String namespace) throws Exception {
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
