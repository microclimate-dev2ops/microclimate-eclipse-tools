/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.core.internal.console;

import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.messages.Messages;

public class MicroclimateConsoleFactory {

	static final String MC_CONSOLE_TYPE = "microclimate-console"; //$NON-NLS-1$
	
	public static IOConsole createApplicationConsole(MicroclimateApplication app) {
		String appLogName = NLS.bind(Messages.AppConsoleName, app.name);
		IOConsole appConsole = new OldSocketConsole(appLogName, app);
		onNewConsole(appConsole);
		return appConsole;
	}
	
	public static IOConsole createBuildConsole(MicroclimateApplication app) {
		if (app.hasBuildLog()) {
			String buildLogName = NLS.bind(Messages.BuildConsoleName, app.name);
			IOConsole buildConsole = new BuildLogConsole(buildLogName, app);
			onNewConsole(buildConsole);
			return buildConsole;
		}
		else {
			MCLogger.logError("No buildLogPath is set for app " + app.name); 		// $NON-NLS-1$
		}
		return null;
	}
	
	public static SocketConsole createLogFileConsole(MicroclimateApplication app, ProjectLogInfo logInfo) {
		String consoleName;
		consoleName = NLS.bind(Messages.LogFileConsoleName, app.name, logInfo.logName);
		SocketConsole console = new SocketConsole(consoleName, logInfo, app);
		onNewConsole(console);
		return console;
	}

	private static void onNewConsole(IOConsole console) {
		IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();

		// See if a console exists matching this one and remove it if it does,
		// so that we don't have multiple of the same console (they would be identical anyway)
		IConsole[] existingMCConsoles = consoleManager.getConsoles();
		for (IConsole existingConsole : existingMCConsoles) {
			if (existingConsole.getName().equals(console.getName())) {
				consoleManager.removeConsoles(new IConsole[] { existingConsole } );
				break;
			}
		}
			
		MCLogger.log(String.format("Creating new application console: %s of type %s", 				//$NON-NLS-1$
				console.getName(), console.getClass().getSimpleName()));

		consoleManager.addConsoles(new IConsole[] { console });
	}
}
