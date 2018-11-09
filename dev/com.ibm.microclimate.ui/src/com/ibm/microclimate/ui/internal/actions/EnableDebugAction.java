/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.actions;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.ibm.microclimate.core.internal.MCEclipseApplication;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.constants.AppState;
import com.ibm.microclimate.core.internal.constants.StartMode;
import com.ibm.microclimate.ui.internal.messages.Messages;

public class EnableDebugAction implements IObjectActionDelegate {

    protected MCEclipseApplication app;

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        if (!(selection instanceof IStructuredSelection)) {
            action.setEnabled(false);
            return;
        }

        IStructuredSelection sel = (IStructuredSelection) selection;
        if (sel.size() == 1) {
            Object obj = sel.getFirstElement();
            if (obj instanceof MCEclipseApplication) {
            	app = (MCEclipseApplication)obj;
            	if (app.isSupportedProject()) {
	            	if (app.getStartMode() == StartMode.RUN) {
	                	action.setText(Messages.RestartInDebugMode);
	                } else {
	                	action.setText(Messages.RestartInRunMode);
	                }
		            action.setEnabled(app.getAppState() == AppState.STARTED || app.getAppState() == AppState.STARTING);
	            	return;
            	}
            }
        }
        
        action.setEnabled(false);
    }

    @Override
    public void run(IAction action) {
        if (app == null) {
        	// should not be possible
        	MCLogger.logError("EnableDebugAction ran but no Microclimate application was selected");
			return;
		}

        try {
        	ILaunch launch = app.getLaunch();
        	if (launch != null) {
        		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        		launchManager.removeLaunch(launch);
        	}
        	app.setLaunch(null);
        	if (app.getStartMode() == StartMode.RUN) {
				app.mcConnection.requestProjectRestart(app, StartMode.DEBUG.startMode);
				app.setStartMode(StartMode.DEBUG);
			} else {
				app.mcConnection.requestProjectRestart(app, StartMode.RUN.startMode);
				app.setStartMode(StartMode.RUN);
			}
		} catch (Exception e) {
			MCLogger.logError("Error initiating restart for project: " + app.name, e); //$NON-NLS-1$
			MCUtil.openDialog(true, Messages.ErrorOnRestartDialogTitle, e.getMessage());
			return;
		}
    }

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		// nothing
	}
}
