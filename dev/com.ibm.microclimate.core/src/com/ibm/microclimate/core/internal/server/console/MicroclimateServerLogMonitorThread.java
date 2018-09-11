/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core.internal.server.console;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.eclipse.ui.console.IOConsoleOutputStream;

import com.ibm.microclimate.core.internal.MCLogger;

/**
 * This thread periodically checks the given input stream for new contents, and if it has changed,
 * propagates the new text to the given output stream.
 */
public class MicroclimateServerLogMonitorThread extends Thread {

	private static final int DELAY_MS = 1000;

	private final File inputFile;
	private BufferedReader inputReader;
	private final IOConsoleOutputStream output;

	private long fileLength = -1;

	private volatile boolean run = true;

	public MicroclimateServerLogMonitorThread(String consoleName, File inputFile, IOConsoleOutputStream output) {

		this.inputFile = inputFile;
		this.output = output;

		setPriority(Thread.MIN_PRIORITY + 1);
		setDaemon(true);
		setName(consoleName + " MonitorThread"); //$NON-NLS-1$
	}

	// From com.ibm.ws.st.core.internal.launch.ConsoleReader.update()
	@Override
	public void run() {
		while (run && !Thread.currentThread().isInterrupted()) {
			try {
				Thread.sleep(DELAY_MS);
			}
			catch(InterruptedException e) {}

			try {
		        // handle file roll-over or deletion
		        if (inputReader != null && fileLength != -1 && fileLength > inputFile.length()) {
		        	inputReader.close();
		        	inputReader = null;
		        }

		        // in case the reader was invalidated above
	            if (inputReader == null && inputFile.exists()) {
	            	/*
	                if (charset != null) {
	                	inputReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
	                }
	            	*/
	            	inputReader = new BufferedReader(new FileReader(inputFile));
	            }

	            if (inputReader == null) {
	            	// The file is missing
					return;
				}

		        fileLength = inputFile.length();

	            String s = inputReader.readLine();
	            while (s != null) {
	            	// MCLogger.log("New log output: " + s);
	            	output.write(s + System.lineSeparator());
	                s = inputReader.readLine();
	            }
			}
			catch(IOException e) {
				MCLogger.logError("Error updating server log for file " + inputFile.getAbsolutePath(), e); //$NON-NLS-1$
			}
		}

		try {
			if (inputReader != null) {
				inputReader.close();
			}
		}
		catch(IOException e) {
			MCLogger.logError(e);
		}
		// The owning IOConsole is responsible for closing its outputstream.
	}

	void disable() {
		run = false;
	}
}
