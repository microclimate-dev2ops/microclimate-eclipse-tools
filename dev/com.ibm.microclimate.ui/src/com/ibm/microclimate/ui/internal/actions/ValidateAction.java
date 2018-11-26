/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
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
				if (app.isEnabled()) {
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
