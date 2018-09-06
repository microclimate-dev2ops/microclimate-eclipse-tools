package com.ibm.microclimate.ui.internal.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.ibm.microclimate.core.internal.MicroclimateConnection;
import com.ibm.microclimate.core.internal.MicroclimateConnectionManager;
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

	private IWorkbench workbench;
	private IStructuredSelection selection;

	private final boolean launchLinkWizardOnFinish;

	public NewMicroclimateConnectionWizard(boolean launchLinkWizardOnFinish) {
		this.launchLinkWizardOnFinish = launchLinkWizardOnFinish;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setDefaultPageImageDescriptor(Activator.getDefaultIcon());
		this.selection = selection;

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

		if (launchLinkWizardOnFinish) {
			getShell().close();

			WizardLauncher.launchWizard(new LinkMicroclimateProjectWizard(), selection, workbench,
					Display.getDefault().getActiveShell());
		}

		return true;
	}
}
