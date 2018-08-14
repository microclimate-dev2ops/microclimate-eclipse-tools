package com.ibm.microclimate.core.internal.server;

import com.ibm.microclimate.core.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;

public class MicroclimateServerMonitorThread extends Thread {

	public static final int POLL_RATE_MS = 2500;

	private volatile boolean run = true;

	private final MicroclimateServerBehaviour serverBehaviour;
	private final MicroclimateApplication app;

	private int lastKnownState;

	private int stateWaitingFor = -1;
	private int stateWaitingTimeout = -1;
	private int waitedMs = 0;

	MicroclimateServerMonitorThread(MicroclimateServerBehaviour serverBehaviour) {
		this.serverBehaviour = serverBehaviour;

		app = serverBehaviour.getApp();

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

	/**
	 * This should only be called once, just before this thread is started, in the Behaviour's initialize method.
	 */
	void setInitialState(int state) {
		lastKnownState = state;
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
	 * @return IServer.STATE constant corresponding to the current status
	 */
	int getAppState() {
		// TODO display build status somewhere if it's building.
		String status = app.getAppStatus();
		MCLogger.log("App status is " + status);
		if (status == null) {
			return -1;
		}
		return MicroclimateServerBehaviour.appStatusToServerState(status);
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
