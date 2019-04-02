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

import java.io.IOException;

import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;

import com.ibm.microclimate.core.MicroclimateCorePlugin;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.connection.MicroclimateSocket;
import com.ibm.microclimate.core.internal.messages.Messages;

public class OldSocketConsole extends IOConsole {

	public final String projectID;
	private final MicroclimateSocket socket;

	private IOConsoleOutputStream outputStream;
	private int previousLength = 0;
	private boolean isInitialized = false;

	public OldSocketConsole(String name, MicroclimateApplication app) {
		super(name, MicroclimateConsoleFactory.MC_CONSOLE_TYPE,
				MicroclimateCorePlugin.getIcon(MicroclimateCorePlugin.DEFAULT_ICON_PATH),
				true);

		this.projectID = app.projectID;
		this.outputStream = newOutputStream();
		this.socket = app.mcConnection.getMCSocket();
		socket.registerOldSocketConsole(this);

		try {
			this.outputStream.write(Messages.LogFileInitialMsg);
		} catch (IOException e) {
			MCLogger.logError("Error writing initial message to " + this.getName(), e);
		}
	}

	public void update(String contents) throws IOException {
		if (!isInitialized) {
			// Clear the INITIAL_MSG
			clearConsole();
			isInitialized = true;
		}

		String newContents = "";
		int diff = contents.length() - previousLength;
		if (diff == 0) {
			// nothing to do
			return;
		}
		else if (diff < 0) {
			// The app log was cleared
			// eg if the dockerfile was changed and the container had to be rebuilt
			MCLogger.log("Console was cleared");
			clearConsole();
			// write the whole new console
			newContents = contents;
		}
		else {
			// write only the new characters to the console
			newContents = contents.substring(previousLength, previousLength + diff);
		}

		MCLogger.log(newContents.length() + " new characters to write to " + this.getName());		// $NON-NLS-1$
		outputStream.write(newContents);
		previousLength = contents.length();
	}

	@Override
	protected void dispose() {
		MCLogger.log("Dispose console " + getName()); //$NON-NLS-1$

		socket.deregisterOldSocketConsole(this);

		try {
			outputStream.close();
		} catch (IOException e) {
			MCLogger.logError("Error closing console output stream", e); //$NON-NLS-1$
		}

		super.dispose();
	}
}
