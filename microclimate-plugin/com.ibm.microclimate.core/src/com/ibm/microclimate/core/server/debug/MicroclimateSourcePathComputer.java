package com.ibm.microclimate.core.server.debug;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;
import org.eclipse.debug.core.sourcelookup.containers.ProjectSourceContainer;

import com.ibm.microclimate.core.server.MicroclimateServer;

/**
 * This class is used by the Microclimate Launch Config to add the Java project of interest
 * to the Debug Source path.
 *
 * @author timetchells@ibm.com
 *
 */
public class MicroclimateSourcePathComputer implements ISourcePathComputerDelegate {

	@Override
	public ISourceContainer[] computeSourceContainers(ILaunchConfiguration config, IProgressMonitor monitor)
			throws CoreException {

		// Get the project name from the launch configuration, look up the IProject, and return that IProject as a
		// source container.
		final String projectName = config.getAttribute(MicroclimateServer.ATTR_ECLIPSE_PROJECT_NAME, "");
		if (!projectName.isEmpty()) {
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);

			// What does the second boolean parameter 'referenced' mean ?
			ISourceContainer projectSourceContainer = new ProjectSourceContainer(project, true);
			// MCLogger.log("Source Container from project " + project.getName());
			return new ISourceContainer[] { projectSourceContainer };
		}

		return new ISourceContainer[0];
	}

}
