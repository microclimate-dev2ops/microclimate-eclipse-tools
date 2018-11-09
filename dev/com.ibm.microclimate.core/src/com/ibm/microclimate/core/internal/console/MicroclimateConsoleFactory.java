/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core.internal.console;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.messages.Messages;

public class MicroclimateConsoleFactory {

	static final String MC_CONSOLE_TYPE = "microclimate-console"; //$NON-NLS-1$
	
	public static Set<IOConsole> createApplicationConsoles(MicroclimateApplication app) {

		Set<IOConsole> consoles = new HashSet<>();

		if (app.hasBuildLog()) {
			String buildLogName = NLS.bind(Messages.BuildConsoleName, app.name);
			IOConsole buildConsole = new BuildLogConsole(buildLogName, app);
			consoles.add(buildConsole);
			onNewConsole(buildConsole);
		}
		else {
			MCLogger.logError("No buildLogPath is set for app " + app.name); 		// $NON-NLS-1$
		}

		String appLogName = NLS.bind(Messages.AppConsoleName, app.name);
		IOConsole appConsole = new SocketConsole(appLogName, app);
		consoles.add(appConsole);
		onNewConsole(appConsole);
		ConsolePlugin.getDefault().getConsoleManager().showConsoleView(appConsole);

		return consoles;
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