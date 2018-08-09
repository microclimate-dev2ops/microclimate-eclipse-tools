package com.ibm.microclimate.core.internal;

import java.io.IOException;
import java.io.StringReader;
import java.net.SocketTimeoutException;

import javax.json.Json;
import javax.json.JsonObject;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

import com.ibm.microclimate.core.Activator;
import com.ibm.microclimate.core.MCLogger;

public class MicroclimateServerBehaviour extends ServerBehaviourDelegate {

	private final String projectID = "abc";

	/*
	public MicroclimateServerBehaviour() {
		MCLogger.log("Initializing MicroclimateServerBehaviour for " + getServer().getName());

		projectID = getServer().getAttribute(MicroclimateServer.ATTR_PROJ_ID, "");
		if (projectID.isEmpty()) {
			MCLogger.logError("No projectID attribute");
		}

		if (!updateState()) {
			MCLogger.logError("Failed to contact server when initializing");
			setServerState(IServer.STATE_STOPPED);
		}
	}
	*/

	@Override
	public IStatus canStop() {
		// TODO when FW supports this
		return new Status(IStatus.INFO, Activator.PLUGIN_ID, 0, "Can stop but nothing will happen", null);
	}

	@Override
	public void stop(boolean force) {
		setServerState(IServer.STATE_STOPPED);
		// TODO when FW supports this
	}

	@Override
	public IStatus canPublish() {
		return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, "Microclimate server does not support publish", null);
	}

	@Override
	public void publish(int kind, java.util.List<IModule[]> modules, IProgressMonitor monitor, IAdaptable info) {
		// do nothing :)
	}


	@Override
	public void restart(String launchMode) throws CoreException {
		MCLogger.log("Restarting in " + launchMode + " mode");

		int currentStatus = getAppState();
		MCLogger.log("Current status = " + convertServerStateToAppStatus(currentStatus));

		if (IServer.STATE_STARTING == currentStatus) {
			waitForStarted();
		}

		String url = "http://localhost:9091/api/v1/projects/action";

		JsonObject restartProjectPayload = Json.createObjectBuilder()
				.add("action", "setExecutionMode")
				.add("mode", launchMode)
				.add("projectID", projectID)
				.build();

		/*String response =*/
		MicroclimateConnection.post(url, restartProjectPayload);
		// returns an operationID if success - what to do with it?
		// MCLogger.log(response);

		try {
			// It takes a moment for the project to update to Stopping state.
			MCLogger.log("Waiting for project to receive execution mode change request");
			Thread.sleep(2000);
		}
		catch(InterruptedException e) {
			MCLogger.logError(e);
		}
		MCLogger.log("Waiting for server to be ready again");

		waitForStarted();
	}

	private void waitForStarted() {
		long startTime = System.currentTimeMillis();
		int pollRateMs = 500;

		while((System.currentTimeMillis() - startTime) < (getServer().getStartTimeout() * 1000)) {
			// TODO how to listen for user canceling?

			try {
				Thread.sleep(pollRateMs);
			}
			catch(InterruptedException e) {
				MCLogger.logError(e);
			}

			// MCLogger.log("Waited " + elapsed + "ms");
			updateState();

			if (getServer().getServerState() == IServer.STATE_STARTED) {
				MCLogger.log("Server is done restarting");
				break;
			}
		}

		if (getServer().getServerState() != IServer.STATE_STARTED) {
			MCLogger.logError("Server did not restart in time!");
			setServerState(IServer.STATE_UNKNOWN);
		}
	}

	boolean updateState() {
		int state = getAppState();
		if (state == -1) {
			MCLogger.logError("Failed to update app state");
			return false;
		}
		else {
			MCLogger.log("Update server status to " + state);
			setServerState(state);
		}
		return true;
	}

	/**
	 * Get the status of the app with the given projectID
	 * @param projectID
	 * @return IServer.STATE constant corresponding to the current status, or -1 if there's an error.
	 */
	int getAppState() {
		String statusUrl = "http://localhost:9091/api/v1/projects/status/?type=appState&projectID=%s";
		statusUrl = String.format(statusUrl, projectID);

		String appStatusResponse = null;

		try {
			// Sometimes during restart, FW will be really slow to reply,
			// and this request will time out.
			appStatusResponse = MicroclimateConnection.get(statusUrl);
		}
		catch (IOException e) {
			if (e instanceof SocketTimeoutException) {
				MCLogger.logError("Server state update request timed out");
			}
			else {
				MCLogger.logError(e);
			}
			return -1;
		}

		JsonObject appStateJso = Json.createReader(new StringReader(appStatusResponse)).readObject();

		final String appStatusKey = "appStatus";
		String status = "unknown";
		if (appStateJso.containsKey(appStatusKey)) {
			status = appStateJso.getString(appStatusKey);
			return convertAppStatusToServerState(status);
		}

		return -1;
	}

	static int convertAppStatusToServerState(String appStatus) {
		if ("started".equals(appStatus)) {
			return IServer.STATE_STARTED;
		}
		else if ("starting".equals(appStatus)) {
			return IServer.STATE_STARTING;
		}
		else if ("stopping".equals(appStatus)) {
			return IServer.STATE_STOPPING;
		}
		else if ("stopped".equals(appStatus)) {
			return IServer.STATE_STOPPED;
		}
		else {
			return IServer.STATE_UNKNOWN;
		}
	}

	static String convertServerStateToAppStatus(int serverState) {
		if (serverState == IServer.STATE_STARTED) {
			return "started";
		}
		else if (serverState == IServer.STATE_STARTING) {
			return "starting";
		}
		else if (serverState == IServer.STATE_STOPPING) {
			return "stopping";
		}
		else if (serverState == IServer.STATE_STOPPED) {
			return "stopped";
		}
		else {
			return "unknown";
		}
	}
}
