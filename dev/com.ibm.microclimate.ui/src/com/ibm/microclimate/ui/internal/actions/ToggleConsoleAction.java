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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;

import com.ibm.microclimate.core.internal.MCEclipseApplication;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;

/**
 * Abstract base action for toggling the display of Microclimate logs.
 */
public abstract class ToggleConsoleAction implements IObjectActionDelegate {

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
            	if (app.isEnabled() && supportsConsole()) {
	            	action.setChecked(hasConsole());
	            	action.setEnabled(true);
	            	return;
            	}
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
        	IConsole console = createConsole();
        	ConsolePlugin.getDefault().getConsoleManager().showConsoleView(console);
        	setConsole(console);
        } else {
        	IConsole console = getConsole();
        	if (console != null) {
	        	IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
	        	consoleManager.removeConsoles(new IConsole[] { console });
	        	setConsole(null);
        	}
        }
    }

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		// nothing
	}
	
	protected abstract boolean supportsConsole();
	protected abstract IConsole createConsole();
	protected abstract void setConsole(IConsole console);
	protected abstract boolean hasConsole();
	protected abstract IConsole getConsole();
}
