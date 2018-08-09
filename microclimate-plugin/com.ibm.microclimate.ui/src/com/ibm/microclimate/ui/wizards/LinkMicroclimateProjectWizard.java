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
import com.ibm.microclimate.core.internal.MicroclimateServer;
import com.ibm.microclimate.ui.Activator;

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

	private void doLink(MicroclimateApplication appToLink) throws CoreException {
		String serverName = "Microclimate Application: " + appToLink.name;

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

		newServer.setAttribute(MicroclimateServer.ATTR_HTTP_PORT, appToLink.httpPort);
		newServer.setAttribute(MicroclimateServer.ATTR_ROOT_URL, appToLink.rootUrl.toString());
		newServer.setAttribute(MicroclimateServer.ATTR_PROJ_ID, appToLink.id);
		newServer.saveAll(true, null);

		String successMsg = "Linked project %s with Microclimate application %s.\n"
				+ "Server \"%s\" is now available in the Servers view.";
		successMsg = String.format(successMsg, selectedProject.getName(), appToLink.name, serverName);

		MessageDialog.openInformation(getShell(), "Linking Complete", successMsg);
	}
}
