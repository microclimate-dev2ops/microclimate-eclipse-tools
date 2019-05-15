/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *	 IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.core.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.ibm.microclimate.core.MicroclimateCorePlugin;
import com.ibm.microclimate.core.internal.PlatformUtil.OperatingSystem;

public class InstallUtil {
	
	private static final Map<OperatingSystem, String> installMap = new HashMap<OperatingSystem, String>();

	static {
		installMap.put(OperatingSystem.LINUX, "resources/linux/installer");
		installMap.put(OperatingSystem.MAC, "resources/osx/installer");
		installMap.put(OperatingSystem.WINDOWS, "resources/windows/installer.exe");
	}
	
	private static final String INSTALLER_DIR = "installerWorkDir";
	private static final String START_CMD = "start";
	private static final String STOP_ALL_CMD = "stop-all";
	
	public static Process startCodewind() throws IOException {
		return runInstaller(START_CMD);
	}
	
	public static Process stopCodewind() throws IOException {
		return runInstaller(STOP_ALL_CMD);
	}
	
	public static Process runInstaller(String cmd) throws IOException {
		String installerPath = getInstallerExecutable();
		String[] command = {installerPath, cmd};
		ProcessBuilder builder = new ProcessBuilder(command);
		if (PlatformUtil.getOS() == PlatformUtil.OperatingSystem.MAC) {
			String pathVar = System.getenv("PATH");
			pathVar = "/usr/local/bin:" + pathVar;
			Map<String, String> env = builder.environment();
			env.put("PATH", pathVar);
		}
		return builder.start();
	}
	
	public static String getInstallerExecutable() throws IOException {
		// Get the current platform and choose the correct executable path
		OperatingSystem os = PlatformUtil.getOS(System.getProperty("os.name"));
		String relPath = installMap.get(os);
		if (relPath == null) {
			String msg = "Failed to get the relative path for the install executable";
			MCLogger.logError(msg);
			throw new IOException(msg);
		}
		
		// Make the installer directory
		String installerDir = getInstallerDir();
		if (!FileUtil.makeDir(installerDir)) {
			String msg = "Failed to make the directory for the installer utility: " + installerDir;
			MCLogger.logError(msg);
			throw new IOException(msg);
		}
		
		// Get the executable name
		String execName = relPath.substring(relPath.lastIndexOf('/') + 1);
		
		// Copy the executable over
		InputStream stream = null;
		String execPath = installerDir + File.separator + execName;
		try {
			stream = FileLocator.openStream(MicroclimateCorePlugin.getDefault().getBundle(), new Path(relPath), false);
			FileUtil.copyFile(stream, execPath);
			if (PlatformUtil.getOS() != PlatformUtil.OperatingSystem.WINDOWS) {
				Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwxr-xr-x");
				File file = new File(execPath);
				Files.setPosixFilePermissions(file.toPath(), permissions);
			}
			return execPath;
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
	
	private static String getInstallerDir() {
		IPath stateLoc = MicroclimateCorePlugin.getDefault().getStateLocation();
		return stateLoc.append(INSTALLER_DIR).toOSString();
	}

}
