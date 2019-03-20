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

import java.net.URI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ibm.microclimate.core.MicroclimateCorePlugin;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.remote.Syncthing;

public class ICPSyncManager {
	
	private static final String SYNCED_PROJECTS_PREFSKEY = "microclimate-synced-projects";
	
	private static final String CONNECTIONS_KEY = "connections";
	private static final String URI_KEY = "uri";
	private static final String PROJECTS_KEY = "projects";
	
	
	public String setupLocalProject(MicroclimateApplication app) throws Exception {
		Syncthing syncthing = getSyncthing();
		
		// Set up synchronization for the application
		String host = app.mcConnection.getHost();
		String namespace = app.mcConnection.getSocketNamespace();
		String localFolder = syncthing.shareICPFolder(host, namespace, app.name);
		addSyncedProject(app);
		return localFolder;
	}
	
	public void removeLocalProject(MicroclimateApplication app) throws Exception {
		removeSyncedProject(app);
		Syncthing syncthing = getSyncthing();
		syncthing.stopSharingFolder(app.name);
	}
	
	public void removeProjectsForConnection(MicroclimateConnection conn) throws Exception {
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
	
	public void initSynchronization() throws Exception {
		Syncthing syncthing = getSyncthing();
		
		// Read in the synced projects from the preferences and set up synchronization
		JSONObject pref = getSyncedProjectsValue();
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
							String host = app.mcConnection.getHost();
							String namespace = app.mcConnection.getSocketNamespace();
							syncthing.shareICPFolderNoWait(host, namespace, app.name);
						} else {
							MCLogger.logError("Could not find the " + projects.getString(projIndex) + " microclimate application for the connection: " + uri);
						}
					}
				} else {
					MCLogger.logError("Could not find the microclimate connection for URI: " + uri);
				}
			}
		}
	}
	
	private Syncthing getSyncthing() throws Exception {
		Syncthing syncthing = MicroclimateCorePlugin.getInstance().getSyncthing();
		if (!syncthing.isRunning()) {
			syncthing.start();
		}
		return syncthing;
	}
	
	private JSONObject getSyncedProjectsValue() throws JSONException {
		String strValue = MicroclimateCorePlugin.getDefault()
				.getPreferenceStore()
				.getString(SYNCED_PROJECTS_PREFSKEY);
		if (strValue == null || strValue.isEmpty()) {
			return null;
		}
		return new JSONObject(strValue);
	}
	
	private void addSyncedProject(MicroclimateApplication app) throws JSONException {
		URI uri = app.mcConnection.baseUrl;
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
		
		MicroclimateCorePlugin.getDefault().getPreferenceStore().setValue(SYNCED_PROJECTS_PREFSKEY, pref.toString());
	}
	
	private void removeSyncedProject(MicroclimateApplication app) throws JSONException {
		URI uri = app.mcConnection.baseUrl;
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
		}
	}
	
	private JSONArray removeConnection(MicroclimateConnection conn) throws JSONException {
		URI uri = conn.baseUrl;
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
		}
		
		return projects;
	}

}
