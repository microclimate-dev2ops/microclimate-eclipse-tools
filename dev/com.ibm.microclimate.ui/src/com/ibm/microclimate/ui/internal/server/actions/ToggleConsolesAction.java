/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.server.actions;

import java.util.Set;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;

import com.ibm.microclimate.core.internal.MCEclipseApplication;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.server.console.MicroclimateConsoleFactory;

public class ToggleConsolesAction implements IObjectActionDelegate {

    protected MCEclipseApplication app;

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        if (!(selection instanceof IStructuredSelection)) {
            action.setEnabled(false);
            return;
        }

        IStructuredSelection sel = (IStructuredSelection) selection;
        if (sel.size() == 1) {
            Object obj = sel.getFirstElement();
            if (obj instanceof MicroclimateApplication) {
            	app = (MCEclipseApplication)obj;
            	action.setChecked(app.hasConsoles());
            	action.setEnabled(true);
            	return;
            }
        }
        action.setChecked(false);
        action.setEnabled(false);
    }

    @Override
    public void run(IAction action) {
        if (app == null) {
        	// should not be possible
        	MCLogger.logError("ToggleConsolesAction ran but no Microclimate application was selected");
			return;
		}

        if (action.isChecked()) {
        	Set<? extends IConsole> consoles = MicroclimateConsoleFactory.createApplicationConsoles(app);
        	app.setConsoles(consoles);
        } else {
        	Set<? extends IConsole> consoles = app.getConsoles();
        	if (consoles != null) {
	        	IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
	        	consoleManager.removeConsoles(consoles.toArray(new IConsole[consoles.size()]));
	        	app.setConsoles(null);
        	}
        }
    }

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		// nothing
	}
}
