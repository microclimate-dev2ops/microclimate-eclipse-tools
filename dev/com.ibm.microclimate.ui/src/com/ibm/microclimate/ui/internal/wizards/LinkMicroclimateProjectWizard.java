package com.ibm.microclimate.ui.internal.wizards;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.wst.server.core.IServer;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.MicroclimateConnection;
import com.ibm.microclimate.core.internal.MicroclimateConnectionManager;
import com.ibm.microclimate.core.internal.server.MicroclimateServerFactory;
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

	private NewMicroclimateConnectionPage newConnectionPage;
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
		setWindowTitle("Link to Microclimate Project");
		
		if (MicroclimateConnectionManager.connectionsCount() < 1 ) {
			newConnectionPage = new NewMicroclimateConnectionPage();
			addPage(newConnectionPage);
		}

		newProjectPage = new LinkMicroclimateProjectPage(selectedProject.getName());
		addPage(newProjectPage);
	}

	@Override
	public boolean canFinish() {
		return newProjectPage.canFinish();
	}

	@Override
	public boolean performFinish() {
		MicroclimateApplication appToLink = newProjectPage.getSelectedApp();

		IPath mcAppPath = appToLink.fullLocalPath;
		String mcAppPathStr = mcAppPath.toOSString();

		IPath eclipseProjPath = selectedProject.getLocation();
		String eclipseProjPathStr = eclipseProjPath.toOSString();

		boolean isSamePath = mcAppPath.equals(eclipseProjPath);
		// Eclipse's IPath.equal() above doesn't seem to handle path case differences properly 
		// on Windows so do a further check
		if ( System.getProperty("os.name").startsWith("Windows")) {
			isSamePath = mcAppPathStr.equalsIgnoreCase(eclipseProjPathStr);
		}

		MCLogger.log(String.format("Link %s to %s - same? %b\n",
				mcAppPathStr, eclipseProjPathStr,  isSamePath));

		if (!isSamePath) {
			String failedMsg = "The project paths do not match:\n"
					+ "Microclimate: %s\n"
					+ "Eclipse:      %s\n"
					+ "Please re-create the Eclipse project at the same location "
					+ "as the Microclimate project you wish to link it with.";
			failedMsg = String.format(failedMsg, mcAppPathStr, eclipseProjPathStr);

			MessageDialog.openError(getShell(), "Linking failed", failedMsg);
			return false;
		}

		try {
			doLink(appToLink);
		}
		catch(CoreException e) {
			MCLogger.logError(e);
			MessageDialog.openError(getShell(), "Error creating Microclimate Server", e.getMessage());
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
				+ "Server \"%s\" is now available in the Servers view, and its logs in the Console view.";
		successMsg = String.format(successMsg, selectedProject.getName(), appToLink.name, newServer.getName());

		MessageDialog.openInformation(getShell(), "Linking Complete", successMsg);
	}

}
