/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.actions;

import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.SelectionProviderAction;

import com.ibm.microclimate.core.internal.MCEclipseApplication;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.constants.AppState;
import com.ibm.microclimate.core.internal.constants.StartMode;
import com.ibm.microclimate.ui.internal.messages.Messages;

/**
 * Action for attaching the debugger.  This action is only enabled if the
 * application is in debug mode and is starting or started and a debugger
 * is not already attached.
 */
public class AttachDebuggerAction extends SelectionProviderAction {
	
	MCEclipseApplication app;
	
	public AttachDebuggerAction(ISelectionProvider selectionProvider) {
        super(selectionProvider, Messages.AttachDebuggerLabel);
        selectionChanged(getStructuredSelection());
    }

    @Override
    public void selectionChanged(IStructuredSelection sel) {
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof MCEclipseApplication) {
            	app = (MCEclipseApplication) obj;
            	if (app.isEnabled() && StartMode.DEBUG_MODES.contains(app.getStartMode()) &&
            			(app.getAppState() == AppState.STARTED || app.getAppState() == AppState.STARTING)) {
            		IDebugTarget debugTarget = app.getDebugTarget();
            		setEnabled(debugTarget == null || debugTarget.isDisconnected());
            		return;
            	}
            }
		}
		setEnabled(false);
    }

    @Override
    public void run() {
    	if (app == null) {
			// should not be possible
			MCLogger.logError("AttachDebuggerAction ran but no application was selected");
			return;
		}

		app.attachDebugger();
    }
    
    public boolean showAction() {
    	// Don't show the action if the app does not support debug
    	return (app != null && app.supportsDebug());
    }

}
