/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core.internal.server.console;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;

import com.ibm.microclimate.core.MicroclimateCorePlugin;
import com.ibm.microclimate.core.internal.MCConstants;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;

public class MicroclimateServerConsole extends IOConsole {

	private static final String MC_CONSOLE_TYPE = "microclimate-console";

	public static Set<MicroclimateServerConsole> createApplicationConsoles(MicroclimateApplication app) {

		Set<MicroclimateServerConsole> consoles = new HashSet<>();

		for (IPath logPath : app.getLogFilePaths()) {
			File logFile = new File(logPath.toOSString());

			if (!logFile.exists()) {
				MCLogger.logError("Log file not found at: " + logPath.toOSString());
				continue;
			}

			// We want the console name to be "myApp build.log" or "myApp messages.log".
			// the build.log has a different structure - it's mc-$projectId-$uuid.build.log, but we just want build.log

			String fileName = logFile.getName();

			if (fileName.endsWith(MCConstants.BUILD_LOG_SHORTNAME)) {
				fileName = MCConstants.BUILD_LOG_SHORTNAME;
			}

			String consoleName = app.name + " - " + fileName;
			consoles.add(new MicroclimateServerConsole(consoleName, logFile));
		}

		return consoles;
	}

	private final MicroclimateServerLogMonitorThread logMonitorThread;
	private final IOConsoleOutputStream outputStream;

	public MicroclimateServerConsole(String consoleName, File logFile) {
		super(consoleName, MC_CONSOLE_TYPE,
				MicroclimateCorePlugin.getIcon(MicroclimateCorePlugin.DEFAULT_ICON_PATH),
				true);

		outputStream = newOutputStream();
		logMonitorThread = new MicroclimateServerLogMonitorThread(consoleName, logFile, outputStream);
		logMonitorThread.start();

		IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();

		// See if a console exists matching this one and remove it if it does,
		// so that we don't have multiple of the same console (they would be identical anyway)
		IConsole[] existingMCConsoles = consoleManager.getConsoles();
		for (IConsole console : existingMCConsoles) {
			if (console instanceof MicroclimateServerConsole && console.getName().equals(consoleName)) {
				consoleManager.removeConsoles(new IConsole[] { console } );
				break;
			}
		}

		MCLogger.log("Creating new server console: " + getName());
		consoleManager.addConsoles(new IConsole[] { this });
	}

	@Override
	protected void dispose() {
		MCLogger.log("Dispose console " + getName());

		logMonitorThread.disable();
		logMonitorThread.interrupt();
		try {
			outputStream.close();
		} catch (IOException e) {
			MCLogger.logError("Error closing console output stream", e);
		}

		super.dispose();
	}
}