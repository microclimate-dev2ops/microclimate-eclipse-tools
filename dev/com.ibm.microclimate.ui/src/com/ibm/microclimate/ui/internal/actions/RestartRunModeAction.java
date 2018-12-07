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
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;

import com.ibm.microclimate.core.internal.MCEclipseApplication;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.constants.AppState;
import com.ibm.microclimate.core.internal.constants.StartMode;
import com.ibm.microclimate.ui.internal.messages.Messages;

/**
 * Action to restart a Microclimate application in run mode.
 */
public class RestartRunModeAction implements IObjectActionDelegate, IViewActionDelegate, IActionDelegate2 {

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
            	if (app.isAvailable() && app.getProjectCapabilities().canRestart()) {
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
        	MCLogger.logError("RestartRunModeAction ran but no Microclimate application was selected");
			return;
		}

        try {
        	// Clear out any old launch and debug target
        	app.clearDebugger();
        	
        	// Restart the project in run mode
			app.mcConnection.requestProjectRestart(app, StartMode.RUN.startMode);
		} catch (Exception e) {
			MCLogger.logError("Error initiating restart for project: " + app.name, e); //$NON-NLS-1$
			MCUtil.openDialog(true, Messages.ErrorOnRestartDialogTitle, e.getMessage());
			return;
		}
    }

	@Override
	public void runWithEvent(IAction action, Event event) {
		run(action);
	}

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		// nothing
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(IAction arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(IViewPart arg0) {
		// TODO Auto-generated method stub
		
	}
}
