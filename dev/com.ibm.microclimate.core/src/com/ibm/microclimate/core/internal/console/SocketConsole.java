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

package com.ibm.microclimate.core.internal.console;

import java.io.IOException;

import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;

import com.ibm.microclimate.core.MicroclimateCorePlugin;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.connection.MicroclimateSocket;
import com.ibm.microclimate.core.internal.messages.Messages;

public class SocketConsole extends IOConsole {

	public final MicroclimateApplication app;
	public final ProjectLogInfo logInfo;
	private final MicroclimateSocket socket;

	private IOConsoleOutputStream outputStream;
	private boolean isInitialized = false;
	private boolean showOnUpdate = false;

	public SocketConsole(String consoleName, ProjectLogInfo logInfo, MicroclimateApplication app) {
		super(consoleName, MicroclimateConsoleFactory.MC_CONSOLE_TYPE,
				MicroclimateCorePlugin.getIcon(MicroclimateCorePlugin.DEFAULT_ICON_PATH),
				true);

		this.app = app;
		this.logInfo = logInfo;
		this.outputStream = newOutputStream();
		this.socket = app.mcConnection.getMCSocket();
		socket.registerSocketConsole(this);

		try {
			this.outputStream.write(Messages.LogFileInitialMsg);
			app.mcConnection.requestEnableLogStream(app, logInfo);
		} catch (IOException e) {
			MCLogger.logError("Error opening console output stream for: " + this.getName(), e);
		}
	}

	public void update(String contents, boolean reset) throws IOException {
		if (!isInitialized || reset) {
			clearConsole();
			isInitialized = true;
		}

		MCLogger.log("Appending contents to log: " + this.getName());		// $NON-NLS-1$
		outputStream.write(contents);
		if (showOnUpdate) {
			activate();
		}
	}

	@Override
	protected void dispose() {
		MCLogger.log("Dispose console " + getName()); //$NON-NLS-1$

		socket.deregisterSocketConsole(this);

		try {
			app.mcConnection.requestDisableLogStream(app, logInfo);
			outputStream.close();
		} catch (IOException e) {
			MCLogger.logError("Error closing console output stream for: " + this.getName(), e); //$NON-NLS-1$
		}

		super.dispose();
	}
	
	public void setShowOnUpdate(boolean value) {
		showOnUpdate = value;
	}
}
