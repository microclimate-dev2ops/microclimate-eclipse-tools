/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
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

		setDefaultPageImageDescriptor(MicroclimateUIPlugin.getIcon(MicroclimateUIPlugin.MICROCLIMATE_BANNER_PATH));

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

		return true;
	}
}
