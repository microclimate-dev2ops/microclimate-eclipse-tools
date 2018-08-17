package com.ibm.microclimate.core.server;

import com.ibm.microclimate.core.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;

/**
 * One of these threads exists for each MicroclimateServer. Each application's state is updated by
 * its MicroclimateSocket whenever a state change event is received. This thread periodically checks for such a
 * state change, and updates the ServerBehaviour accordingly.
 *
 * @author timetchells@ibm.com
 *
 */
public class MicroclimateServerMonitorThread extends Thread {

	public static final int POLL_RATE_MS = 2500;

	private final MicroclimateServerBehaviour serverBehaviour;
	private final MicroclimateApplication app;

	private int lastKnownState;

	private volatile boolean run = true;

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

			try {
				Thread.sleep(POLL_RATE_MS);
			}
			catch(InterruptedException e) {}
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
		// MCLogger.log("App status is " + status);
		if (status == null) {
			return -1;
		}
		return MicroclimateServerBehaviour.appStatusToServerState(status);
	}
}
