/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.core.internal.console;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;

import com.ibm.microclimate.core.MicroclimateCorePlugin;
import com.ibm.microclimate.core.internal.MCLogger;

public class FileConsole extends IOConsole {

	private final FileConsoleMonitorThread logMonitorThread;
	private final IOConsoleOutputStream outputStream;

	public FileConsole(String consoleName, IPath logFilePath) throws FileNotFoundException {
		super(consoleName, MicroclimateConsoleFactory.MC_CONSOLE_TYPE,
				MicroclimateCorePlugin.getIcon(MicroclimateCorePlugin.DEFAULT_ICON_PATH),
				true);

		File logFile = new File(logFilePath.toOSString());
		if (!logFile.exists()) {
			throw new FileNotFoundException(logFile.getAbsolutePath() + " does not exist");
		}

		outputStream = newOutputStream();
		logMonitorThread = new FileConsoleMonitorThread(consoleName, logFile, outputStream);
		logMonitorThread.start();
	}

	@Override
	protected void dispose() {
		MCLogger.log("Dispose console " + getName()); //$NON-NLS-1$

		logMonitorThread.disable();
		logMonitorThread.interrupt();
		try {
			outputStream.close();
		} catch (IOException e) {
			MCLogger.logError("Error closing console output stream", e); //$NON-NLS-1$
		}

		super.dispose();
	}
}
