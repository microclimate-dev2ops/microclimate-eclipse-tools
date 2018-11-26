/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;

import com.ibm.microclimate.core.internal.MCEclipseApplication;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.constants.AppState;
import com.ibm.microclimate.core.internal.constants.StartMode;
import com.ibm.microclimate.ui.MicroclimateUIPlugin;
import com.ibm.microclimate.ui.internal.messages.Messages;

/**
 * Action to restart a Microclimate application in debug mode.
 */
public class RestartDebugModeAction implements IObjectActionDelegate, IViewActionDelegate, IActionDelegate2 {

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
            if (obj instanceof MCEclipseApplication) {
            	app = (MCEclipseApplication)obj;
            	if (app.isEnabled() && app.supportsDebug()) {
		            action.setEnabled(app.getAppState() == AppState.STARTED || app.getAppState() == AppState.STARTING);
	            	return;
            	}
            }
        }
        
        action.setEnabled(false);
    }

    @Override
    public void run(IAction action) {
        if (app == null) {
        	// should not be possible
        	MCLogger.logError("RestartDebugModeAction ran but no Microclimate application was selected");
			return;
		}
        
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(app.name);
        // Check if the project has been imported into Eclipse. If not, offer to import it.
        if (project == null || !project.exists()) {
        	int result = openDialog(NLS.bind(Messages.ProjectNotImportedDialogTitle, app.name), NLS.bind(Messages.ProjectNotImportedDialogMsg, app.name));
        	if (result == 0) {
        		// Import the project
        		ImportProjectAction.importProject(app);
        	} else if (result == 2) {
        		// Cancel selected
        		return;
        	}
        // Check if the project is open in Eclipse. If not, offer to open it.
        } else if (!project.isOpen()) {
        	int result = openDialog(NLS.bind(Messages.ProjectClosedDialogTitle, app.name), NLS.bind(Messages.ProjectClosedDialogMsg, app.name));
        	if (result == 0) {
        		// Open the project
        		Job job = new Job(NLS.bind(Messages.ProjectOpenJob, app.name)) {
        			@Override
        			protected IStatus run(IProgressMonitor monitor) {
        				try {
        					project.open(monitor);
        					return Status.OK_STATUS;
        				} catch (CoreException e) {
        					return new Status(IStatus.ERROR, MicroclimateUIPlugin.PLUGIN_ID,
        							NLS.bind(Messages.ProjectOpenError, app.name), e);
        				}
        			}
        		};
        		job.setPriority(Job.LONG);
        		job.schedule();
        	} else if (result == 2) {
        		// Cancel selected
        		return;
        	}
        }

        try {
        	// Clear out any old launch
        	ILaunch launch = app.getLaunch();
        	if (launch != null) {
        		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        		launchManager.removeLaunch(launch);
        	}
        	app.setLaunch(null);
        	
        	// Restart the project in debug mode. The debugger will be attached when the restart result
        	// event is received from Microclimate.
			app.mcConnection.requestProjectRestart(app, StartMode.DEBUG.startMode);
			app.setStartMode(StartMode.DEBUG);
		} catch (Exception e) {
			MCLogger.logError("Error initiating restart for project: " + app.name, e); //$NON-NLS-1$
			MCUtil.openDialog(true, Messages.ErrorOnRestartDialogTitle, e.getMessage());
			return;
		}
    }
    
    /*
     * Dialog which asks the user a question and they can select Yes, No
     * or Cancel.
     * Returns:
     *  0 - user selected Yes
     *  1 - user selected No
     *  2 - user selected Cancel
     */
    private static int openDialog(String title, String msg) {
    	final int[] result = new int[1];
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				Shell shell = Display.getDefault().getActiveShell();
				String[] buttonLabels = new String[] {Messages.DialogYesButton, Messages.DialogNoButton, Messages.DialogCancelButton};
				MessageDialog dialog = new MessageDialog(shell, title, MicroclimateUIPlugin.getImage(MicroclimateUIPlugin.MICROCLIMATE_ICON),
						msg, MessageDialog.QUESTION, buttonLabels, 0);
				result[0] = dialog.open();
			}
		});
		
		return result[0];
	}
    
	@Override
	public void runWithEvent(IAction action, Event event) {
		run(action);
	}

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		// nothing
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
	public void init(IViewPart arg0) {
		// TODO Auto-generated method stub
		
	}
}
