package com.ibm.microclimate.ui.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.ibm.microclimate.ui.Activator;

public class NewMicroclimateConnectionWizard extends Wizard implements INewWizard {

	// private IProject project;
	private NewMicroclimateConnectionPage newConnectionPage;

	private IStructuredSelection selection;
	private IWorkbench workbench;

	private boolean runLinkProjectWizardOnFinish;

	public NewMicroclimateConnectionWizard(boolean runLinkProjectWizardOnFinish) {
		// TODO perhaps display a message like "You must connect to a Microclimate instance before adding a project"
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
