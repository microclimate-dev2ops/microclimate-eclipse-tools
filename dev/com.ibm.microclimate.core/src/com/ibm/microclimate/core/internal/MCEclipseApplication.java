/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core.internal;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;

import com.ibm.microclimate.core.MicroclimateCorePlugin;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.constants.ProjectType;
import com.ibm.microclimate.core.internal.launch.MicroclimateLaunchConfigDelegate;
import com.ibm.microclimate.core.internal.messages.Messages;

public class MCEclipseApplication extends MicroclimateApplication {
	
	// Validation marker
	public static final String MARKER_TYPE = MicroclimateCorePlugin.PLUGIN_ID + ".validationMarker";
	public static final String CONNECTION_URL = "connectionUrl";
	public static final String PROJECT_ID = "projectId";
	public static final String QUICK_FIX_ID = "quickFixId";
	public static final String QUICK_FIX_DESCRIPTION = "quickFixDescription";
	
	// in seconds
	public static final int DEFAULT_DEBUG_CONNECT_TIMEOUT = 3;
	
	// Consoles, null if not showing
	private IConsole appConsole = null;
	private IConsole buildConsole = null;
	
	// Debug launch, null if not debugging
	private ILaunch launch = null;

	MCEclipseApplication(MicroclimateConnection mcConnection,
			String id, String name, ProjectType projectType, String pathInWorkspace, String containerId, String contextRoot)
					throws MalformedURLException {
		super(mcConnection, id, name, projectType, pathInWorkspace, containerId, contextRoot);
	}
	
	public synchronized boolean hasAppConsole() {
		return appConsole != null;
	}
	
	public synchronized boolean hasBuildConsole() {
		return buildConsole != null;
	}
	
	public synchronized void setAppConsole(IConsole console) {
		this.appConsole = console;
	}
	
	public synchronized void setBuildConsole(IConsole console) {
		this.buildConsole = console;
	}
	
	public synchronized IConsole getAppConsole() {
		return appConsole;
	}
	
	public synchronized IConsole getBuildConsole() {
		return buildConsole;
	}
	
	public synchronized void setLaunch(ILaunch launch) {
		this.launch = launch;
	}
	
	public synchronized ILaunch getLaunch() {
		return launch;
	}

	@Override
	public void connectDebugger() {
		final MCEclipseApplication app = this;
		Job job = new Job(Messages.ReconnectDebugJob) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
			        ILaunchConfigurationType launchConfigurationType = launchManager.getLaunchConfigurationType(MicroclimateLaunchConfigDelegate.LAUNCH_CONFIG_ID);
			        ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance((IContainer) null, app.name);
			        MicroclimateLaunchConfigDelegate.setConfigAttributes(workingCopy, app);
			        ILaunchConfiguration launchConfig = workingCopy.doSave();
		            ILaunch launch = launchConfig.launch(ILaunchManager.DEBUG_MODE, monitor);
		            app.setLaunch(launch);
		            return Status.OK_STATUS;
				} catch (Exception e) {
					MCLogger.logError("An error occurred while trying to launch the debugger for project: " + app.name);
					return new Status(IStatus.ERROR, MicroclimateCorePlugin.PLUGIN_ID,
							NLS.bind(Messages.DebugLaunchError, app.name), e);
				}
			}
		};
		job.setPriority(Job.LONG);
		job.schedule();
	}

	@Override
	public void reconnectDebugger() {
		// First check if there is a launch and it is registered
		if (launch != null) {
			ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
			for (ILaunch launchItem : launchManager.getLaunches()) {
				if (launch.equals(launchItem)) {
					// Check if the debugger is still attached (for Liberty, a small change to the app does not require a server restart)
					IDebugTarget debugTarget = launch.getDebugTarget();
					if (debugTarget == null || debugTarget.isDisconnected()) {
						// Reconnect the debugger
						launchManager.removeLaunch(launch);
						launch = null;
						connectDebugger();
					}
				}
			}
		}
	}

	@Override
	public void dispose() {
		if (launch != null) {
			ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
			launchManager.removeLaunch(launch);
		}
		List<IConsole> consoles = new ArrayList<IConsole>();
		if (appConsole != null) {
			consoles.add(appConsole);
		}
		if (buildConsole != null) {
			consoles.add(buildConsole);
		}
		if (!consoles.isEmpty()) {
			IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
			consoleManager.removeConsoles(consoles.toArray(new IConsole[consoles.size()]));
		}
		super.dispose();
	}
	
	@Override
	public void resetValidation() {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		try {
			project.deleteMarkers(MARKER_TYPE, true, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {
			MCLogger.logError("Failed to delete existing markers for the " + name + " project.", e);
		}
	}
	
	@Override
	public void validationError(String filePath, String message, String quickFixId, String quickFixDescription) {
		validationEvent(IMarker.SEVERITY_ERROR, filePath, message, quickFixId, quickFixDescription);
	}
	
	@Override
	public void validationWarning(String filePath, String message, String quickFixId, String quickFixDescription) {
		validationEvent(IMarker.SEVERITY_WARNING, filePath, message, quickFixId, quickFixDescription);
	}
	
    private void validationEvent(int severity, String filePath, String message, String quickFixId, String quickFixDescription) {
        try {
        	IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
        	IResource resource = project;
        	if (filePath != null && !filePath.isEmpty()) {
	        	IPath path = new Path(filePath);
	        	if (filePath.startsWith(project.getName())) {
	        		path = path.removeFirstSegments(1);
	        	}
	        	IFile file = project.getFile(path);
	        	if (file != null && file.exists()) {
	        		resource = file;
	        	}
        	}
            final IMarker marker = resource.createMarker(MARKER_TYPE);
            marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
            marker.setAttribute(IMarker.LINE_NUMBER, 1);
            marker.setAttribute(IMarker.MESSAGE, message);
            if (quickFixId != null && !quickFixId.isEmpty()) {
            	marker.setAttribute(CONNECTION_URL, mcConnection.baseUrl.toString());
            	marker.setAttribute(PROJECT_ID, projectID);
            	marker.setAttribute(QUICK_FIX_ID, quickFixId);
            	marker.setAttribute(QUICK_FIX_DESCRIPTION, quickFixDescription);
            }
        } catch (CoreException e) {
            MCLogger.logError("Failed to create a marker for the " + name + " application: " + message, e);
        }
    }
    
}
