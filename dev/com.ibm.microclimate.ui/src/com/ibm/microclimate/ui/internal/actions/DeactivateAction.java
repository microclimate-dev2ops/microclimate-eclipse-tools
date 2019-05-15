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
import com.ibm.microclimate.core.internal.connection.MicroclimateConnectionManager;
import com.ibm.microclimate.ui.MicroclimateUIPlugin;
import com.ibm.microclimate.ui.internal.views.ViewHelper;

/**
 * Action to create a new project.
 */
public class DeactivateAction extends SelectionProviderAction {

	protected MicroclimateConnection connection;
	
	public DeactivateAction(ISelectionProvider selectionProvider) {
		super(selectionProvider, "Deactivate");
		selectionChanged(getStructuredSelection());
	}

	@Override
	public void selectionChanged(IStructuredSelection sel) {
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof MicroclimateConnection) {
				connection = (MicroclimateConnection)obj;
				setEnabled(connection.isConnected());
				return;
			}
		}
		setEnabled(false);
	}

	@Override
	public void run() {
		if (connection == null) {
			// should not be possible
			MCLogger.logError("DeactivateAction ran but no Microclimate connection was selected");
			return;
		}

		try {
			MicroclimateConnectionManager.removeConnection(connection.baseUrl.toString());
			connection.close();
			ViewHelper.refreshMicroclimateExplorerView(null);
			Job job = new Job("Deactivating Codewind") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					// Try to stop Codewind
					Process stopProcess = null;
					try {
						stopProcess = InstallUtil.stopCodewind();
						ProcessResult result = ProcessHelper.waitForProcess(stopProcess, 500, 60, monitor, "Stopping Codewind");
						if (result.getExitValue() != 0) {
							return new Status(IStatus.ERROR, MicroclimateUIPlugin.PLUGIN_ID, "There was a problem trying to stop Codewind: " + result.getError());
						}
					} catch (IOException e) {
						return new Status(IStatus.ERROR, MicroclimateUIPlugin.PLUGIN_ID, "An error occurred trying to stop Codewind.", e);
					} catch (TimeoutException e) {
						return new Status(IStatus.ERROR, MicroclimateUIPlugin.PLUGIN_ID, "Codewind did not stop in the expected time.", e);
					} finally {
						if (stopProcess != null && stopProcess.isAlive()) {
							stopProcess.destroy();
						}
					}
					return Status.OK_STATUS;
				}
			};
			job.schedule();
		} catch (Exception e) {
			MCLogger.logError("An error occurred deactivating connection: " + connection.baseUrl, e);
		}
	}
}
