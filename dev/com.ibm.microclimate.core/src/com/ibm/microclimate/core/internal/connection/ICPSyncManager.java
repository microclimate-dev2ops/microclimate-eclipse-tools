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

package com.ibm.microclimate.core.internal.connection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ibm.microclimate.core.MicroclimateCorePlugin;
import com.ibm.microclimate.core.internal.FileUtil;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.messages.Messages;
import com.ibm.microclimate.core.internal.remote.Syncthing;
import com.ibm.microclimate.core.internal.remote.SyncthingEvent;
import com.ibm.microclimate.core.internal.remote.SyncthingEventListener;
import com.ibm.microclimate.core.internal.remote.SyncthingEventMonitor;

public class ICPSyncManager {
	
	private static final String SYNCED_PROJECTS_PREFSKEY = "microclimate-synced-projects";
	
	private static final String CONNECTIONS_KEY = "connections";
	private static final String URI_KEY = "uri";
	private static final String PROJECTS_KEY = "projects";
	
	
	public static String setupLocalProject(ICPMicroclimateConnection conn, MicroclimateApplication app, IProgressMonitor monitor) throws Exception {
		Syncthing syncthing = getSyncthing();
		
		// Get notified when the folder setup is complete
		boolean[] upToDate = new boolean[1];
		SyncthingEventListener folderCompletionListener = new SyncthingEventListener() {
			@Override
			public void eventNotify(SyncthingEvent event) {
				if (app.name.equals(event.data.get(SyncthingEventMonitor.FOLDER_KEY))) {
					Object completion = event.data.get(SyncthingEventMonitor.COMPLETION_KEY);
					if (completion != null && completion instanceof Integer && ((Integer)completion).intValue() == 100) {
						upToDate[0] = true;
					}
				}
			}
		};
		syncthing.addEventListener(folderCompletionListener, SyncthingEventMonitor.FOLDER_COMPLETION_TYPE);
		
		try {
			// Set up synchronization for the application
			String localFolder = syncthing.shareICPFolder(conn.getMasterIP(), conn.getNamespace(), app.name);
			while (!upToDate[0] && !monitor.isCanceled()) {
				// Wait for the project to be up to date, checking the progress monitor to see if canceled
				try {
					Thread.sleep(500);
				} catch (Exception e) {
					// Ignore
				}
			}
			if (monitor.isCanceled()) {
				String location = syncthing.stopSharingFolder(app.name);
				if (location != null) {
					FileUtil.deleteDirectory(location, true);
				}
				return null;
			}
			addSyncedProject(app);
			return localFolder;
		} finally {
			syncthing.removeEventListener(folderCompletionListener, SyncthingEventMonitor.FOLDER_COMPLETION_TYPE);
		}
	}
	
	public static void removeLocalProject(MicroclimateApplication app) throws Exception {
		removeSyncedProject(app);
		Syncthing syncthing = getSyncthing();
		syncthing.stopSharingFolder(app.name);
	}
	
	public static void removeProjectsForConnection(MicroclimateConnection conn) throws Exception {
		JSONArray projects = removeConnection(conn);
		if (projects == null || projects.length() == 0) {
			return;
		}
		
		Syncthing syncthing = getSyncthing();
		for (int projIndex = 0; projIndex < projects.length(); projIndex++) {
			syncthing.stopSharingFolder(projects.getString(projIndex));
		}
		syncthing.removeICPDevice(conn.getHost(), conn.getSocketNamespace());
	}
	
	public static void syncProject(MicroclimateApplication app) throws Exception {
		Syncthing syncthing = getSyncthing();
		syncthing.scanFolder(app.name);
	}
	
