/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jem.util.emf.workbench.ProjectUtilities;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.server.core.IServer;

import com.ibm.microclimate.core.MicroclimateCorePlugin;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnectionManager;
import com.ibm.microclimate.core.internal.server.MicroclimateServerFactory;
import com.ibm.microclimate.ui.MicroclimateUIPlugin;
import com.ibm.microclimate.ui.internal.Messages;

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
		selectedProject = getProjectFromSelection(selection);

		setDefaultPageImageDescriptor(MicroclimateUIPlugin.getIcon(MicroclimateUIPlugin.MICROCLIMATE_BANNER_PATH));

		// TODO help
		setHelpAvailable(false);
	}

	private static IProject getProjectFromSelection(ISelection selection) {
		if (selection == null) {
			MCLogger.logError("Null selection passed to getProjectFromSelection"); //$NON-NLS-1$
			return null;
		}
		IStructuredSelection structuredSelection = null;
		if (selection instanceof IStructuredSelection) {
			structuredSelection = (IStructuredSelection) selection;
		}
		else {
			MCLogger.logError("Non-structured selection passed to getProjectFromSelection"); //$NON-NLS-1$
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

	@Override
	public void addPages() {
		setWindowTitle(Messages.LinkWizard_ShellTitle);

		if (MicroclimateConnectionManager.activeConnectionsCount() < 1) {
			newConnectionPage = new NewMicroclimateConnectionPage();
			addPage(newConnectionPage);
		}

		newProjectPage = new LinkMicroclimateProjectPage(selectedProject, newConnectionPage == null);
		addPage(newProjectPage);
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		// Make sure the project link page is initialized
		MicroclimateConnection connection = null;
		if (page != null && page.equals(newConnectionPage)) {
			// Pass the connection specified in the connection page to the project link page
			connection = newConnectionPage.getMCConnection();
		}
		newProjectPage.init(connection);

		return super.getNextPage(page);
	}

	@Override
	public boolean canFinish() {
		return newProjectPage.canFinish();
	}

	@Override
	public boolean performFinish() {
		// Finish the connection page
		if (newConnectionPage != null) {
			newConnectionPage.performFinish();
		}

		// appToLink must be non-null to finish the wizard page.
		MicroclimateApplication appToLink = newProjectPage.getAppToLink();

		try {
			doLink(appToLink, newProjectPage.getSelectedProject());
		}
		catch(CoreException e) {
			MCLogger.logError(e);
			MessageDialog.openError(getShell(), Messages.LinkWizard_GenericErrorCreatingServer, e.getMessage());
		}

		return true;
	}

	private static final String
			// from org.eclipse.wst.server.ui.internal.ServerUIPlugin.VIEW_ID
			SERVERS_VIEW_ID = "org.eclipse.wst.server.ui.ServersView", //$NON-NLS-1$
			// from https://help.eclipse.org/photon/index.jsp?topic=%2Forg.eclipse.platform.doc.\
			// isv%2Freference%2Fapi%2Forg%2Feclipse%2Fui%2Fconsole%2FIConsoleConstants.html
			CONSOLE_VIEW_ID = "org.eclipse.ui.console.ConsoleView"; //$NON-NLS-1$


	/**
	 * Called when the wizard finishes.
	 * This creates the Server which is 'linked' to the given application.
	 */
	private void doLink(MicroclimateApplication appToLink, IProject project) throws CoreException {
		IServer newServer = MicroclimateServerFactory.create(appToLink);

		IPreferenceStore prefs = MicroclimateCorePlugin.getDefault().getPreferenceStore();

		// The user can choose to hide this dialog
		if(!prefs.getBoolean(MicroclimateCorePlugin.HIDE_ONFINISH_MSG_PREFSKEY)) {

			String successMsg = NLS.bind(Messages.LinkWizard_LinkedSuccessDialogMsg,
					new Object[] { project.getName(), appToLink.name, newServer.getName() });

			MessageDialogWithToggle dialog = MessageDialogWithToggle
					.openInformation(getShell(), Messages.LinkWizard_LinkSuccessDialogTitle, successMsg,
					Messages.LinkWizard_DontShowThisAgain, false,
					prefs, MicroclimateCorePlugin.HIDE_ONFINISH_MSG_PREFSKEY);

			// The call above is supposed to set this prefs key, but it doesn't seem to work.
			prefs.setValue(MicroclimateCorePlugin.HIDE_ONFINISH_MSG_PREFSKEY, dialog.getToggleState());
		}

		// Make sure the Console and Server views are open
		IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		activePage.showView(CONSOLE_VIEW_ID);
		activePage.showView(SERVERS_VIEW_ID);
	}

}
