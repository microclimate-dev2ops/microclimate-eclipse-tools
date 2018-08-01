package com.ibm.microclimate.ui.wizards;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

public class WizardUtil {

	/**
	 * Useful when the wizard to be launched does not care about the workbench or selection.
	 * Do not use with the LinkMicroclimateProjectWizard since it requires these.
	 * @param wizard
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
