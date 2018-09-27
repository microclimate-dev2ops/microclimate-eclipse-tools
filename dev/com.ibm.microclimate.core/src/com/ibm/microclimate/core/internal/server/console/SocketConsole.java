package com.ibm.microclimate.core.internal.server.console;

import java.io.IOException;

import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;

import com.ibm.microclimate.core.MicroclimateCorePlugin;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.connection.MicroclimateSocket;

public class SocketConsole extends IOConsole {

	// This message is displayed when the console is created, until the server sends the first set of logs.
	private static final String INITIAL_MSG = "Waiting for server to send logs...";

	public final String projectID;
	private final MicroclimateSocket socket;

	private IOConsoleOutputStream outputStream;
	private int previousLength = 0;
	private boolean isInitialized = false;

	public SocketConsole(String name, MicroclimateApplication app) {
		super(name, MicroclimateConsoleFactory.MC_CONSOLE_TYPE,
				MicroclimateCorePlugin.getIcon(MicroclimateCorePlugin.DEFAULT_ICON_PATH),
				true);

		this.projectID = app.projectID;
		this.outputStream = newOutputStream();
		this.socket = app.mcConnection.mcSocket;
		socket.registerSocketConsole(this);

		try {
			this.outputStream.write(INITIAL_MSG);
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

		int diff = contents.length() - previousLength;
		if (diff < 0) {
			MCLogger.logError("Negative console diff");
			diff = 0;
		}

		MCLogger.log(diff + " new characters to write to " + this.getName());		// $NON-NLS-1$
		if (diff == 0) {
			return;
		}

		String newContents = contents.substring(previousLength, previousLength + diff);
		outputStream.write(newContents);
		previousLength = contents.length();

	}

	@Override
	protected void dispose() {
		MCLogger.log("Dispose console " + getName()); //$NON-NLS-1$

		socket.deregisterSocketConsole(this);

		try {
			outputStream.close();
		} catch (IOException e) {
			MCLogger.logError("Error closing console output stream", e); //$NON-NLS-1$
		}

		super.dispose();
	}
}
