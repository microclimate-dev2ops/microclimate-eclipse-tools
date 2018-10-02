package com.ibm.microclimate.core.internal.server.console;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.Messages;
import com.ibm.microclimate.core.internal.MicroclimateApplication;

public class MicroclimateConsoleFactory {

	static final String MC_CONSOLE_TYPE = "microclimate-console"; //$NON-NLS-1$

	public static Set<IOConsole> createApplicationConsoles(MicroclimateApplication app) {

		Set<IOConsole> consoles = new HashSet<>();

		if (app.buildLogPath != null) {
			try {
				String buildLogName = NLS.bind(Messages.BuildConsoleName, app.name);
				IOConsole buildConsole = new FileConsole(buildLogName, app.buildLogPath);
				consoles.add(buildConsole);
				onNewConsole(buildConsole);
			} catch (FileNotFoundException e) {
				MCUtil.openDialog(true, Messages.FileNotFoundTitle,
						NLS.bind(Messages.FileNotFoundMsg, app.buildLogPath));
			}
		}
		else {
			MCLogger.logError("No buildLogPath is set for app " + app.name); 		// $NON-NLS-1$
		}

		if (app.hasAppLog) {
			String appLogName = NLS.bind(Messages.AppConsoleName, app.name);
			IOConsole appConsole = new SocketConsole(appLogName, app);
			consoles.add(appConsole);
			onNewConsole(appConsole);
			ConsolePlugin.getDefault().getConsoleManager().showConsoleView(appConsole);
		}
		else {
			MCLogger.logError("No app log is available for " + app.name); 			// $NON-NLS-1$
		}

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

		MCLogger.log(String.format("Creating new server console: %s of type %s", 				//$NON-NLS-1$
				console.getName(), console.getClass().getSimpleName()));

		consoleManager.addConsoles(new IConsole[] { console });
	}
}
