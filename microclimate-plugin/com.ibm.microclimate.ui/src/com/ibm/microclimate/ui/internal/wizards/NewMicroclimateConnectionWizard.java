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

	// private IProject project;
	private NewMicroclimateConnectionPage newConnectionPage;

	private IStructuredSelection selection;
	private IWorkbench workbench;

	private boolean runLinkProjectWizardOnFinish;

	public NewMicroclimateConnectionWizard(boolean runLinkProjectWizardOnFinish) {
		this.runLinkProjectWizardOnFinish = runLinkProjectWizardOnFinish;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
		this.selection = selection;

		setDefaultPageImageDescriptor(Activator.getDefaultIcon());

		// TODO help
		setHelpAvailable(false);
	}

	@Override
	public void addPages() {
		setWindowTitle("New Microclimate Connection Wizard Window Title");
		newConnectionPage = new NewMicroclimateConnectionPage();
		// newConnectionPage.setProject(project);
		addPage(newConnectionPage);

		if (runLinkProjectWizardOnFinish) {
			// This means that the user did not launch this wizard directly, but that this wizard was
			// launched as a prerequisite to the Link project wizard.
			// So, we should tell them why they got this wizard and not the Link wizard.
			newConnectionPage.setMessage("You must connect to a Microclimate instance before you can Link a project.");
		}
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

		if(runLinkProjectWizardOnFinish) {
			getShell().close();

			WizardLauncher.launchWizard(new LinkMicroclimateProjectWizard(),
					selection, workbench, workbench.getActiveWorkbenchWindow().getShell());
		}

		return true;
	}
}
