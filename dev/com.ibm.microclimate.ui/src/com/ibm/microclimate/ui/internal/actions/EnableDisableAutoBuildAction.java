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

import com.ibm.microclimate.core.internal.MCEclipseApplication;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.constants.MCConstants;
import com.ibm.microclimate.ui.internal.messages.Messages;

/**
 * Action for enabling/disabling auto build on a Microclimate project.
 */
public class EnableDisableAutoBuildAction implements IObjectActionDelegate {

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
            	if (app.isAvailable()) {
	            	if (app.isAutoBuild()) {
	                	action.setText(Messages.DisableAutoBuildLabel);
	                } else {
	                	action.setText(Messages.EnableAutoBuildLabel);
	                }
		            action.setEnabled(true);
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
        	MCLogger.logError("EnableDisableAutoBuildAction ran but no Microclimate application was selected");
			return;
		}

        try {
        	String actionKey = app.isAutoBuild() ? MCConstants.VALUE_ACTION_DISABLEAUTOBUILD : MCConstants.VALUE_ACTION_ENABLEAUTOBUILD;
			app.mcConnection.requestProjectBuild(app, actionKey);
			app.setAutoBuild(!app.isAutoBuild());
		} catch (Exception e) {
			MCLogger.logError("Error initiating enable/disable for project: " + app.name, e); //$NON-NLS-1$
			MCUtil.openDialog(true, Messages.ErrorOnEnableDisableAutoBuildDialogTitle, e.getMessage());
			return;
		}
    }

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		// nothing
	}
}
