package com.ibm.microclimate.ui.wizards;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.ibm.microclimate.core.internal.MicroclimateConnectionManager;

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

		// If there is not an existing connection, we must create one before a project can be linked
		if(MicroclimateConnectionManager.connectionsCount() < 1) {
			wizard = new NewMicroclimateConnectionWizard(true);
		}
		else {
			// TODO check if already linked, in this case there's no point in running the wizard
			wizard = new LinkMicroclimateProjectWizard();
		}

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
}
