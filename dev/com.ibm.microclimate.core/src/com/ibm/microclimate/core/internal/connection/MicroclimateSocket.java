/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core.internal.connection;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.osgi.util.NLS;
import org.json.JSONException;
import org.json.JSONObject;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.constants.MCConstants;
import com.ibm.microclimate.core.internal.messages.Messages;
import com.ibm.microclimate.core.internal.server.MicroclimateServerBehaviour;
import com.ibm.microclimate.core.internal.server.console.SocketConsole;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Wrapper for a SocketIO client socket, which connects to FileWatcher and listens for project state changes,
 * then updates the corresponding MicroclimateApplication's state.
 * One of these exists for each MicroclimateConnection. That connection is stored here so we can access
 * its applications.
 *
 * @author timetchells@ibm.com
 *
 */
public class MicroclimateSocket {

	public final Socket socket;

	public final URI socketUri;

	private boolean hasLostConnection = false;

	private volatile boolean hasConnected = false;

	private Set<SocketConsole> socketConsoles = new HashSet<>();

	// Track the previous Exception so we don't spam the logs with the same connection failure message
	private Exception previousException;

	// SocketIO Event names
	private static final String
			EVENT_PROJECT_CHANGED = "projectChanged", 				//$NON-NLS-1$
			EVENT_PROJECT_STATUS_CHANGE = "projectStatusChanged", 	//$NON-NLS-1$
			EVENT_PROJECT_RESTART = "projectRestartResult", 		//$NON-NLS-1$
			EVENT_PROJECT_CLOSED = "projectClosed", 				//$NON-NLS-1$
			EVENT_PROJECT_DELETION = "projectDeletion", 			//$NON-NLS-1$
			EVENT_CONTAINER_LOGS = "container-logs";				//$NON-NLS-1$

