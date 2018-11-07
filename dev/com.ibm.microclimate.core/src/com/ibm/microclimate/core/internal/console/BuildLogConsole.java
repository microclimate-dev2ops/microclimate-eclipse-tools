/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core.internal.console;

import java.io.IOException;

import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;

import com.ibm.microclimate.core.MicroclimateCorePlugin;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;

public class BuildLogConsole extends IOConsole {
	
	private final MicroclimateApplication app;
	private final IOConsoleOutputStream outputStream;
	private double lastModified = 0;
	private final BuildLogMonitor monitor;
	private final Thread monitorThread;
	
	public BuildLogConsole(String name, MicroclimateApplication app) {
		super(name, MicroclimateConsoleFactory.MC_CONSOLE_TYPE,
				MicroclimateCorePlugin.getIcon(MicroclimateCorePlugin.DEFAULT_ICON_PATH),
				true);
		this.app = app;
		outputStream = newOutputStream();
		monitor = new BuildLogMonitor(this);
		monitorThread = new Thread(monitor, name);
		monitorThread.start();
	}
	
	public MicroclimateApplication getApp() {
		return app;
	}
	
	public boolean hasChanged(double timestamp) {
		if (timestamp > lastModified) {
			return true;
		}
		return false;
	}
	
	public synchronized void update(String content, double lastModified, boolean replace) {
		if (outputStream.isClosed()) {
			return;
		}
		MCLogger.log("Updating build log for: " + app.name); //$NON-NLS-1$
		this.lastModified = lastModified;
		if (replace) {
			clearConsole();
		}
		try {
			outputStream.write(content);
		} catch (IOException e) {
			MCLogger.logError("Failed to write to the build console for application: " + app.name, e); //$NON-NLS-1$
		}
	}

	@Override
	protected synchronized void dispose() {
		monitor.dispose();
		monitorThread.interrupt();
		try {
			outputStream.close();
		} catch (IOException e) {
			MCLogger.log("Failed to close output stream"); //$NON-NLS-1$
		}
		super.dispose();
	}
}
