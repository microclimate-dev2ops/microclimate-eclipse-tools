/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.wizards;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

/**
 * Static utilities to eliminate some boilerplate for launching wizards.
 *
 * @author timetchells@ibm.com
 */
public class WizardLauncher {

	private WizardLauncher() {}

	/**
	 * Useful when the wizard to be launched does not care about the workbench or selection.
	 * Do not use with the LinkMicroclimateProjectWizard since it requires these.
	 */
	public static void launchWizardWithoutSelection(Wizard wizard) {
		WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
		dialog.create();
		dialog.open();
	}

	public static void launchWizard(INewWizard wizard, ISelection selection, IWorkbench workbench, Shell parentShell) {
		IStructuredSelection structuredSelection = null;
		if (selection instanceof IStructuredSelection) {
			structuredSelection = (IStructuredSelection) selection;
		}

		wizard.init(workbench, structuredSelection);

		WizardDialog dialog = new WizardDialog(parentShell, wizard);
		dialog.create();
		dialog.open();
	}
}
