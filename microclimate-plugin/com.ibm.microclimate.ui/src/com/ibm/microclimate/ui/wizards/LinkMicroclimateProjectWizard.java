package com.ibm.microclimate.ui.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.wst.server.core.IServer;

import com.ibm.microclimate.core.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.server.MicroclimateServerFactory;
import com.ibm.microclimate.ui.Activator;

/**
 * This wizard allows the user to select a Microclimate project, then links it the Eclipse project whose context menu
 * was used to launch the wizard. Upon finishing successfully, a MicroclimateServer representing the selected
 * project is created and available in the UI.
 *
 * @author timetchells@ibm.com
 *
 */
public class LinkMicroclimateProjectWizard extends Wizard implements INewWizard {

	private IProject selectedProject;

	private LinkMicroclimateProjectPage newProjectPage;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		selectedProject = LinkMicroclimateProjectDelegate.getProjectFromSelection(selection);

		setDefaultPageImageDescriptor(Activator.getDefaultIcon());

		// TODO help
		setHelpAvailable(false);
	}

	@Override
	public void addPages() {
		setWindowTitle("Link Microclimate Project Wizard Window Title");

		newProjectPage = new LinkMicroclimateProjectPage();
		// newConnectionPage.setProject(project);
		addPage(newProjectPage);
	}

	@Override
	public boolean canFinish() {
		return newProjectPage.canFinish();
	}

	@Override
	public boolean performFinish() {
		MicroclimateApplication appToLink = newProjectPage.getSelectedApp();

		String mcAppPath = appToLink.fullLocalPath.toOSString();

		String eclipseProjPath = selectedProject.getLocation().toOSString();

		boolean isSamePath = mcAppPath.equals(eclipseProjPath);

		MCLogger.log(String.format("Link %s to %s - same? %b\n", mcAppPath, eclipseProjPath, isSamePath));

		if (!isSamePath) {
			String failedMsg = "The project paths do not match:\n"
					+ "Microclimate: %s\n"
					+ "Eclipse:      %s\n"
					+ "Please re-create the Eclipse project at the same location "
					+ "as the Microclimate project you wish to link it with.";
			failedMsg = String.format(failedMsg, eclipseProjPath, mcAppPath);

			MessageDialog.openError(getShell(), "Linking failed", failedMsg);
			return false;
		}

		try {
			doLink(appToLink);
		}
		catch(CoreException e) {
			MCLogger.logError(e);
			MessageDialog.openError(getShell(), "Error creating Microclimate Server/Application", e.getMessage());
		}

		return true;
	}

	/**
	 * Called when the wizard finishes.
	 * This creates the Server which is 'linked' to the given application.
	 */
	private void doLink(MicroclimateApplication appToLink) throws CoreException {
		IServer newServer = MicroclimateServerFactory.create(appToLink);

		String successMsg = "Linked project %s with Microclimate application %s.\n"
				+ "Server \"%s\" is now available in the Servers view.";
		successMsg = String.format(successMsg, selectedProject.getName(), appToLink.name, newServer.getName());

		MessageDialog.openInformation(getShell(), "Linking Complete", successMsg);
	}
}
