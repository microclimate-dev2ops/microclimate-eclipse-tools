/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.core.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

import com.ibm.microclimate.core.MicroclimateCorePlugin;
import com.ibm.microclimate.core.internal.connection.ICPSyncManager;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection.ConnectionType;
import com.ibm.microclimate.core.internal.constants.MCConstants;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnectionManager;
import com.ibm.microclimate.core.internal.messages.Messages;

public class MCResourceChangeListener implements IResourceChangeListener {

    private static MCResourceChangeListener resourceChangeListener;

    public synchronized static void start() {
        if (resourceChangeListener != null)
            return;
        resourceChangeListener = new MCResourceChangeListener();
        ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener, IResourceChangeEvent.POST_CHANGE);
    }

    public synchronized static void stop() {
        if (resourceChangeListener == null)
            return;
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        if (workspace != null)
            workspace.removeResourceChangeListener(resourceChangeListener);
        resourceChangeListener = null;
    }
		
	@Override
	public void resourceChanged(IResourceChangeEvent event) {
        IResourceDelta delta = event.getDelta();
        if (delta == null)
            return;

        // ignore clean builds
        if (event.getBuildKind() == IncrementalProjectBuilder.CLEAN_BUILD)
            return;
        
        try {
        	final Set<MicroclimateApplication> changedApps = new HashSet<MicroclimateApplication>();
        	final Set<MicroclimateApplication> toBeRemovedApps = new HashSet<MicroclimateApplication>();
        	
        	delta.accept(new IResourceDeltaVisitor() {

				@Override
				public boolean visit(IResourceDelta visitorDelta) throws CoreException {
					
					IResource resource = visitorDelta.getResource();
					int kind = visitorDelta.getKind();
					
					if (resource != null && resource instanceof IProject && kind == IResourceDelta.REMOVED) {
						MicroclimateApplication app = getApplication(resource);
						if (app != null && app.mcConnection.getType() == ConnectionType.ICP_CONNECTION) {
							toBeRemovedApps.add(app);
						}
					} else if (resource != null && resource instanceof IFile) {
						MicroclimateApplication app = getApplication(resource);
						if (app != null) {
							changedApps.add(app);
						}
					} else if (resource != null && resource instanceof IFolder) {
						if (kind == IResourceDelta.ADDED || kind == IResourceDelta.REMOVED) {
							MicroclimateApplication app = getApplication(resource);
							if (app != null) {
								changedApps.add(app);
							}
						}
					}
					return true;
				}
        	});
        	
			for (MicroclimateApplication app : toBeRemovedApps) {
				MCLogger.log("Application " + app.name + " removed. Stop any synchronization.");
				Job job = new Job(NLS.bind(Messages.ICPStopSyncJob, app.name)) {

					@Override
					protected IStatus run(IProgressMonitor arg0) {
						// Call method to remove - does nothing if not synced
						try {
							ICPSyncManager.removeLocalProject(app);
							return Status.OK_STATUS;
						} catch (Exception e) {
							MCLogger.logError("Failed to stop synchronization for microclimate ICP project: " + app.name, e);
							return new Status(IStatus.ERROR, MicroclimateCorePlugin.PLUGIN_ID, NLS.bind(Messages.ICPStopSyncFailed, app.name));
						}
					}
				};
				job.schedule();
			}
			
			for (MicroclimateApplication app : changedApps) {
				MCLogger.log("Application " + app.name + " changed.");
				String msg = app.mcConnection.getType() == ConnectionType.ICP_CONNECTION ? Messages.ICPSyncJob : Messages.MicroclimateBuildJob;
				Job job = new Job(NLS.bind(msg, app.name)) {

					@Override
					protected IStatus run(IProgressMonitor arg0) {
						// Call method to remove - does nothing if not synced
						switch (app.mcConnection.getType()) {
						case ICP_CONNECTION:
							try {
								ICPSyncManager.syncProject(app);
								return Status.OK_STATUS;
							} catch (Exception e) {
								MCLogger.logError("Failed to synchronize local changes to ICP for project: " + app.name, e);
								return new Status(IStatus.ERROR, MicroclimateCorePlugin.PLUGIN_ID, NLS.bind(Messages.ICPSyncFailed, app.name));
							}
						case LOCAL_CONNECTION:
							try {
								app.mcConnection.requestProjectBuild(app, MCConstants.VALUE_ACTION_BUILD);
								return Status.OK_STATUS;
							} catch (Exception e) {
								MCLogger.logError("Failed to start a build for microclimate project: " + app.name, e);
								return new Status(IStatus.ERROR, MicroclimateCorePlugin.PLUGIN_ID, NLS.bind(Messages.MicroclimateBuildError, app.name));
							}
						default:
							MCLogger.logError("Unsupported connection type found by resource change listener: " + app.mcConnection.getType());
							return Status.OK_STATUS;
						}
					}
				};
				job.schedule();
			}
			
        } catch (CoreException e) {
        	MCLogger.log("Exception processing resource changed even: " + e);
        }
	}
	
	private MicroclimateApplication getApplication(IResource resource) {
		IProject project = resource.getProject();
		if (project != null) {
			List<MicroclimateConnection> connections = MicroclimateConnectionManager.activeConnections();
			for (MicroclimateConnection connection : connections) {
				MicroclimateApplication app = connection.getAppByName(project.getName());
				if (app != null) {
					return app;
				}
			}
		}
		return null;
	}
	
}
