package com.ibm.microclimate.ui.internal.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.wst.server.core.IServer;

import com.ibm.microclimate.core.MicroclimateCorePlugin;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.server.MicroclimateServerFactory;
import com.ibm.microclimate.ui.MicroclimateUIPlugin;

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

		setDefaultPageImageDescriptor(MicroclimateUIPlugin.getDefaultIcon());

		// TODO help
		setHelpAvailable(false);
	}

	@Override
	public void addPages() {
		setWindowTitle("Link to Microclimate Project");

		newProjectPage = new LinkMicroclimateProjectPage(selectedProject);
		addPage(newProjectPage);
	}

	@Override
	public boolean canFinish() {
		return newProjectPage.canFinish();
	}

	@Override
	public boolean performFinish() {
		// appToLink must be non-null to finish the wizard page.
		MicroclimateApplication appToLink = newProjectPage.getAppToLink();

		try {
			doLink(appToLink, newProjectPage.getSelectedProject());
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
	private void doLink(MicroclimateApplication appToLink, IProject project) throws CoreException {
		IServer newServer = MicroclimateServerFactory.create(appToLink);

		IPreferenceStore prefs = MicroclimateCorePlugin.getDefault().getPreferenceStore();

		// The user can choose to hide this dialog
		if(!prefs.getBoolean(MicroclimateCorePlugin.HIDE_ONFINISH_MSG_PREFSKEY)) {

			String successMsg = String.format("Linked project %s with Microclimate application %s.\n"
					+ "Server \"%s\" is now available in the Servers view, and its logs in the Console view.",
					project.getName(), appToLink.name, newServer.getName());

			MessageDialogWithToggle dialog = MessageDialogWithToggle
					.openInformation(getShell(), "Linking Complete", successMsg,
					"Don't show this again", false,
					prefs, MicroclimateCorePlugin.HIDE_ONFINISH_MSG_PREFSKEY);

			// The call above is supposed to set this prefs key, but it doesn't seem to work.
			prefs.setValue(MicroclimateCorePlugin.HIDE_ONFINISH_MSG_PREFSKEY, dialog.getToggleState());
		}
	}

}
