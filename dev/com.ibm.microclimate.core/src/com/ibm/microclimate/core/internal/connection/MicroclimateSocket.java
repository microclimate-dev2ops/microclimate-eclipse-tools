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

package com.ibm.microclimate.core.internal.connection;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.osgi.util.NLS;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.console.OldSocketConsole;
import com.ibm.microclimate.core.internal.console.SocketConsole;
import com.ibm.microclimate.core.internal.constants.MCConstants;
import com.ibm.microclimate.core.internal.constants.ProjectType;
import com.ibm.microclimate.core.internal.constants.StartMode;
import com.ibm.microclimate.core.internal.messages.Messages;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Wrapper for a SocketIO client socket, which connects to Microclimate and listens for project state changes,
 * then updates the corresponding MicroclimateApplication's state.
 * One of these exists for each MicroclimateConnection. That connection is stored here so we can access
 * its applications.
 */
public class MicroclimateSocket {
	
	private final MicroclimateConnection mcConnection;

	public final Socket socket;

	public final URI socketUri;

	private boolean hasLostConnection = false;

	private volatile boolean hasConnected = false;

	private Set<OldSocketConsole> oldSocketConsoles = new HashSet<>();
	
	private Set<SocketConsole> socketConsoles = new HashSet<>();
	
	private Map<String, IOperationHandler> projectCreateHandlers = new HashMap<String, IOperationHandler>();

	// Track the previous Exception so we don't spam the logs with the same connection failure message
	private Exception previousException;

	// SocketIO Event names
	private static final String
			EVENT_PROJECT_CREATION = "projectCreation",				//$NON-NLS-1$
			EVENT_PROJECT_CHANGED = "projectChanged", 				//$NON-NLS-1$
			EVENT_PROJECT_STATUS_CHANGE = "projectStatusChanged", 	//$NON-NLS-1$
			EVENT_PROJECT_RESTART = "projectRestartResult", 		//$NON-NLS-1$
			EVENT_PROJECT_CLOSED = "projectClosed", 				//$NON-NLS-1$
			EVENT_PROJECT_DELETION = "projectDeletion", 			//$NON-NLS-1$
			EVENT_CONTAINER_LOGS = "container-logs",				//$NON-NLS-1$
			EVENT_PROJECT_VALIDATED = "projectValidated",			//$NON-NLS-1$
			EVENT_LOG_UPDATE = "log-update",						//$NON-NLS-1$
			EVENT_PROJECT_SETTINGS_CHANGED = "projectSettingsChanged";	//$NON-NLS-1$

