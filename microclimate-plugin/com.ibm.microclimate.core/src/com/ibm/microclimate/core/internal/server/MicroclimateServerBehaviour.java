package com.ibm.microclimate.core.internal.server;

import java.io.IOException;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

import com.ibm.microclimate.core.Activator;
import com.ibm.microclimate.core.MCLogger;
import com.ibm.microclimate.core.internal.HttpUtil;
import com.sun.jdi.connect.AttachingConnector;

public class MicroclimateServerBehaviour extends ServerBehaviourDelegate {

	private String projectID;

	private MicroclimateServerMonitorThread monitorThread;

	@Override
	public void initialize(IProgressMonitor monitor) {
        super.initialize(monitor);

		MCLogger.log("Initializing MicroclimateServerBehaviour for " + getServer().getName());

		projectID = getServer().getAttribute(MicroclimateServer.ATTR_PROJ_ID, "");
		if (projectID.isEmpty()) {
			MCLogger.logError("No projectID attribute");
		}

		monitorThread = new MicroclimateServerMonitorThread(this);
		monitorThread.start();

		/*
		if (!updateState()) {
			MCLogger.logError("Failed to contact server when initializing");
			setServerState(IServer.STATE_STOPPED);
		}
		*/
	}

	@Override
	public IStatus canStop() {
		// TODO when FW supports this
		return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, "Not yet supported", null);
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
	public void dispose() {
		MCLogger.log("Dispose " + getServer().getName());
		monitorThread.disable();
	}

	/**
	 * Public wrapper for protected setServerState
	 */
	public void setServerState_(int serverState) {
		setServerState(serverState);
	}

	String getProjectID() {
		return projectID;
	}

	@Override
	public void restart(String launchMode) throws CoreException {
		MCLogger.log("Restarting " + getServer().getHost() + ":" + getServer().getServerPorts(null)[0].getPort()
				+ " in " + launchMode + " mode");

		int currentState = monitorThread.getLastKnownState();
		MCLogger.log("Current status = " + serverStateToAppStatus(currentState));

		if (IServer.STATE_STARTING == currentState) {
			waitForStarted();
		}

		String url = "http://localhost:9091/api/v1/projects/action";

		JsonObject restartProjectPayload = Json.createObjectBuilder()
				.add("action", "restart")
				.add("mode", launchMode)
				.add("projectID", projectID)
				.build();

		try {
			// returns an operationID if success - what to do with it?
			HttpUtil.post(url, restartProjectPayload);
		} catch (IOException e) {
			MCLogger.logError("Error POSTing restart request", e);
			return;
		}
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


		waitForState(IServer.STATE_STARTING, getStartTimeoutMs());
		if ("debug".equalsIgnoreCase(launchMode)) {
			//onStartingInDebug();
		}
		waitForStarted();
	}

	private void onStartingInDebug() {
		AttachingConnector connector = LaunchUtilities.getAttachingConnector();

		// TODO obviously
		// Is this supposed to be the local port (IE any free port) or the port we're connecting to?
		int debugPort = 32858;

		Map connectorArgs = connector.defaultArguments();
        connectorArgs = LaunchUtilities.configureConnector(connectorArgs, getServer().getHost(), debugPort);
	}


	private int getStartTimeoutMs() {
		return getServer().getStartTimeout() * 1000;
	}

	private void waitForStarted() {
		waitForState(IServer.STATE_STARTED, getStartTimeoutMs());
	}

	private void waitForState(int state, int timeoutMs) {
		long startTime = System.currentTimeMillis();
		int pollRateMs = 1000;

		while((System.currentTimeMillis() - startTime) < timeoutMs) {
			// TODO how to listen for user canceling?

			try {
				MCLogger.log("Waiting for server restart");
				Thread.sleep(pollRateMs);
			}
			catch(InterruptedException e) {
				MCLogger.logError(e);
			}

			// the ServerMonitorThread will update the state
			if (getServer().getServerState() == state) {
				MCLogger.log("Server is done restarting");
				break;
			}
		}

		if (getServer().getServerState() != state) {
			MCLogger.logError("Server did not enter state " + serverStateToAppStatus(state) + " in " + timeoutMs);
			setServerState(IServer.STATE_UNKNOWN);
		}
	}

	static int appStatusToServerState(String appStatus) {
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

	static String serverStateToAppStatus(int serverState) {
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
