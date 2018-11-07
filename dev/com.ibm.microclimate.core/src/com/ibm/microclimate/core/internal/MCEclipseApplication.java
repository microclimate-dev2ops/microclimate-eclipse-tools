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
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
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
	
	// in seconds
	public static final int DEFAULT_DEBUG_CONNECT_TIMEOUT = 3;
	
	// Consoles, null if not showing
	private Set<? extends IConsole> consoles = null;
	
	// Debug launch, null if not debugging
	private ILaunch launch = null;

	MCEclipseApplication(MicroclimateConnection mcConnection,
			String id, String name, ProjectType projectType, String pathInWorkspace, String contextRoot)
					throws MalformedURLException {
		super(mcConnection, id, name, projectType, pathInWorkspace, contextRoot);
	}
	
	public synchronized boolean hasConsoles() {
		return consoles != null;
	}
	
	public synchronized void setConsoles(Set<? extends IConsole> consoles) {
		this.consoles = consoles;
	}
	
	public synchronized Set<? extends IConsole> getConsoles() {
		return consoles;
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
		Job job = new Job("Reconnect debugger") {
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
    	if (consoles != null) {
	    	IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
	    	consoleManager.removeConsoles(consoles.toArray(new IConsole[consoles.size()]));
    	}
		super.dispose();
	}
	
}
