/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.test.util;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnectionManager;
import com.ibm.microclimate.core.internal.constants.AppState;

public class MicroclimateUtil {
	
	public static boolean waitForProject(MicroclimateConnection connection, String projectName, long timeout, long interval ) {
        // Wait for the project to be created
		TestUtil.wait(new Condition() {
			@Override
			public boolean test() {
				connection.refreshApps(null);
				return connection.getAppByName(projectName) != null;
			}
		}, timeout, interval);
		return connection.getAppByName(projectName) != null;
	}
	
	public static boolean waitForProjectStart(MicroclimateConnection connection, String projectName, long timeout, long interval) {
		// Wait for the project to be started
		TestUtil.wait(new Condition() {
			@Override
			public boolean test() {
				return connection.getAppByName(projectName).isRunning();
			}
		}, timeout, interval);
        return connection.getAppByName(projectName).isRunning();
	}
	
	public static void cleanup(MicroclimateConnection connection) throws Exception {
		// Delete eclipse projects
		List<MicroclimateApplication> allApps = connection.getApps();
		for (MicroclimateApplication app: allApps) {
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(app.name);
			if (project != null && project.exists()) {
				try {
	                project.delete(IResource.FORCE | IResource.NEVER_DELETE_PROJECT_CONTENT, null);
	            } catch (Exception e) {
	                TestUtil.print("Failed to clean up project: " + project.getName(), e);
	            }
			}
		}
		
		// Delete the projects from Microclimate
		List<MicroclimateApplication> apps = connection.getApps();
		for (MicroclimateApplication app: apps) {
			connection.requestProjectDelete(app.projectID);
			try {
				Thread.sleep(2000);
			} catch (Exception e) {
				// Ignore
			}
		}
		
		MicroclimateConnectionManager.removeConnection(connection.baseUrl.toString());
	}
	
	public static boolean waitForAppState(MicroclimateApplication app, AppState state, long timeout, long interval) {
		TestUtil.wait(new Condition() {
			@Override
			public boolean test() {
				return app.getAppState() == state;
			}
		}, timeout, interval);
		return app.getAppState() == state;
	}

}
