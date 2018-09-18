package com.ibm.microclimate.test.util;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnectionManager;

public class MicroclimateUtil {
	
	public static boolean waitForProject(MicroclimateConnection connection, String projectName, long timeout, long interval ) {
        // Wait for the project to be created
		TestUtil.wait(new Condition() {
			@Override
			public boolean test() {
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
		// Delete servers for linked projects
		List<MicroclimateApplication> linkedApps = connection.getLinkedApps();
		for (MicroclimateApplication app: linkedApps) {
			ServerBehaviourDelegate serverBehaviour = app.getLinkedServer();
			serverBehaviour.getServer().delete();
		}
		
		// Delete eclipse projects
		List<MicroclimateApplication> allApps = connection.getApps();
		for (MicroclimateApplication app: allApps) {
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(app.name);
			if (project != null && project.exists()) {
				try {
	                project.delete(IResource.FORCE | IResource. NEVER_DELETE_PROJECT_CONTENT, null);
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

}
