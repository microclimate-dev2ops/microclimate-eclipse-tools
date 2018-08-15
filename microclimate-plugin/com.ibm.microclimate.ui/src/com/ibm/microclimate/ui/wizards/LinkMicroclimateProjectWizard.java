package com.ibm.microclimate.ui.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jem.util.emf.workbench.ProjectUtilities;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;

import com.ibm.microclimate.core.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.server.MicroclimateServer;
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
		selectedProject = getProjectFromSelection(selection);
		setDefaultPageImageDescriptor(Activator.getDefaultIcon());

		// TODO help
		setHelpAvailable(false);
	}

	private static IProject getProjectFromSelection(IStructuredSelection selection) {
		if (selection == null) {
			MCLogger.logError("Null selection passed to getProjectFromSelection");
			return null;
		}

		IProject project = ProjectUtilities.getProject(selection.getFirstElement());
		if (project == null){
			Object firstElement = selection.getFirstElement();
			if (firstElement instanceof IResource){
				project = ((IResource)firstElement).getProject();
			}
		}
		// If there are criteria which exclude certain projects, check those here,
		// and return null if the project is not valid
		return project;
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
			MessageDialog.openError(getShell(), "Error create Microclimate Server/Application", e.getMessage());
		}

		return true;
	}

	/**
	 * Called when the wizard finishes.
	 * This creates the Server which is 'linked' to the given application.
	 */
	private void doLink(MicroclimateApplication appToLink) throws CoreException {
		final String serverName = "Microclimate Application: " + appToLink.name;

		IServerType mcServerType = null;

		for (IServerType type : ServerCore.getServerTypes()) {
			if (MicroclimateServer.SERVER_ID.equals(type.getId())) {
				mcServerType = type;
			}
		}

		if (mcServerType == null) {
			MCLogger.logError("Didn't find MC Server Type!");
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					"Missing Server Type: " + MicroclimateServer.SERVER_ID));
		}

		IServerWorkingCopy newServer = mcServerType.createServer(null, null, null, null);		// TODO progress mon?
		newServer.setHost(appToLink.host);

		newServer.setName(serverName);

		// We can't pass Objects to the server framework - only primitives
		// so we provide the info needed for the Server to look up the relevant Microclimate app
		newServer.setAttribute(MicroclimateServer.ATTR_HTTP_PORT, appToLink.getHttpPort());
		newServer.setAttribute(MicroclimateServer.ATTR_APP_URL, appToLink.rootUrl.toString());
		newServer.setAttribute(MicroclimateServer.ATTR_PROJ_ID, appToLink.projectID);

		// The server will determine the corresponding MCConnection from the baseUrl
		newServer.setAttribute(MicroclimateServer.ATTR_MCC_URL, appToLink.mcConnection.baseUrl);

		// Store the name of the Eclipse project linked to this server. This will be used by the
		// launch configuration to locate the source code when debugging
		newServer.setAttribute(MicroclimateServer.ATTR_ECLIPSE_PROJECT_NAME, appToLink.name);

		// Creates the server in the Servers view
		newServer.saveAll(true, null);

		String successMsg = "Linked project %s with Microclimate application %s.\n"
				+ "Server \"%s\" is now available in the Servers view.";
		successMsg = String.format(successMsg, selectedProject.getName(), appToLink.name, serverName);

		MessageDialog.openInformation(getShell(), "Linking Complete", successMsg);
	}
}
