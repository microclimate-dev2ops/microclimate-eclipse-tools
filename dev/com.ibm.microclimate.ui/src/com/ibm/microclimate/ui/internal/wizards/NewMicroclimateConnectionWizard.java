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

package com.ibm.microclimate.ui.internal.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.ui.MicroclimateUIPlugin;
import com.ibm.microclimate.ui.internal.messages.Messages;
import com.ibm.microclimate.ui.internal.views.ViewHelper;

/**
 * This wizard, which can be launched through the MC Preferences page or from the New menu.
 */
public class NewMicroclimateConnectionWizard extends Wizard implements INewWizard {

	private NewMicroclimateConnectionPage newConnectionPage;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {

		setDefaultPageImageDescriptor(MicroclimateUIPlugin.getImageDescriptor(MicroclimateUIPlugin.MICROCLIMATE_BANNER));

		// TODO help
		setHelpAvailable(false);
	}

	@Override
	public void addPages() {
		setWindowTitle(Messages.NewConnectionWizard_ShellTitle);
		newConnectionPage = new NewMicroclimateConnectionPage();
		addPage(newConnectionPage);
	}

	@Override
	public boolean canFinish() {
		return newConnectionPage.getMCConnection() != null;
	}

	@Override
	public boolean performCancel() {
		MicroclimateConnection connection = newConnectionPage.getMCConnection();
		if (connection != null) {
			connection.close();
		}
		return true;
	}

	@Override
	public boolean performFinish() {
		if(!canFinish()) {
			return false;
		}

		newConnectionPage.performFinish();

		ViewHelper.openMicroclimateExplorerView();
		ViewHelper.refreshMicroclimateExplorerView(null);
		ViewHelper.expandConnection(newConnectionPage.getMCConnection());

		return true;
	}
}
