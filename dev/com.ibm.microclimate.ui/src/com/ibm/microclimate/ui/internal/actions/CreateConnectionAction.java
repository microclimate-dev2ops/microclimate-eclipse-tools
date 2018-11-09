/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import com.ibm.microclimate.ui.internal.wizards.NewMicroclimateConnectionWizard;
import com.ibm.microclimate.ui.internal.wizards.WizardLauncher;

public class CreateConnectionAction implements IViewActionDelegate, IActionDelegate2 {
	
	@Override
	public void run(IAction arg0) {
		Wizard wizard = new NewMicroclimateConnectionWizard();
		WizardLauncher.launchWizardWithoutSelection(wizard);
	}

	@Override
	public void selectionChanged(IAction arg0, ISelection arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(IAction arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void runWithEvent(IAction arg0, Event arg1) {
		Wizard wizard = new NewMicroclimateConnectionWizard();
		WizardLauncher.launchWizardWithoutSelection(wizard);
	}

	@Override
	public void init(IViewPart arg0) {
		// TODO Auto-generated method stub
	}

}