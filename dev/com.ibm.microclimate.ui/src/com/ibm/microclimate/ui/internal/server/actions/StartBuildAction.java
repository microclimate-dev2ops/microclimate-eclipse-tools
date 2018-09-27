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
import org.eclipse.wst.server.core.IServer;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.server.MicroclimateServerBehaviour;
import com.ibm.microclimate.ui.internal.Messages;

public class StartBuildAction implements IObjectActionDelegate {

	protected MicroclimateServerBehaviour mcServer;

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (!(selection instanceof IStructuredSelection)) {
			action.setEnabled(false);
			return;
		}

		IStructuredSelection sel = (IStructuredSelection) selection;
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof IServer) {
				IServer srv = (IServer) obj;
				mcServer = (MicroclimateServerBehaviour) srv.loadAdapter(MicroclimateServerBehaviour.class, null);
				action.setEnabled(mcServer != null && mcServer.isStarted());
				return;
			}
		}
		action.setEnabled(false);
	}

	@Override
	public void run(IAction action) {
		if (mcServer == null) {
			// should not be possible
			MCLogger.logError("StartBuildAction ran but no MCServer was selected");
			return;
		}

		MicroclimateApplication app = mcServer.getApp();
		// Shouldn't happen because the action is disabled for non-started servers, but just in case:
		if (app == null || !mcServer.isStarted() || app.getBaseUrl() == null) {
			MCUtil.openDialog(true, Messages.StartBuildAction_AppNotStartedTitle,
					Messages.StartBuildAction_AppNotStartedMsg);
			return;
		}

		try {
			app.mcConnection.requestProjectBuild(app);
		} catch (Exception e) {
			MCLogger.logError("Error requesting build for application: " + app.name, e); //$NON-NLS-1$
		}
	}

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		// nothing
	}
}
