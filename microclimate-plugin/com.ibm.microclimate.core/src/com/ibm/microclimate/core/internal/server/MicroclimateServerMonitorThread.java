package com.ibm.microclimate.core.internal.server;

import java.io.IOException;
import java.io.StringReader;
import java.net.SocketTimeoutException;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;

import com.ibm.microclimate.core.MCLogger;
import com.ibm.microclimate.core.internal.HttpUtil;
import com.ibm.microclimate.core.internal.HttpUtil.HttpResult;

public class MicroclimateServerMonitorThread extends Thread {

	public static final int POLL_RATE_MS = 1000;

	private volatile boolean run = true;

	private final MicroclimateServerBehaviour serverBehaviour;

	private int lastKnownState;

	private int stateWaitingFor = -1;
	private int stateWaitingTimeout = -1;
	private int waitedMs = 0;
	private Function<?, ?> stateWaitingCallback;

	MicroclimateServerMonitorThread(MicroclimateServerBehaviour serverBehaviour) {
		this.serverBehaviour = serverBehaviour;
	}

	@Override
	public void run() {
		MCLogger.log("Start MCMonitorThread");
		while(run && !Thread.currentThread().isInterrupted()) {
			try {
				Thread.sleep(POLL_RATE_MS);
			}
			catch(InterruptedException e) {
				MCLogger.logError(e);
			}

			// MCLogger.log("Waited " + elapsed + "ms");
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
			MCLogger.logError("Failed to update app state");
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
				if (result.error.contains("Unknown project")) {
					MCLogger.logError("Project " + serverBehaviour.getProjectID()+ " is unknown - deleted or disabled");
				}
				return -1;
			}

			appStatusResponse = result.response;
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
			// MCLogger.log("Update app state to " + status);
			return MicroclimateServerBehaviour.appStatusToServerState(status);
		}

		return -1;
	}

	void waitForState(int state, int timeout, Function<?, ?> callback) {
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

		if (stateWaitingCallback != null) {
			stateWaitingCallback.apply(null);
		}

		stateWaitingFor = -1;
		stateWaitingTimeout = -1;
		waitedMs = 0;
		stateWaitingCallback = null;
	}
}
