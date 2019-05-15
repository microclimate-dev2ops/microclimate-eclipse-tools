/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *	 IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.actions;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.SelectionProviderAction;

import com.ibm.microclimate.core.internal.InstallUtil;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.ProcessHelper;
import com.ibm.microclimate.core.internal.ProcessHelper.ProcessResult;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.ui.MicroclimateUIPlugin;
import com.ibm.microclimate.ui.internal.views.ViewHelper;

/**
 * Action to create a new project.
 */
public class ActivateAction extends SelectionProviderAction {

	protected MicroclimateConnection connection;
	
	public ActivateAction(ISelectionProvider selectionProvider) {
		super(selectionProvider, "Activate");
		selectionChanged(getStructuredSelection());
	}

	@Override
	public void selectionChanged(IStructuredSelection sel) {
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof MicroclimateConnection) {
				connection = (MicroclimateConnection)obj;
				setEnabled(!connection.isConnected());
				return;
			}
		}
		setEnabled(false);
	}

	@Override
	public void run() {
		if (connection == null) {
			// should not be possible
			MCLogger.logError("ActivateAction ran but no Microclimate connection was selected");
			return;
		}

		try {
			Job job = new Job("Activating Codewind") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					// Try to stop Codewind
					Process startProcess = null;
					try {
						startProcess = InstallUtil.startCodewind();
						ProcessResult result = ProcessHelper.waitForProcess(startProcess, 500, 60, monitor, "Starting Codewind");
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
					ViewHelper.refreshMicroclimateExplorerView(null);
					return Status.OK_STATUS;
				}
			};
			job.schedule();
		} catch (Exception e) {
			MCLogger.logError("An error occurred activating connection: " + connection.baseUrl, e);
		}
	}
}
