/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.constants.BuildStatus;
import com.ibm.microclimate.core.internal.constants.MCConstants;
import com.ibm.microclimate.ui.internal.messages.Messages;

/**
 * Action to start an application build.
 */
public class StartBuildAction implements IObjectActionDelegate {

	protected MicroclimateApplication app;

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (!(selection instanceof IStructuredSelection)) {
			action.setEnabled(false);
			return;
		}

		IStructuredSelection sel = (IStructuredSelection) selection;
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof MicroclimateApplication) {
				app = (MicroclimateApplication) obj;
				if (app.isEnabled() && app.getBuildStatus() != BuildStatus.IN_PROGRESS && app.getBuildStatus() != BuildStatus.QUEUED) {
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
			MCLogger.logError("StartBuildAction ran but no application was selected");
			return;
		}

		try {
			app.mcConnection.requestProjectBuild(app, MCConstants.VALUE_ACTION_BUILD);
		} catch (Exception e) {
			MCLogger.logError("Error requesting build for application: " + app.name, e); //$NON-NLS-1$
			MCUtil.openDialog(true, NLS.bind(Messages.StartBuildError, app.name), e.getMessage());
			return;
		}
	}

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		// nothing
	}
}
