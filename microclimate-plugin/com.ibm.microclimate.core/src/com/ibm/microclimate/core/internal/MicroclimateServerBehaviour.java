package com.ibm.microclimate.core.internal;

import java.io.IOException;
import java.io.StringReader;
import java.net.SocketTimeoutException;

import javax.json.Json;
import javax.json.JsonObject;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

import com.ibm.microclimate.core.Activator;

public class MicroclimateServerBehaviour extends ServerBehaviourDelegate {

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
	public void restart(String launchMode) throws CoreException {
		System.out.println("Restarting in " + launchMode + " mode");

		String projectID = getServer().getAttribute(MicroclimateServer.ATTR_PROJ_ID, "");
		String currentStatus = getAppStatus(projectID);
		System.out.println("Current status = " + currentStatus);

		if (currentStatus == null) {
			System.err.println("Error getting app status");
			return;
		}
		else if (IServer.STATE_STARTING == convertAppStatusToServerState(currentStatus)) {
			waitForStarted(projectID);
		}

		String url = "http://localhost:9091/api/v1/projects/action";

		JsonObject restartProjectPayload = Json.createObjectBuilder()
				.add("action", "setExecutionMode")
				.add("mode", launchMode)
				.add("projectID", projectID)
				.build();

		/*String response =*/ MicroclimateConnection.post(url, restartProjectPayload);
		// returns an operationID if success - what to do with it?
		// System.out.println(response);

		try {
			// It takes a moment for the project to update to Stopping state.
			System.out.println("Waiting for project to receive execution mode change request");
			Thread.sleep(2000);
		}
		catch(InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Waiting for server to be ready again");

		waitForStarted(projectID);
	}

	private void waitForStarted(String projectID) {
		long elapsed = 0;
		int pollRateMs = 500;

		while(elapsed < getServer().getStartTimeout() * 1000) {
			// TODO how to listen for user canceling?

			try {
				Thread.sleep(pollRateMs);
				elapsed += pollRateMs;
			}
			catch(InterruptedException e) {
				e.printStackTrace();
			}

			// System.out.println("Waited " + elapsed + "ms");

			Long startTime = System.currentTimeMillis();

			String status = getAppStatus(projectID);
			if (status == null) {
				continue;
			}
			System.out.println("Update server status to " + status);
			setServerState(convertAppStatusToServerState(status));

			elapsed += System.currentTimeMillis() - startTime;

			if (getServer().getServerState() == IServer.STATE_STARTED) {
				System.out.println("Server is done restarting");
				break;
			}
		}

		if (getServer().getServerState() != IServer.STATE_STARTED) {
			System.err.println("Server did not restart in time!");
			setServerState(IServer.STATE_UNKNOWN);
		}
	}

	private static String getAppStatus(String projectID) {
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
				System.err.println("Server state update request timed out");
			}
			else {
				e.printStackTrace();
			}
			return null;
		}

		JsonObject appStateJso = Json.createReader(new StringReader(appStatusResponse)).readObject();

		final String appStatusKey = "appStatus";
		String status = "unknown";
		if (appStateJso.containsKey(appStatusKey)) {
			status = appStateJso.getString(appStatusKey);
			return status;
		}

		return null;
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
}