	public static void initSynchronization() throws Exception {
		// Read in the synced projects from the preferences and set up synchronization
		Job job = new Job(Messages.ICPSyncInitializationJobLabel) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					JSONObject pref = getSyncedProjectsValue();
					Map<MicroclimateConnection, List<MicroclimateApplication>> syncMap = new HashMap<MicroclimateConnection, List<MicroclimateApplication>>();
					if (pref != null) {
						JSONArray connections = pref.getJSONArray(CONNECTIONS_KEY);
						for (int connIndex = 0; connIndex < connections.length(); connIndex++) {
							JSONObject connection = connections.getJSONObject(connIndex);
							String uri = connection.getString(URI_KEY);
							MicroclimateConnection mcConnection = MicroclimateConnectionManager.getActiveConnection(uri);
							if (mcConnection != null) {
								JSONArray projects = connection.getJSONArray(PROJECTS_KEY);
								for (int projIndex = 0; projIndex < projects.length(); projIndex++) {
									MicroclimateApplication app = mcConnection.getAppByName(projects.getString(projIndex));
									if (app != null) {
										List<MicroclimateApplication> appList = syncMap.get(mcConnection);
										if (appList == null) {
											appList = new ArrayList<MicroclimateApplication>();
											syncMap.put(mcConnection, appList);
										}
										appList.add(app);
									} else {
										MCLogger.logError("Could not find the " + projects.getString(projIndex) + " microclimate application for the connection: " + uri);
									}
								}
							} else {
								MCLogger.logError("Could not find the microclimate connection for URI: " + uri);
							}
						}
					}
					
					// Only start up syncthing if there is something to be synchronized
					if (!syncMap.isEmpty()) {
						Syncthing syncthing = getSyncthing();
						for (Map.Entry<MicroclimateConnection, List<MicroclimateApplication>> entry : syncMap.entrySet()) {
							MicroclimateConnection conn = entry.getKey();
							for (MicroclimateApplication app : entry.getValue()) {
								syncthing.shareICPFolder(conn.getHost(), conn.getSocketNamespace(), app.name);
							}
						}
					}
					return Status.OK_STATUS;
				} catch (Exception e) {
					MCLogger.logError("An error occurred while trying to initialize ICP synchronization", e); //$NON-NLS-1$
					return new Status(IStatus.ERROR, MicroclimateCorePlugin.PLUGIN_ID, Messages.ICPSyncInitializationError, e);
				}
			}
		};
		job.setPriority(Job.LONG);
		job.schedule();
	}
	
	public static boolean isProjectSynced(MicroclimateApplication app) throws JSONException {
		JSONObject pref = getSyncedProjectsValue();
		if (pref == null) {
			return false;
		}
		JSONArray connections = pref.getJSONArray(CONNECTIONS_KEY);
		for (int connIndex = 0; connIndex < connections.length(); connIndex++) {
			JSONObject connection = connections.getJSONObject(connIndex);
			if (app.mcConnection.baseUrl.toString().equals(connection.get(URI_KEY))) {
				JSONArray projects = connection.getJSONArray(PROJECTS_KEY);
				for (int projIndex = 0; projIndex < projects.length(); projIndex++) {
					if (app.name.equals(projects.getString(projIndex))) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	private static Syncthing getSyncthing() throws Exception {
		Syncthing syncthing = MicroclimateCorePlugin.getInstance().getSyncthing();
		if (!syncthing.isRunning()) {
			syncthing.start();
		}
		return syncthing;
	}
	
	private static JSONObject getSyncedProjectsValue() throws JSONException {
		String strValue = MicroclimateCorePlugin.getDefault()
				.getPreferenceStore()
				.getString(SYNCED_PROJECTS_PREFSKEY);
		if (strValue == null || strValue.isEmpty()) {
			return null;
		}
		return new JSONObject(strValue);
	}
	
	private static void setSyncedProjectsValue(JSONObject pref) {
		MicroclimateCorePlugin.getDefault().getPreferenceStore().setValue(SYNCED_PROJECTS_PREFSKEY, pref.toString());
	}
	
	private static void addSyncedProject(MicroclimateApplication app) throws JSONException {
		String uri = app.mcConnection.baseUrl.toString();
		String projectName = app.name;
		
		JSONObject pref = getSyncedProjectsValue();
		JSONObject connection = null;
		if (pref == null) {
			pref = new JSONObject();
		} else {
			JSONArray connections = pref.getJSONArray(CONNECTIONS_KEY);
			for (int connIndex = 0; connIndex < connections.length(); connIndex++) {
				connection = connections.getJSONObject(connIndex);
				if (uri.equals(connection.get(URI_KEY))) {
					JSONArray projects = connection.getJSONArray(PROJECTS_KEY);
					for (int projIndex = 0; projIndex < projects.length(); projIndex++) {
						if (projectName.equals(projects.getString(projIndex))) {
							return;
						}
					}
				}
			}
		}
		
		if (connection == null) {
			connection = new JSONObject();
			connection.put(URI_KEY, uri);
			pref.append(CONNECTIONS_KEY, connection);
		}
		connection.append(PROJECTS_KEY, projectName);
		setSyncedProjectsValue(pref);
	}
	
	private static void removeSyncedProject(MicroclimateApplication app) throws JSONException {
		String uri = app.mcConnection.baseUrl.toString();
		String projectName = app.name;
		
		JSONObject pref = getSyncedProjectsValue();
		if (pref != null) {
			JSONArray connections = pref.getJSONArray(CONNECTIONS_KEY);
			for (int connIndex = 0; connIndex < connections.length(); connIndex++) {
				JSONObject connection = connections.getJSONObject(connIndex);
				if (uri.equals(connection.get(URI_KEY))) {
					JSONArray projects = connection.getJSONArray(PROJECTS_KEY);
					JSONArray newProjects = new JSONArray();
					for (int projIndex = 0; projIndex < projects.length(); projIndex++) {
						if (!projectName.equals(projects.getString(projIndex))) {
							newProjects.put(projects.getString(projIndex));
						}
					}
					connection.put(PROJECTS_KEY, newProjects);
				}
			}
			setSyncedProjectsValue(pref);
		}
	}
	
	private static JSONArray removeConnection(MicroclimateConnection conn) throws JSONException {
		String uri = conn.baseUrl.toString();
		JSONArray projects = null;
		
		JSONObject pref = getSyncedProjectsValue();
		if (pref != null) {
			JSONArray connections = pref.getJSONArray(CONNECTIONS_KEY);
			JSONArray newConnections = new JSONArray();
			for (int connIndex = 0; connIndex < connections.length(); connIndex++) {
				JSONObject connection = connections.getJSONObject(connIndex);
				if (!uri.equals(connection.get(URI_KEY))) {
					newConnections.put(connection);
				} else {
					projects = connection.getJSONArray(PROJECTS_KEY);
				}
			}
			pref.put(CONNECTIONS_KEY, newConnections);
			setSyncedProjectsValue(pref);
		}
		
		return projects;
	}

}
