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

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.SelectionProviderAction;

import com.ibm.microclimate.core.internal.MCEclipseApplication;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.constants.AppState;
import com.ibm.microclimate.core.internal.constants.ProjectType;
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
            	if (app.projectType.isLanguage(ProjectType.LANGUAGE_NODEJS)) {
            		this.setText(Messages.LaunchDebugSessionLabel);
            	} else {
            		this.setText(Messages.AttachDebuggerLabel);
            	}
            	if (app.isAvailable() && StartMode.DEBUG_MODES.contains(app.getStartMode()) && app.getDebugPort() != -1 &&
            			(app.getAppState() == AppState.STARTED || app.getAppState() == AppState.STARTING)) {
            		setEnabled(app.canAttachDebugger());
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
