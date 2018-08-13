package com.ibm.microclimate.core.internal.server;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;

import com.ibm.microclimate.core.MCLogger;

public class MicroclimateServerLaunchConfigDelegate extends AbstractJavaLaunchConfigurationDelegate {

	@Override
	public void launch(ILaunchConfiguration config, String launchMode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {

		MCLogger.log("Launching!!!!!! mode=" + launchMode);

        final IServer server = ServerUtil.getServer(config);
        if (server == null) {
            MCLogger.logError("Could not find server from configuration " + config.getName());
            return;
        }

        final MicroclimateServerBehaviour serverBehaviour =
        		(MicroclimateServerBehaviour) server.loadAdapter(MicroclimateServerBehaviour.class, null);

        serverBehaviour.doRestart(config, launchMode, launch, monitor);
	}

}
