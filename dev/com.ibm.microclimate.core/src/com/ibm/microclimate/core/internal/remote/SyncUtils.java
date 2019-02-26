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
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;

import com.ibm.microclimate.core.MicroclimateCorePlugin;
import com.ibm.microclimate.core.internal.FileUtil;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.PlatformUtil;
import com.ibm.microclimate.core.internal.PlatformUtil.OperatingSystem;

public class SyncUtils {
	
private static final Map<OperatingSystem, String> syncthingMap = new HashMap<OperatingSystem, String>();

	private static final String SYNCTHING_DIR = "syncthing";
	private static final String HOST_CONFIG_BASE_NAME = "HostBaseConfig.xml";
	private static final String HOST_CONFIG_BASE_PATH = "resources/HostBaseConfig.xml";
	
	static {
		syncthingMap.put(OperatingSystem.LINUX, "resources/linux/syncthing");
		syncthingMap.put(OperatingSystem.MAC, "resources/osx/syncthing");
		syncthingMap.put(OperatingSystem.WINDOWS, "resources/windows/syncthing.exe");
	}
	
	public static String getSyncthingExecutable() throws IOException {
		// Get the current platform and choose the correct executable path
		OperatingSystem os = PlatformUtil.getOS(System.getProperty("os.name"));
		String relPath = syncthingMap.get(os);
		if (relPath == null) {
			String msg = "Failed to get the relative path for the syncthing executable";
			MCLogger.logError(msg);
			throw new IOException(msg);
		}
		
		// Make the directory for storing the executable
		String syncthingDir = getSyncthingDir();
		if (!FileUtil.makeDir(syncthingDir)) {
			String msg = "Failed to make the directory for storing the syncthing executable";
			MCLogger.logError(msg);
			throw new IOException(msg);
		}
		
		// Get the executable name
		String execName = relPath.substring(relPath.lastIndexOf('/') + 1);
		
		// Copy the executable over
		InputStream stream = null;
		String execPath = syncthingDir + File.separator + execName;
		try {
			stream = FileLocator.openStream(MicroclimateCorePlugin.getInstance().getBundle(), new Path(relPath), false);
			FileUtil.copyFile(stream, execPath);
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					// Ignore
				}
			}
		}
		
		return execPath;
	}

	public static File getHostConfigBaseFile() throws IOException {
		String syncthingDir = getSyncthingDir();
		String targetPath = syncthingDir + File.separator + HOST_CONFIG_BASE_NAME;
		InputStream stream = null;
		try {
			stream = FileLocator.openStream(MicroclimateCorePlugin.getInstance().getBundle(), new Path(HOST_CONFIG_BASE_PATH), false);
			FileUtil.copyFile(stream, targetPath);
			return new File(targetPath);
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					// Ignore
				}
			}
		}
	}
	
	public static String getSyncthingDir() {
		String stateLoc = MicroclimateCorePlugin.getInstance().getStateLocationString();
		String syncthingDir = stateLoc + File.separator + SYNCTHING_DIR;
		return syncthingDir;
	}

}
