package com.ibm.microclimate.ui.internal.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jem.util.emf.workbench.ProjectUtilities;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.MicroclimateConnectionManager;
import com.ibm.microclimate.core.internal.server.MicroclimateServer;

/**
 *
 * This delegate handles the action when a user selects the Microclimate > Link Project context menu item.
 * Adapted from http://www.eclipse.org/articles/article.php?file=Article-JFaceWizards/index.html
 *
 * @author timetchells@ibm.com
 *
 */
public class LinkMicroclimateProjectDelegate implements IObjectActionDelegate {

	private IWorkbenchPart part;
	private ISelection selection;

	@Override
	public void run(IAction arg0) {

		INewWizard wizard;

		IProject selectedProject = getProjectFromSelection(selection);

		// If the project is already linked, don't run the wizard.
		String alreadyLinkedServer = isProjectAlreadyLinked(selectedProject);
		if (alreadyLinkedServer != null) {
			String alreadyLinkedMsg = String.format("%s is already linked to server \"%s\".\n"
					+ "If you wish to unlink this project, delete this server from the Servers view.",
					selectedProject.getName(), alreadyLinkedServer);

			MCUtil.openDialog(false, "Project already linked", alreadyLinkedMsg);

			// Don't launch the wizard.
			return;
		}

		wizard = new LinkMicroclimateProjectWizard();
		
		WizardLauncher.launchWizard(wizard,
				selection,
				part.getSite().getWorkbenchWindow().getWorkbench(),
				part.getSite().getShell());
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart part) {
		this.part = part;
	}

	public static IProject getProjectFromSelection(ISelection selection) {
		if (selection == null) {
			MCLogger.logError("Null selection passed to getProjectFromSelection");
			return null;
		}
		IStructuredSelection structuredSelection = null;
		if (selection instanceof IStructuredSelection) {
			structuredSelection = (IStructuredSelection) selection;
		}
		else {
			MCLogger.logError("Non-structured selection passed to getProjectFromSelection");
			return null;
		}

		IProject project = ProjectUtilities.getProject(structuredSelection.getFirstElement());
		if (project == null){
			Object firstElement = structuredSelection.getFirstElement();
			if (firstElement instanceof IResource){
				project = ((IResource)firstElement).getProject();
			}
		}
		// If there are criteria which exclude certain projects, check those here,
		// and return null if the project is not valid
		return project;
	}

	/**
	 * Loop over Microclimate Servers, and see if any of them has its project attribute
	 * set to the same name as this project. In this case there's no point in running the wizard.
	 *
	 * If the user renames the project after creating the server, this will fail and return null, but
	 * they will still be blocked from linking when they try to Finish this wizard, so I think that is acceptable.
	 *
	 * @return
	 * 	The name of the server that the given project is already linked to,
	 * 	or null if the project is not yet linked.
	 */
	private static String isProjectAlreadyLinked(IProject project) {
		for (IServer server : ServerCore.getServers()) {
			if (!MicroclimateServer.SERVER_ID.equals(server.getServerType().getId())) {
				// not a MC server
				continue;
			}

			final String serverProjectName = server.getAttribute(MicroclimateServer.ATTR_ECLIPSE_PROJECT_NAME, "");
			if (serverProjectName.isEmpty()) {
				MCLogger.logError("MC Server " + server.getName() + " didn't have an Eclipse Project attribute");
				continue;
			}

			if (serverProjectName.equals(project.getName())) {
				return server.getName();
			}
		}
		return null;
	}
}
