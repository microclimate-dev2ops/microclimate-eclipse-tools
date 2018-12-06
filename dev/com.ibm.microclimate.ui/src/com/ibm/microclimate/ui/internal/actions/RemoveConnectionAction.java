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

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnectionManager;

/**
 * Action to remove a Microclimate connection.
 */
public class RemoveConnectionAction implements IObjectActionDelegate {

	protected MicroclimateConnection connection;

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (!(selection instanceof IStructuredSelection)) {
			action.setEnabled(false);
			return;
		}

		IStructuredSelection sel = (IStructuredSelection) selection;
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof MicroclimateConnection) {
				connection = (MicroclimateConnection) obj;
				action.setEnabled(true);
				return;
			}
		}
		action.setEnabled(false);
	}

	@Override
	public void run(IAction action) {
		if (connection == null) {
			// should not be possible
			MCLogger.logError("RemoveConnectionAction ran but no connection was selected");
			return;
		}

		try {
			MicroclimateConnectionManager.removeConnection(connection.baseUrl.toString());
		} catch (Exception e) {
			MCLogger.logError("Error removing connection: " + connection.baseUrl.toString(), e); //$NON-NLS-1$
		}
	}

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		// nothing
	}
}
