package com.ibm.microclimate.ui.internal.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.ibm.microclimate.ui.Activator;

/**
 * This wizard, which can be launched through the MC Preferences page or as a prerequisite to Linking a project,
 * allows the user to add at least one new MicroclimateConnection.
 *
 * @author timetchells@ibm.com
 *
 */
public class NewMicroclimateConnectionWizard extends Wizard implements INewWizard {

	private NewMicroclimateConnectionPage newConnectionPage;

	public NewMicroclimateConnectionWizard() {
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setDefaultPageImageDescriptor(Activator.getDefaultIcon());

		// TODO help
		setHelpAvailable(false);
	}

	@Override
	public void addPages() {
		setWindowTitle("New Microclimate Connection");
		newConnectionPage = new NewMicroclimateConnectionPage();
		addPage(newConnectionPage);
	}

	@Override
	public boolean canFinish() {
		return newConnectionPage.canFinish();
	}

	@Override
	public boolean performFinish() {
		if(!newConnectionPage.canFinish()) {
			return false;
		}

		return true;
	}
}
