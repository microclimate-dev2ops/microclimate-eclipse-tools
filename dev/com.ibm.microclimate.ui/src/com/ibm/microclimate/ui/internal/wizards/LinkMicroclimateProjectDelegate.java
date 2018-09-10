/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.wizards;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

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
}
