/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.server.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.ui.internal.views.ViewHelper;

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
