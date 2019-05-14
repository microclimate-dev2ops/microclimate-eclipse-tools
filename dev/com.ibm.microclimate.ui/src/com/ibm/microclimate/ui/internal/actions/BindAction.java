/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *	 IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.actions;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.SelectionProviderAction;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.ui.MicroclimateUIPlugin;
import com.ibm.microclimate.ui.internal.views.ViewHelper;
import com.ibm.microclimate.ui.internal.wizards.BindProjectWizard;

/**
 * Action to create a new project.
 */
public class BindAction extends SelectionProviderAction {

	protected MicroclimateConnection connection;
	
	public BindAction(ISelectionProvider selectionProvider) {
		super(selectionProvider, "Add Project...");
		setImageDescriptor(MicroclimateUIPlugin.getDefaultIcon());
		selectionChanged(getStructuredSelection());
	}


	@Override
	public void selectionChanged(IStructuredSelection sel) {
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof MicroclimateConnection) {
				connection = (MicroclimateConnection)obj;
				setEnabled(connection.isConnected());
				return;
			}
		}
		setEnabled(false);
	}

	@Override
	public void run() {
		if (connection == null) {
			// should not be possible
			MCLogger.logError("BindAction ran but no Microclimate connection was selected");
			return;
		}

		try {
			BindProjectWizard wizard = new BindProjectWizard(connection);
			WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
			if (dialog.open() == Window.CANCEL) {
				return;
			}
			ViewHelper.openMicroclimateExplorerView();
			ViewHelper.refreshMicroclimateExplorerView(null);
			ViewHelper.expandConnection(connection);
		} catch (Exception e) {
			MCLogger.logError("An error occurred running the bind action on connection: " + connection.baseUrl, e);
		}
	}
}
