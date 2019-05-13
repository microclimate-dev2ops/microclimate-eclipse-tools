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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;

import com.ibm.microclimate.core.MicroclimateCorePlugin;
import com.ibm.microclimate.core.internal.PlatformUtil.OperatingSystem;

public class InstallUtil {
	
	private static final Map<OperatingSystem, String> installMap = new HashMap<OperatingSystem, String>();

	static {
		installMap.put(OperatingSystem.LINUX, "resources/linux/install");
		installMap.put(OperatingSystem.MAC, "resources/osx/install");
		installMap.put(OperatingSystem.WINDOWS, "resources/windows/install.exe");
	}
	
	public static String getInstallExecutable() throws IOException {
		// Get the current platform and choose the correct executable path
		OperatingSystem os = PlatformUtil.getOS(System.getProperty("os.name"));
		String relPath = installMap.get(os);
		if (relPath == null) {
			String msg = "Failed to get the relative path for the install executable";
			MCLogger.logError(msg);
			throw new IOException(msg);
		}
		
		try {
			URL url = FileLocator.find(MicroclimateCorePlugin.getDefault().getBundle(), new Path(relPath));
			if (url != null) {
				url = FileLocator.resolve(url);
				File file = Paths.get(url.toURI()).toFile();
				return file.getCanonicalPath();
			}
		} catch (URISyntaxException e) {
			String msg = "An error occurred while trying to get the full path for the install executable:" + e.getMessage();
			MCLogger.logError(msg);
			throw new IOException(msg);
		}
		return null;
	}

}
