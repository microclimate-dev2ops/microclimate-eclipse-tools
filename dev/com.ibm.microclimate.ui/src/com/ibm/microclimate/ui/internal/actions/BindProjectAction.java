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

package com.ibm.microclimate.ui.internal.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.ibm.microclimate.core.internal.InstallUtil;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.PlatformUtil;
import com.ibm.microclimate.core.internal.ProcessHelper;
import com.ibm.microclimate.core.internal.ProcessHelper.ProcessResult;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnectionManager;
import com.ibm.microclimate.ui.internal.views.ViewHelper;
import com.ibm.microclimate.ui.internal.wizards.BindProjectWizard;

public class BindProjectAction implements IObjectActionDelegate {
	
	private IWorkbenchPart part;
	private IProject project;

	@Override
	public void run(IAction action) {
		if (project == null) {
			// Should not happen
			MCLogger.logError("BindProjectAction ran but no project was selected");
			return;
		}
		
		MicroclimateConnection connection = setupConnection();
		if (connection == null || !connection.isConnected()) {
			MCUtil.openDialog(true, "Add Project Error", "Adding the project failed because a connection to Codewind could not be established. Check workspace logs for details.");
			return;
		}
		
		String projectError = getProjectError(connection, project);
		if (projectError != null) {
			MCUtil.openDialog(true, "Add Project Error", projectError);
			// If connection is new (not already registered), then close it
			if (MicroclimateConnectionManager.getActiveConnection(connection.baseUrl.toString()) == null) {
				connection.close();
			}
			return;
		}
		
		BindProjectWizard wizard = new BindProjectWizard(connection, project);
		WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
		if (dialog.open() == Window.CANCEL) {
			// If connection is new (not already registered), then close it
			if (MicroclimateConnectionManager.getActiveConnection(connection.baseUrl.toString()) == null) {
				connection.close();
			}
		} else {
			// Add the connection if not already registered
			if (MicroclimateConnectionManager.getActiveConnection(connection.baseUrl.toString()) == null) {
				MicroclimateConnectionManager.add(connection);
			}
			ViewHelper.openMicroclimateExplorerView();
			ViewHelper.refreshMicroclimateExplorerView(null);
			ViewHelper.expandConnection(connection);
		}
	}
	
	private String getProjectError(MicroclimateConnection connection, IProject project) {
		if (connection.getAppByName(project.getName()) != null) {
			return "A Codewind project with the name " + project.getName() + " already exists.";
		}
		IPath workspacePath = connection.getWorkspacePath();
		IPath projectPath = project.getLocation();
		if (PlatformUtil.getOS() == PlatformUtil.OperatingSystem.WINDOWS) {
			workspacePath = new Path(workspacePath.toPortableString().toLowerCase());
			projectPath = new Path(projectPath.toPortableString().toLowerCase());
		}
		if (!workspacePath.isPrefixOf(projectPath)) {
			return "The " + project.getName() + " project is not located in the Codewind workspace: " + connection.getWorkspacePath().toOSString();
		}
		return null;
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
        if (!(selection instanceof IStructuredSelection)) {
            action.setEnabled(false);
            return;
        }

        IStructuredSelection sel = (IStructuredSelection) selection;
        if (sel.size() == 1) {
            Object obj = sel.getFirstElement();
            if (obj instanceof IProject) {
            	project = (IProject)obj;
            	action.setEnabled(project.isAccessible());
            	return;
            }
        }
        
        action.setEnabled(false);
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart part) {
		this.part = part;
	}
	
	
	private MicroclimateConnection setupConnection() {
		MicroclimateConnection connection = null;
		List<MicroclimateConnection> connections = MicroclimateConnectionManager.activeConnections();
		if (connections != null && !connections.isEmpty()) {
			connection = connections.get(0);
		} else {
			try {
				// Will throw an Exception if fails
				connection = MicroclimateConnectionManager.createConnection(MicroclimateConnectionManager.DEFAULT_CONNECTION_URL);
			} catch(Exception e) {
				MCLogger.log("Attempting to connect to Codewind failed: " + e.getMessage());
			}
		}
		if (connection != null && connection.isConnected()) {
			return connection;
		}
		
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				Process startProcess = null;
				try {
					startProcess = InstallUtil.startCodewind();
					ProcessResult result = ProcessHelper.waitForProcess(startProcess, 500, 60, monitor, "Starting Codewind");
					if (result.getExitValue() != 0) {
						throw new InvocationTargetException(null, "There was a problem trying to start Codewind: " + result.getError());
					}
				} catch (IOException e) {
					throw new InvocationTargetException(e, "An error occurred trying to start Codewind: " + e.getMessage());
				} catch (TimeoutException e) {
					throw new InvocationTargetException(e, "Codewind did not start in the expected time: " + e.getMessage());
				} finally {
					if (startProcess != null && startProcess.isAlive()) {
						startProcess.destroy();
					}
				}
				
			}
		};
		try {
			ProgressMonitorDialog dialog = new ProgressMonitorDialog(part.getSite().getShell());
			dialog.run(true, true, runnable);
		} catch (InvocationTargetException e) {
			MCLogger.logError("An error occurred trying to start Codewind", e);
			return null;
		} catch (InterruptedException e) {
			MCLogger.logError("Codewind start was interrupted", e);
			return null;
		}
		
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
			}
			if (!connection.isConnected()) {
				MCLogger.logError("The connection at " + connection.baseUrl + " is not active.");
				return null;
			}
			return connection;
		}
		
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
		}
		if (connection == null) {
			MCLogger.logError("Failed to connect to Codewind at: " + MicroclimateConnectionManager.DEFAULT_CONNECTION_URL);
		}
		return connection;
	}
}
