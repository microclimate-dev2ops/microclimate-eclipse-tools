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

package com.ibm.microclimate.ui.internal.actions;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import com.ibm.microclimate.core.internal.InstallUtil;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.ProcessHelper;
import com.ibm.microclimate.core.internal.ProcessHelper.ProcessResult;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnectionManager;
import com.ibm.microclimate.ui.MicroclimateUIPlugin;
import com.ibm.microclimate.ui.internal.messages.Messages;
import com.ibm.microclimate.ui.internal.views.ViewHelper;

/**
 * Action to create a new Microclimate connection. This action is used in several places
 * including:
 *    The File > New menu
 *    Popup menu in the Microclimate view
 *    Toolbar action in the Microclimate view
 *
 */
public class CreateConnectionAction extends Action implements IViewActionDelegate, IActionDelegate2 {
	
	public CreateConnectionAction(Shell shell) {
        super(Messages.ActionNewConnection);
        setImageDescriptor(MicroclimateUIPlugin.getImageDescriptor(MicroclimateUIPlugin.MICROCLIMATE_ICON));
    }

    public CreateConnectionAction() {
        // Intentionally empty
    }

	
	@Override
	public void run(IAction arg0) {
//		run();
		setupConnection();
	}

	@Override
	public void run() {
//		Wizard wizard = new NewMicroclimateConnectionWizard();
//		WizardLauncher.launchWizardWithoutSelection(wizard);
		List<MicroclimateConnection> connections = MicroclimateConnectionManager.activeConnections();
		if (connections != null && !connections.isEmpty() && connections.get(0).isConnected()) {
			MCUtil.openDialog(false, "Connection Exists", "An active connection to Codewind already exists.");
			return;
		}
		setupConnection();
	}

	@Override
	public void selectionChanged(IAction arg0, ISelection arg1) {
		// Intentionally empty
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(IAction arg0) {
		// Intentionally empty
	}

	@Override
	public void runWithEvent(IAction arg0, Event arg1) {
		run();
	}

	@Override
	public void init(IViewPart arg0) {
		// Intentionally empty
	}
	
    public static void setupConnection() {
		List<MicroclimateConnection> connections = MicroclimateConnectionManager.activeConnections();
		MicroclimateConnection conn = null;
		if (connections != null && !connections.isEmpty()) {
			conn = connections.get(0);
			if (conn.isConnected()) {
				return;
			}
		}
		final MicroclimateConnection existingConnection = conn;
		
    	Job job = new Job("Connecting to Codewind") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				SubMonitor mon = SubMonitor.convert(monitor, 100);
				mon.setTaskName("Detecting Codewind");
				MicroclimateConnection connection = existingConnection;
				if (connection == null) {
					// Try to create a connection
					try {
						connection = MicroclimateConnectionManager.createConnection(MicroclimateConnectionManager.DEFAULT_CONNECTION_URL);
					} catch(Exception e) {
						MCLogger.log("Attempting to connect to Codewind failed: " + e.getMessage());
					}
				}
				mon.worked(5);
				
				if (connection == null || !connection.isConnected()) {
					// Try to start Codewind
					Process startProcess = null;
					try {
						SubMonitor subMon = mon.split(75);
						startProcess = InstallUtil.startCodewind();
						ProcessResult result = ProcessHelper.waitForProcess(startProcess, 500, 60, subMon, "Starting Codewind");
						if (result.getExitValue() != 0) {
							return new Status(IStatus.ERROR, MicroclimateUIPlugin.PLUGIN_ID, "There was a problem trying to start Codewind: " + result.getError());
						}
					} catch (IOException e) {
						return new Status(IStatus.ERROR, MicroclimateUIPlugin.PLUGIN_ID, "An error occurred trying to start Codewind.", e);
					} catch (TimeoutException e) {
						return new Status(IStatus.ERROR, MicroclimateUIPlugin.PLUGIN_ID, "Codewind did not start in the expected time.", e);
					} finally {
						if (startProcess != null && startProcess.isAlive()) {
							startProcess.destroy();
						}
					}
				}
				if (mon.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				
				mon.setTaskName("Connecting to Codewind");
				// If there was a connection, check to see if it is connected to Codewind now
				if (connection != null) {
					for (int i = 0; i < 10; i++) {
						if (connection.isConnected()) {
							break;
						}
						try {
							Thread.sleep(500);
						} catch (InterruptedException e1) {
							// Ignore
						}
						if (mon.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
						mon.worked(1);
					}
					if (!connection.isConnected()) {
						if (connection != existingConnection) {
							connection.close();
						}
						MCLogger.logError("The connection at " + connection.baseUrl + " is not active.");
						return new Status(IStatus.ERROR, MicroclimateUIPlugin.PLUGIN_ID, "Codewind was started but the connection is not active.");
					}
				} else {
					// If there was no connection, try to create one
					for (int i = 0; i < 10; i++) {
						try {
							connection = MicroclimateConnectionManager.createConnection(MicroclimateConnectionManager.DEFAULT_CONNECTION_URL);
							break;
						} catch (Exception e) {
							try {
								Thread.sleep(500);
							} catch (InterruptedException e1) {
								// Ignore
							}
						}
						if (mon.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
						mon.worked(1);
					}
					if (connection == null) {
						return new Status(IStatus.ERROR, MicroclimateUIPlugin.PLUGIN_ID, "Codewind was started but a connection could not be created.");
					}
				}
				
				if (MicroclimateConnectionManager.getActiveConnection(connection.baseUrl.toString()) == null) {
					MicroclimateConnectionManager.add(connection);
				}
				ViewHelper.refreshMicroclimateExplorerView(null);
				ViewHelper.expandConnection(connection);
				
				return Status.OK_STATUS;
		    }
    	};
    	job.schedule();
    }

}