	public MicroclimateSocket(MicroclimateConnection mcConnection) throws URISyntaxException {

		socketUri = mcConnection.baseUrl;

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
					onProjectDeletion(event);
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
				// MCLogger.log(EVENT_CONTAINER_LOGS + ": " + arg0[0].toString()); //$NON-NLS-1$
				MCLogger.log(EVENT_CONTAINER_LOGS);

				try {
					JSONObject event = new JSONObject(arg0[0].toString());
					onLogUpdate(event);
				} catch (JSONException e) {
					MCLogger.logError("Error parsing JSON: " + arg0[0].toString(), e); //$NON-NLS-1$
				}
			}
		});

		socket.connect();

		MCLogger.log("Created MicroclimateSocket connected to " + socketUri); //$NON-NLS-1$
	}

	private void onProjectChanged(JSONObject event) throws JSONException {
		MicroclimateServerBehaviour serverBehaviour = getServerForEvent(event);
		if (serverBehaviour == null) {
			// This is OK. It will happen if the project that's changed is not linked to a server.
			return;
		}
        MicroclimateApplication app = serverBehaviour.getApp();

        // Update ports
        JSONObject portsObj = event.getJSONObject(MCConstants.KEY_PORTS);

		// If the app is started, get the http port. If it's not started, this will be missing, and we can't proceed.
        if (!portsObj.has(MCConstants.KEY_EXPOSED_PORT)) {
        	MCLogger.log(String.format("No %s key - %s is not running.", 		//$NON-NLS-1$
        			MCConstants.KEY_EXPOSED_PORT, app.name));
        	return;
        }

		int port = parsePort(portsObj.getString(MCConstants.KEY_EXPOSED_PORT));
		if (port != -1) {
			app.setHttpPort(port);
		}

		if (portsObj.has(MCConstants.KEY_EXPOSED_DEBUG_PORT)) {
			int debugPort = parsePort(portsObj.getString(MCConstants.KEY_EXPOSED_DEBUG_PORT));
			app.setDebugPort(debugPort);
			if (serverBehaviour.getServer().getMode() == ILaunchManager.DEBUG_MODE && debugPort != -1) {
				// If the debug connection is lost then reconnect
				final ILaunch launch = serverBehaviour.getServer().getLaunch();
				if (launch == null || launch.getLaunchMode() != ILaunchManager.DEBUG_MODE || launch.getDebugTarget() == null) {
					if (launch != null) {
						// Remove the old launch
						DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);
					}
					Job job = new Job("Reconnect debugger") {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							serverBehaviour.reconnectDebug(monitor);
							return Status.OK_STATUS;
						}
					};
					job.setPriority(Job.LONG);
					job.schedule();
				}
			}
		} else {
			app.setDebugPort(-1);
		}
	}

	private void onProjectStatusChanged(JSONObject event) throws JSONException {
		MicroclimateServerBehaviour serverBehaviour = getServerForEvent(event);
		if (serverBehaviour == null) {
			// This is OK. It will happen if the project that's changed is not linked to a server.
			return;
		}

		serverBehaviour.updateServerState(event);
	}

	private void onProjectRestart(JSONObject event) throws JSONException {
		// The restart event doesn't provide a project state. We just update the ports here
		// The project state events will be received separately and handled normally.
		MicroclimateServerBehaviour serverBehaviour = getServerForEvent(event);
		if (serverBehaviour == null) {
			MCLogger.logError("Failed to get serverBehaviour, aborting state update triggered by restart"); //$NON-NLS-1$
			return;
		}

		String status = event.getString(MCConstants.KEY_STATUS);
		if (!MCConstants.REQUEST_STATUS_SUCCESS.equalsIgnoreCase(status)) {
			MCLogger.logError("Project restart failed on the server: " + event.toString()); //$NON-NLS-1$
			MCUtil.openDialog(true,
					Messages.MicroclimateSocket_ErrRestartingProjectDialogTitle,
					NLS.bind(Messages.MicroclimateSocket_ErrRestartingProjectDialogMsg,
							serverBehaviour.getServer().getName(), status));
			return;
		}

		// this event should always have a 'ports' sub-object
		JSONObject portsObj = event.getJSONObject(MCConstants.KEY_PORTS);

		// ports object should always have an http port
		int port = parsePort(portsObj.getString(MCConstants.KEY_EXPOSED_PORT));
		if (port != -1) {
			serverBehaviour.getApp().setHttpPort(port);
		}

		// Debug port will obviously be missing if the restart was into Run mode.
		if (portsObj.has(MCConstants.KEY_EXPOSED_DEBUG_PORT)) {
			int debugPort = parsePort(portsObj.getString(MCConstants.KEY_EXPOSED_DEBUG_PORT));
			if (debugPort != -1) {
				serverBehaviour.getApp().setDebugPort(debugPort);
			}
		}
	}

	private void onProjectDeletion(JSONObject event) throws JSONException {
		MicroclimateServerBehaviour serverBehaviour = getServerForEvent(event);
		if (serverBehaviour == null) {
			// This is normal if this project is not linked to an Eclipse server.
			// It's an error if this project IS linked and above still returned null.
			MCLogger.log("Failed to get serverBehaviour, aborting projectDeletion status update"); //$NON-NLS-1$
			return;
		}

		serverBehaviour.onProjectDisableOrDelete();
	}

	private MicroclimateServerBehaviour getServerForEvent(JSONObject event) throws JSONException {
		String projectID = event.getString(MCConstants.KEY_PROJECT_ID);

		MicroclimateServerBehaviour server = MicroclimateApplication.getServerWithProjectID(projectID);
		if (server == null) {
			// can't recover from this
			// This is normal if the project has been deleted or disabled
			MCLogger.log("No server linked to project with ID " + projectID); //$NON-NLS-1$
			return null;
		}

		return server;
	}

	public void registerSocketConsole(SocketConsole console) {
		MCLogger.log("Register socketConsole for projectID " + console.projectID);
		this.socketConsoles.add(console);
	}

	public void deregisterSocketConsole(SocketConsole console) {
		this.socketConsoles.remove(console);
	}

	private void onLogUpdate(JSONObject event) throws JSONException {
		String projectID = event.getString(MCConstants.KEY_PROJECT_ID);
		String logContents = event.getString(MCConstants.KEY_LOGS);
		MCLogger.log("Update logs for project " + projectID);

		for (SocketConsole console : this.socketConsoles) {
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

	private int parsePort(String portStr) {
		try {
			return Integer.parseInt(portStr);
		}
		catch(NumberFormatException e) {
			MCLogger.logError(String.format("Couldn't parse port from \"%s\"", portStr), e); //$NON-NLS-1$
			return -1;
		}
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
