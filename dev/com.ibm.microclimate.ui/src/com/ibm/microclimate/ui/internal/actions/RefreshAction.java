/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
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
        	MicroclimateConnection connection = (MicroclimateConnection) microclimateObject;
        	connection.refreshApps(null);
        	ViewHelper.refreshMicroclimateExplorerView(connection);
        } else if (microclimateObject instanceof MicroclimateApplication) {
        	MicroclimateApplication app = (MicroclimateApplication) microclimateObject;
        	app.mcConnection.refreshApps(app.projectID);
        	ViewHelper.refreshMicroclimateExplorerView(app);
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
