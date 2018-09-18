/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core.internal.server.debug;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.Messages;
import com.ibm.microclimate.core.internal.server.MicroclimateServer;
import com.ibm.microclimate.core.internal.server.MicroclimateServerBehaviour;

/**
 *
 * @author timetchells@ibm.com
 *
 */
public class MicroclimateServerLaunchConfigDelegate extends AbstractJavaLaunchConfigurationDelegate {

	@Override
	public void launch(ILaunchConfiguration config, String launchMode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {

		try {
			launchInner(config, launchMode, launch, monitor);
		}
		catch(CoreException e) {
			monitor.setCanceled(true);
			getLaunchManager().removeLaunch(launch);
			throw e;
		}
	}

	private void launchInner(ILaunchConfiguration config, String launchMode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {

        final IServer server = ServerUtil.getServer(config);
        if (server == null) {
        	String msg = "Could not find server from configuration " + config.getName();		// $NON-NLS-1$
            MCLogger.logError(msg);
            abort(msg, null, IStatus.ERROR);
        }
        else if (server.getServerState() == IServer.STATE_STARTING) {
        	MCLogger.logError("Trying to start server that is Starting");		// $NON-NLS-1$

        	// There are likely other scenarios where we could present the 'only launch from servers view' message,
        	// but I'm not sure how to detect them at this point.
        	abort(NLS.bind(Messages.MicroclimateServerLaunchConfigDelegate_OnlyLaunchFromServers, server.getName()),
        			null, IStatus.ERROR);
        }

        // Give the launch config the same name as the server
        ILaunchConfigurationWorkingCopy configWc = config.getWorkingCopy();
        configWc.rename(server.getName());
        config = configWc.doSave();

        final MicroclimateServerBehaviour serverBehaviour = server.getAdapter(MicroclimateServerBehaviour.class);

        // Validate here that the server can actually be restarted.
        String errorMsg = ""; //$NON-NLS-1$
        if (serverBehaviour.isErrored() && serverBehaviour.getSuffix() != null) {
        	errorMsg = serverBehaviour.getSuffix();
        }
        else if (serverBehaviour.getApp() == null) {
			errorMsg = NLS.bind(Messages.MicroclimateServerLaunchConfigDelegate_ErrInitServer, server.getName());
        }

        if (!errorMsg.isEmpty()) {
        	abort(errorMsg, null, IStatus.ERROR);
        }

        // Remove the old launch from the debug view.
        ILaunch oldLaunch = server.getLaunch();
        if (oldLaunch != null && !oldLaunch.equals(launch)) {
        	MCLogger.log("Removing old launch");
            getLaunchManager().removeLaunch(oldLaunch);
        }

		MCLogger.log("Launching " + server.getName() + " in " + launchMode + " mode"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        final String projectName = server.getAttribute(MicroclimateServer.ATTR_ECLIPSE_PROJECT_NAME, ""); //$NON-NLS-1$
        if (projectName.isEmpty()) {
        	// Need the project name to set the source path - in this case the user can work around it by
        	// adding the project to the source path manually.
        	MCLogger.logError(MicroclimateServer.ATTR_ECLIPSE_PROJECT_NAME + " was not set on server " //$NON-NLS-1$
        			+ server.getName());
        }

        configWc = config.getWorkingCopy();
        configWc.setAttribute(MicroclimateServer.ATTR_ECLIPSE_PROJECT_NAME, projectName);
        config = configWc.doSave();

        setDefaultSourceLocator(launch, config);
        serverBehaviour.setLaunch(launch);

        serverBehaviour.doLaunch(config, launchMode, launch, monitor);
        monitor.done();
	}
}
