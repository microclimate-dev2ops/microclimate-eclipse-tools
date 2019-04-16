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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.ui.internal.messages.Messages;
import com.ibm.microclimate.ui.internal.views.ViewHelper;

/**
 * Refresh action for a Microclimate connection or application.  This retrieves the
 * latest information for the object from Microcliamte and updates the view.
 */
public class RefreshAction implements IObjectActionDelegate {

    protected Object microclimateObject;

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        if (!(selection instanceof IStructuredSelection)) {
            action.setEnabled(false);
            return;
        }

        IStructuredSelection sel = (IStructuredSelection) selection;
        if (sel.size() == 1) {
            Object obj = sel.getFirstElement();
            if (obj instanceof MicroclimateConnection || obj instanceof MicroclimateApplication) {
            	microclimateObject = obj;
            	action.setEnabled(true);
            	return;
            }
        }
        action.setEnabled(false);
    }

    @Override
    public void run(IAction action) {
        if (microclimateObject instanceof MicroclimateConnection) {
        	final MicroclimateConnection connection = (MicroclimateConnection) microclimateObject;
        	Job job = new Job(NLS.bind(Messages.RefreshConnectionJobLabel, connection.baseUrl.toString())) {
    			@Override
    			protected IStatus run(IProgressMonitor monitor) {
		        	connection.refreshApps(null);
		        	ViewHelper.refreshMicroclimateExplorerView(connection);
		        	return Status.OK_STATUS;
    			}
    		};
    		job.schedule();
        } else if (microclimateObject instanceof MicroclimateApplication) {
        	final MicroclimateApplication app = (MicroclimateApplication) microclimateObject;
        	Job job = new Job(NLS.bind(Messages.RefreshProjectJobLabel, app.name)) {
    			@Override
    			protected IStatus run(IProgressMonitor monitor) {
    				app.mcConnection.refreshApps(app.projectID);
    				ViewHelper.refreshMicroclimateExplorerView(app);
		        	return Status.OK_STATUS;
    			}
    		};
    		job.schedule();
        } else {
        	// Should not happen
        	MCLogger.logError("RefreshAction ran but no Microclimate object was selected");
        }
    }

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		// nothing
	}
}
