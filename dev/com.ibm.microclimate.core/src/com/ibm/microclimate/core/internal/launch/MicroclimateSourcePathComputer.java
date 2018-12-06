/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.core.internal.launch;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;
import org.eclipse.debug.core.sourcelookup.containers.ProjectSourceContainer;

import com.ibm.microclimate.core.internal.MCLogger;

/**
 * This class is used by the Microclimate Launch Config to add the Java project of interest
 * to the Debug Source path.
 */
public class MicroclimateSourcePathComputer implements ISourcePathComputerDelegate {

	@Override
	public ISourceContainer[] computeSourceContainers(ILaunchConfiguration config, IProgressMonitor monitor)
			throws CoreException {

		// Get the project name from the launch configuration, look up the IProject, and return that IProject as a
		// source container.

		// MicroclimateLaunchConfigDelegate sets this attribute in the launch config.
		final String projectName = config.getAttribute(MicroclimateLaunchConfigDelegate.PROJECT_NAME_ATTR, "");
		if (!projectName.isEmpty()) {
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);

			if (project == null || !project.exists()) {
				MCLogger.logError("Could not find project with name " + projectName + " to add to source path");
				return new ISourceContainer[0];
			}

			// What does the second boolean parameter 'referenced' mean ?
			ISourceContainer projectSourceContainer = new ProjectSourceContainer(project, true);
			MCLogger.log("Adding source container from project " + project.getName());
			return new ISourceContainer[] { projectSourceContainer };
		}

		MCLogger.logError("Could not retrieve project name from launch config " + config.getName());
		return new ISourceContainer[0];
	}

}
