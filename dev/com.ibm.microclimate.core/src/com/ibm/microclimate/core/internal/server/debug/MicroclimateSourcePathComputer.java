/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core.internal.server.debug;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;
import org.eclipse.debug.core.sourcelookup.containers.ProjectSourceContainer;

import com.ibm.microclimate.core.internal.server.MicroclimateServer;

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

		// MicroclimateServerLaunchConfigDelegate sets this attribute in the launch config.
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
