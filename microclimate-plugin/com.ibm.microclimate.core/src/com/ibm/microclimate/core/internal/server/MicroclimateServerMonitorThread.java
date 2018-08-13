package com.ibm.microclimate.core.internal.server;

import java.io.IOException;
import java.net.SocketTimeoutException;

import org.eclipse.wst.server.core.IServer;
import org.json.JSONException;
import org.json.JSONObject;

import com.ibm.microclimate.core.MCLogger;
import com.ibm.microclimate.core.internal.HttpUtil;
import com.ibm.microclimate.core.internal.HttpUtil.HttpResult;

public class MicroclimateServerMonitorThread extends Thread {

	public static final int POLL_RATE_MS = 2000;

	private volatile boolean run = true;

	private final MicroclimateServerBehaviour serverBehaviour;

	private int lastKnownState;

	private int stateWaitingFor = -1;
	private int stateWaitingTimeout = -1;
	private int waitedMs = 0;

	MicroclimateServerMonitorThread(MicroclimateServerBehaviour serverBehaviour) {
		this.serverBehaviour = serverBehaviour;

		setPriority(Thread.MIN_PRIORITY + 1);
		setDaemon(true);
		setName(serverBehaviour.getServer().getName() + " MonitorThread");
	}

	@Override
	public void run() {
		MCLogger.log("Start MCMonitorThread");
		while(run && !Thread.currentThread().isInterrupted()) {
			updateState();

			if (isWaitingForState()) {
				if (lastKnownState == stateWaitingFor) {
					onFinishWaitingForState(true);
				}
				else if (waitedMs > stateWaitingTimeout) {
					onFinishWaitingForState(false);
				}
				else {
					waitedMs += POLL_RATE_MS;
				}
			}

			try {
				Thread.sleep(POLL_RATE_MS);
			}
			catch(InterruptedException e) {
				// MCLogger.logError(e);
			}
		}

		MCLogger.log("Stop MCMonitorThread");
	}

	int getLastKnownState() {
		return lastKnownState;
	}

	void disable() {
		run = false;
	}

	boolean updateState() {
		int state = getAppState();
		if (state == -1) {
			// MCLogger.logError("Failed to update app state");
			return false;
		}
		else {
			serverBehaviour.setServerState_(state);
			lastKnownState = state;
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
			statusUrl = String.format(statusUrl, serverBehaviour.getProjectID());

		String appStatusResponse = null;

		try {
			// Sometimes during restart, FW will be really slow to reply,
			// and this request will time out.
			HttpResult result = HttpUtil.get(statusUrl);

			if (!result.isGoodResponse) {
				MCLogger.logError("Received bad response from server: " + result.responseCode);
				if (result.error.contains("Unknown project")) {
					MCLogger.logError("Project " + serverBehaviour.getProjectID()+ " is unknown - deleted or disabled");
				}
				else {
					MCLogger.logError("Error message: " + result.error);
				}
				return IServer.STATE_UNKNOWN;
			}

			appStatusResponse = result.response;
		}
		catch (IOException e) {
			if (e instanceof SocketTimeoutException) {
				// This happens in the normal course of application restarts, so we can safely ignore it
				MCLogger.logError("Server state update request timed out");
				return -1;
			}
			else if (e.getMessage().contains("Connection refused")) {
				// should display a msg to the user in this case
				MCLogger.logError("Connection refused; Microclimate is not running at the expected URL " + statusUrl);
				return IServer.STATE_UNKNOWN;
			}
			else {
				// Unexpected error
				MCLogger.logError(e);
				return IServer.STATE_UNKNOWN;
			}
		}


		final String appStatusKey = "appStatus";
		try {
			JSONObject appStateJso = new JSONObject(appStatusResponse);
			if (appStateJso.has(appStatusKey)) {
				String status = appStateJso.getString(appStatusKey);

				// MCLogger.log("Update app state to " + status);
				return MicroclimateServerBehaviour.appStatusToServerState(status);
			}
		} catch (JSONException e) {
			MCLogger.logError("JSON had app status, but exception occurred anyway", e);
		}

		return -1;
	}

	void waitForState(int state, int timeout) {
		stateWaitingFor = state;
		stateWaitingTimeout = timeout;
	}

	boolean isWaitingForState() {
		return stateWaitingFor != -1;
	}

	void onFinishWaitingForState(boolean success) {

		String state = MicroclimateServerBehaviour.serverStateToAppStatus(stateWaitingFor);

		if (success) {
			MCLogger.log("Finished waiting for state: " + state);
		}
		else {
			MCLogger.logError("Waiting for state " + state + " timed out");
		}

		stateWaitingFor = -1;
		stateWaitingTimeout = -1;
		waitedMs = 0;
	}
}