	public MicroclimateSocket(MicroclimateConnection mcConnection) throws URISyntaxException {
		this.mcConnection = mcConnection;
		
		URI uri = mcConnection.baseUrl;
		if (mcConnection.getSocketNamespace() != null) {
			uri = uri.resolve(mcConnection.getSocketNamespace());
		}
		socketUri = uri;

		socket = IO.socket(socketUri);
		
		socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				MCLogger.log("SocketIO connect success @ " + socketUri); //$NON-NLS-1$

				if (!hasConnected) {
					hasConnected = true;
				}
				if (hasLostConnection) {
					mcConnection.clearConnectionError();
					previousException = null;
				}
			}
		})
		.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				if (arg0[0] instanceof Exception) {
					Exception e = (Exception) arg0[0];
					if (previousException == null || !e.getMessage().equals(previousException.getMessage())) {
						previousException = e;
						MCLogger.logError("SocketIO Connect Error @ " + socketUri, e); //$NON-NLS-1$
					}
				}
				mcConnection.onConnectionError();
				hasLostConnection = true;
			}
		})
		.on(Socket.EVENT_ERROR, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				if (arg0[0] instanceof Exception) {
					Exception e = (Exception) arg0[0];
					MCLogger.logError("SocketIO Error @ " + socketUri, e); //$NON-NLS-1$
				}
			}
		})
		.on(Socket.EVENT_MESSAGE, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				// Don't think this is ever used
				MCLogger.log("SocketIO EVENT_MESSAGE " + arg0[0].toString()); //$NON-NLS-1$
			}
		})
		.on(EVENT_PROJECT_CREATION, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				MCLogger.log(EVENT_PROJECT_CREATION + ": " + arg0[0].toString()); //$NON-NLS-1$

				try {
					JSONObject event = new JSONObject(arg0[0].toString());
					onProjectCreation(event);
				} catch (JSONException e) {
					MCLogger.logError("Error parsing JSON: " + arg0[0].toString(), e); //$NON-NLS-1$
				}
			}
		})
		.on(EVENT_PROJECT_CHANGED, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				MCLogger.log(EVENT_PROJECT_CHANGED + ": " + arg0[0].toString()); //$NON-NLS-1$

				try {
					JSONObject event = new JSONObject(arg0[0].toString());
					onProjectChanged(event);
				} catch (JSONException e) {
					MCLogger.logError("Error parsing JSON: " + arg0[0].toString(), e); //$NON-NLS-1$
				}
			}
		})
		.on(EVENT_PROJECT_SETTINGS_CHANGED, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				MCLogger.log(EVENT_PROJECT_SETTINGS_CHANGED + ": " + arg0[0].toString()); //$NON-NLS-1$

				try {
					JSONObject event = new JSONObject(arg0[0].toString());
					onProjectSettingsChanged(event);
				} catch (JSONException e) {
					MCLogger.logError("Error parsing JSON: " + arg0[0].toString(), e); //$NON-NLS-1$
				}
			}
		})
		.on(EVENT_PROJECT_STATUS_CHANGE, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				MCLogger.log(EVENT_PROJECT_STATUS_CHANGE + ": " + arg0[0].toString()); //$NON-NLS-1$

				try {
					JSONObject event = new JSONObject(arg0[0].toString());
					onProjectStatusChanged(event);
				} catch (JSONException e) {
					MCLogger.logError("Error parsing JSON: " + arg0[0].toString(), e); //$NON-NLS-1$
				}
			}
		})
		.on(EVENT_PROJECT_RESTART, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				MCLogger.log(EVENT_PROJECT_RESTART + ": " + arg0[0].toString()); //$NON-NLS-1$

				try {
					JSONObject event = new JSONObject(arg0[0].toString());
					onProjectRestart(event);
				} catch (JSONException e) {
					MCLogger.logError("Error parsing JSON: " + arg0[0].toString(), e); //$NON-NLS-1$
				}
			}
		})
		.on(EVENT_PROJECT_CLOSED, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				MCLogger.log(EVENT_PROJECT_CLOSED + ": " + arg0[0].toString()); //$NON-NLS-1$

				try {
					JSONObject event = new JSONObject(arg0[0].toString());
					onProjectClosed(event);
				} catch (JSONException e) {
					MCLogger.logError("Error parsing JSON: " + arg0[0].toString(), e); //$NON-NLS-1$
				}
			}
		})
		.on(EVENT_PROJECT_DELETION, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				MCLogger.log(EVENT_PROJECT_DELETION + ": " + arg0[0].toString()); //$NON-NLS-1$

				try {
					JSONObject event = new JSONObject(arg0[0].toString());
					onProjectDeletion(event);
				} catch (JSONException e) {
					MCLogger.logError("Error parsing JSON: " + arg0[0].toString(), e); //$NON-NLS-1$
				}
			}
		})
		.on(EVENT_CONTAINER_LOGS, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				// can't print this whole thing because the logs strings flood the output
				MCLogger.log(EVENT_CONTAINER_LOGS);

				try {
					JSONObject event = new JSONObject(arg0[0].toString());
					onContainerLogs(event);
				} catch (JSONException e) {
					MCLogger.logError("Error parsing JSON: " + arg0[0].toString(), e); //$NON-NLS-1$
				}
			}
		})
		.on(EVENT_LOG_UPDATE, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				// can't print this whole thing because the logs strings flood the output
				MCLogger.log(EVENT_LOG_UPDATE);

				try {
					JSONObject event = new JSONObject(arg0[0].toString());
					onLogUpdate(event);
				} catch (JSONException e) {
					MCLogger.logError("Error parsing JSON: " + arg0[0].toString(), e); //$NON-NLS-1$
				}
			}
		})
		.on(EVENT_PROJECT_VALIDATED, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				MCLogger.log(EVENT_PROJECT_VALIDATED + ": " + arg0[0].toString()); //$NON-NLS-1$

				try {
					JSONObject event = new JSONObject(arg0[0].toString());
					onValidationEvent(event);
				} catch (JSONException e) {
					MCLogger.logError("Error parsing JSON: " + arg0[0].toString(), e); //$NON-NLS-1$
				}
			}
		});

		socket.connect();

		MCLogger.log("Created MicroclimateSocket connected to " + socketUri); //$NON-NLS-1$
	}
	
	public void close() {
		if (socket != null) {
			if (socket.connected()) {
				socket.disconnect();
			}
			socket.close();
		}
	}
	
	private void onProjectCreation(JSONObject event) throws JSONException {
		String projectID = event.getString(MCConstants.KEY_PROJECT_ID);
		mcConnection.refreshApps(projectID);
		MicroclimateApplication app = mcConnection.getAppByID(projectID);
		if (app != null) {
			app.setEnabled(true);
		} else {
			MCLogger.logError("No application found matching the project id for the project creation event: " + projectID); //$NON-NLS-1$
		}
		MCUtil.updateConnection(mcConnection);
		String projectName = event.has(MCConstants.KEY_NAME) ? event.getString(MCConstants.KEY_NAME) : null;
		if (projectName != null) {
			IOperationHandler handler = projectCreateHandlers.get(projectName);
			if (handler != null) {
				handler.operationComplete(true, null);
			}
		}
	}

	private void onProjectChanged(JSONObject event) throws JSONException {
		String projectID = event.getString(MCConstants.KEY_PROJECT_ID);
		MicroclimateApplication app = mcConnection.getAppByID(projectID);
		if (app == null) {
			MCLogger.logError("No application found matching the project id for the project changed event: " + projectID); //$NON-NLS-1$
			return;
		}
		
		app.setEnabled(true);
		
		// Update container id
		String containerId = null;
		if (event.has(MCConstants.KEY_CONTAINER_ID)) {
		    containerId = event.getString(MCConstants.KEY_CONTAINER_ID);
		}
		app.setContainerId(containerId);
	
        // Update ports
        JSONObject portsObj = event.getJSONObject(MCConstants.KEY_PORTS);

        if (portsObj != null && portsObj.has(MCConstants.KEY_EXPOSED_PORT)) {
        	int port = MCUtil.parsePort(portsObj.getString(MCConstants.KEY_EXPOSED_PORT));
    		app.setHttpPort(port);
        } else {
        	MCLogger.logError("No http port on project changed event for: " + app.name); //$NON-NLS-1$
        }

		if (portsObj != null && portsObj.has(MCConstants.KEY_EXPOSED_DEBUG_PORT)) {
			int debugPort = MCUtil.parsePort(portsObj.getString(MCConstants.KEY_EXPOSED_DEBUG_PORT));
			app.setDebugPort(debugPort);
			if (StartMode.DEBUG_MODES.contains(app.getStartMode()) && debugPort != -1) {
				app.reconnectDebugger();
			}
		} else {
			app.setDebugPort(-1);
		}
		
		if (event.has(MCConstants.KEY_AUTO_BUILD)) {
			boolean autoBuild = event.getBoolean(MCConstants.KEY_AUTO_BUILD);
			app.setAutoBuild(autoBuild);
		}
	}
	
	private void onProjectSettingsChanged(JSONObject event) throws JSONException {
		String projectID = event.getString(MCConstants.KEY_PROJECT_ID);
		MicroclimateApplication app = mcConnection.getAppByID(projectID);
		if (app == null) {
			MCLogger.logError("No application found matching the project id for the project settings changed event: " + projectID); //$NON-NLS-1$
			return;
		}
		
		app.setEnabled(true);
		
		// Update context root
		if (event.has(MCConstants.KEY_CONTEXT_ROOT)) {
			app.setContextRoot(event.getString(MCConstants.KEY_CONTEXT_ROOT));
		}
		
		// TODO: need to update ports?
	}

	private void onProjectStatusChanged(JSONObject event) throws JSONException {
		String projectID = event.getString(MCConstants.KEY_PROJECT_ID);
		MicroclimateApplication app = mcConnection.getAppByID(projectID);
		if (app == null) {
			// Likely a new project is being created
			mcConnection.refreshApps(projectID);
			MCUtil.updateConnection(mcConnection);
			return;
		}
		
		app.setEnabled(true);
		
		if (event.has(MCConstants.KEY_APP_STATUS)) {
			String appStatus = event.getString(MCConstants.KEY_APP_STATUS);
			app.setAppStatus(appStatus);
		}

		// Update build status if the project is not started or starting.
		if (event.has(MCConstants.KEY_BUILD_STATUS)) {
			String buildStatus = event.getString(MCConstants.KEY_BUILD_STATUS);
			String detail = ""; //$NON-NLS-1$
			if (event.has(MCConstants.KEY_DETAILED_BUILD_STATUS)) {
				detail = event.getString(MCConstants.KEY_DETAILED_BUILD_STATUS);
			}
			app.setBuildStatus(buildStatus, detail);
		}
		
		MCUtil.updateApplication(app);
	}

	private void onProjectRestart(JSONObject event) throws JSONException {
		String projectID = event.getString(MCConstants.KEY_PROJECT_ID);
		MicroclimateApplication app = mcConnection.getAppByID(projectID);
		if (app == null) {
			MCLogger.logError("No application found matching the project id for the project restart event: " + projectID); //$NON-NLS-1$
			return;
		}
		
		app.setEnabled(true);
		
		String status = event.getString(MCConstants.KEY_STATUS);
		if (!MCConstants.REQUEST_STATUS_SUCCESS.equalsIgnoreCase(status)) {
			MCLogger.logError("Project restart failed on the application: " + event.toString()); //$NON-NLS-1$
			MCUtil.openDialog(true,
					Messages.MicroclimateSocket_ErrRestartingProjectDialogTitle,
					NLS.bind(Messages.MicroclimateSocket_ErrRestartingProjectDialogMsg,
							app.name, status));
			return;
		}

		// This event should always have a 'ports' sub-object
		JSONObject portsObj = event.getJSONObject(MCConstants.KEY_PORTS);

		// The ports object should always have an http port
		if (portsObj != null && portsObj.has(MCConstants.KEY_EXPOSED_PORT)) {
			int port = MCUtil.parsePort(portsObj.getString(MCConstants.KEY_EXPOSED_PORT));
			app.setHttpPort(port);
		} else {
			MCLogger.logError("No http port on project restart event for: " + app.name); //$NON-NLS-1$
		}

		// Debug port will be missing if the restart was into Run mode.
		int debugPort = -1;
		if (portsObj != null && portsObj.has(MCConstants.KEY_EXPOSED_DEBUG_PORT)) {
			debugPort = MCUtil.parsePort(portsObj.getString(MCConstants.KEY_EXPOSED_DEBUG_PORT));
		}
		app.setDebugPort(debugPort);
		
		StartMode startMode = StartMode.get(event);
		app.setStartMode(startMode);
		
		// Update the application
		MCUtil.updateApplication(app);
		
		// Make sure no old debugger is running
		app.clearDebugger();
		
		if (StartMode.DEBUG_MODES.contains(startMode) && debugPort != -1) {
			app.connectDebugger();
		}
	}
	
	private void onProjectClosed(JSONObject event) throws JSONException {
		String projectID = event.getString(MCConstants.KEY_PROJECT_ID);
		MicroclimateApplication app = mcConnection.getAppByID(projectID);
		if (app == null) {
			MCLogger.logError("No application found for project being closed: " + projectID); //$NON-NLS-1$
			return;
		}
		app.setEnabled(false);
		MCUtil.updateConnection(mcConnection);
	}

	private void onProjectDeletion(JSONObject event) throws JSONException {
		String projectID = event.getString(MCConstants.KEY_PROJECT_ID);
		MicroclimateApplication app = mcConnection.removeApp(projectID);
		if (app == null) {
			MCLogger.logError("No application found for project being deleted: " + projectID); //$NON-NLS-1$
			return;
		}
		MCUtil.updateConnection(mcConnection);
		app.dispose();
	}

	public void registerOldSocketConsole(OldSocketConsole console) {
		MCLogger.log("Register socketConsole for projectID " + console.projectID); //$NON-NLS-1$
		this.oldSocketConsoles.add(console);
	}

	public void deregisterOldSocketConsole(OldSocketConsole console) {
		this.oldSocketConsoles.remove(console);
	}
	
	public void registerSocketConsole(SocketConsole console) {
		MCLogger.log("Register socketConsole for project: " + console.app.name); //$NON-NLS-1$
		this.socketConsoles.add(console);
	}

	public void deregisterSocketConsole(SocketConsole console) {
		this.socketConsoles.remove(console);
	}
	
	public void registerProjectCreateHandler(String projectName, IOperationHandler handler) {
		this.projectCreateHandlers.put(projectName, handler);
	}
	
	public void deregisterProjectCreateHandler(String projectName) {
		this.projectCreateHandlers.remove(projectName);
	}

	private void onContainerLogs(JSONObject event) throws JSONException {
		String projectID = event.getString(MCConstants.KEY_PROJECT_ID);
		String logContents = event.getString(MCConstants.KEY_LOGS);
		MCLogger.log("Update logs for project " + projectID); //$NON-NLS-1$

		for (OldSocketConsole console : this.oldSocketConsoles) {
			if (console.projectID.equals(projectID)) {
				try {
					console.update(logContents);
				}
				catch(IOException e) {
					MCLogger.logError("Error updating console " + console.getName(), e);	// $NON-NLS-1$
				}
			}
		}
	}
	
	private void onLogUpdate(JSONObject event) throws JSONException {
		String projectID = event.getString(MCConstants.KEY_PROJECT_ID);
		String type = event.getString(MCConstants.KEY_LOG_TYPE);
		String logName = event.getString(MCConstants.KEY_LOG_NAME);
		MCLogger.log("Update the " + logName + " log for project: " + projectID); //$NON-NLS-1$ //$NON-NLS-2$

		for (SocketConsole console : this.socketConsoles) {
			if (console.app.projectID.equals(projectID) && console.logInfo.isThisLogInfo(type, logName)) {
				try {
					String logContents = event.getString(MCConstants.KEY_LOGS);
					boolean reset = event.getBoolean(MCConstants.KEY_LOG_RESET);
					console.update(logContents, reset);
				}
				catch(IOException e) {
					MCLogger.logError("Error updating console " + console.getName(), e);	// $NON-NLS-1$
				}
			}
		}
	}
	
	private void onValidationEvent(JSONObject event) throws JSONException {
		String projectID = event.getString(MCConstants.KEY_PROJECT_ID);
		MicroclimateApplication app = mcConnection.getAppByID(projectID);
		if (app == null) {
			MCLogger.logError("No application found for project: " + projectID); //$NON-NLS-1$
			return;
		}
		
		// Clear out any old validation objects
		app.resetValidation();
		
		// If the validation is successful then just return
		String status = event.getString(MCConstants.KEY_VALIDATION_STATUS);
		if (MCConstants.VALUE_STATUS_SUCCESS.equals(status)) {
			// Nothing to do
			return;
		}
		
		// If the validation is not successful, create validation objects for each problem
		if (event.has(MCConstants.KEY_VALIDATION_RESULTS)) {
			JSONArray results = event.getJSONArray(MCConstants.KEY_VALIDATION_RESULTS);
			for (int i = 0; i < results.length(); i++) {
				JSONObject result = results.getJSONObject(i);
				String severity = result.getString(MCConstants.KEY_SEVERITY);
				String filename = result.getString(MCConstants.KEY_FILENAME);
				String filepath = result.getString(MCConstants.KEY_FILEPATH);
				String type = null;
				if (result.has(MCConstants.KEY_TYPE)) {
					type = result.getString(MCConstants.KEY_TYPE);
				}
				String details = result.getString(MCConstants.KEY_DETAILS);
				String quickFixId = null;
				String quickFixDescription = null;
				if (result.has(MCConstants.KEY_QUICKFIX) && supportsQuickFix(app, type, filename)) {
					JSONObject quickFix = result.getJSONObject(MCConstants.KEY_QUICKFIX);
					quickFixId = quickFix.getString(MCConstants.KEY_FIXID);
					quickFixDescription = quickFix.getString(MCConstants.KEY_DESCRIPTION);
				}
				if (MCConstants.VALUE_SEVERITY_WARNING.equals(severity)) {
					app.validationWarning(filepath, details, quickFixId, quickFixDescription);
				} else {
					app.validationError(filepath, details, quickFixId, quickFixDescription);
				}
			}
		} else {
			MCLogger.log("Validation event indicates failure but no validation results,"); //$NON-NLS-1$
		}
	}
	
	private boolean supportsQuickFix(MicroclimateApplication app, String type, String filename) {
		// The regenerate job only works in certain cases so only show the quickfix in the working cases
		if (!MCConstants.VALUE_TYPE_MISSING.equals(type) || app.projectType.isType(ProjectType.TYPE_DOCKER)) {
			return false;
		}
		if (MCConstants.DOCKERFILE.equals(filename)) {
			return true;
		}
		if (app.projectType.isType(ProjectType.TYPE_LIBERTY) && MCConstants.DOCKERFILE_BUILD.equals(filename)) {
			return true;
		}
		return false;
	}

	boolean blockUntilFirstConnection() {
		final int delay = 100;
		final int timeout = 2500;
		int waited = 0;
		while(!hasConnected && waited < timeout) {
			try {
				Thread.sleep(delay);
				waited += delay;

				if (waited % (5 * delay) == 0) {
					MCLogger.log("Waiting for MicroclimateSocket initial connection"); //$NON-NLS-1$
				}
			}
			catch(InterruptedException e) {
				MCLogger.logError(e);
			}
		}
		MCLogger.log("MicroclimateSocket initialized in time ? " + hasConnected); //$NON-NLS-1$
		return hasConnected;
	}
}
