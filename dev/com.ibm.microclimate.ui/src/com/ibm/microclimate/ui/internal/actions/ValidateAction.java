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

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.SelectionProviderAction;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.ui.internal.messages.Messages;

/**
 * Action for initiating the validation of a Microclimate application.  This
 * action only shows when auto build is disabled on the project.  When auto
 * build is enabled, a build occurs every time a change is made and validation
 * is run automatically on every build.
 */
public class ValidateAction extends SelectionProviderAction {
	
	MicroclimateApplication app;
	
	public ValidateAction(ISelectionProvider selectionProvider) {
        super(selectionProvider, Messages.ValidateLabel);
        selectionChanged(getStructuredSelection());
    }

    @Override
    public void selectionChanged(IStructuredSelection sel) {
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof MicroclimateApplication) {
				app = (MicroclimateApplication) obj;
				if (app.isAvailable()) {
					setEnabled(true);
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
			MCLogger.logError("ValidateAction ran but no application was selected");
			return;
		}

		try {
			app.mcConnection.requestValidate(app);
		} catch (Exception e) {
			MCLogger.logError("Error requesting validation for application: " + app.name, e); //$NON-NLS-1$
		}
    }
    
    public boolean showAction() {
    	return app != null && !app.isAutoBuild();
    }

}
