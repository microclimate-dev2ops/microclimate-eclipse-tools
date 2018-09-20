/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core.internal.server;

import java.io.IOException;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.json.JSONException;
import org.json.JSONObject;

import com.ibm.microclimate.core.MicroclimateCorePlugin;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnectionManager;
import com.ibm.microclimate.core.internal.constants.AppState;
import com.ibm.microclimate.core.internal.constants.BuildStatus;
import com.ibm.microclimate.core.internal.constants.MCConstants;
import com.ibm.microclimate.core.internal.messages.Messages;
import com.ibm.microclimate.core.internal.server.console.MicroclimateConsoleFactory;
import com.ibm.microclimate.core.internal.server.debug.MicroclimateDebugConnector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;

/**
 *
 * @author timetchells@ibm.com
 *
 */
@SuppressWarnings("restriction")
public class MicroclimateServerBehaviour extends ServerBehaviourDelegate {

	// Only set these once, in initialize().
	private MicroclimateApplication app;
	private Set<IOConsole> consoles;

	private String suffix = null;
	private boolean isErrored;

	// in seconds
	public static final int DEFAULT_DEBUG_CONNECT_TIMEOUT = 3;

	@Override
	public void initialize(IProgressMonitor monitor) {
		MCLogger.log("Initializing MicroclimateServerBehaviour for " + getServer().getName()); //$NON-NLS-1$
		setServerState(IServer.STATE_UNKNOWN);

		String projectID = getServer().getAttribute(MicroclimateServer.ATTR_PROJ_ID, ""); //$NON-NLS-1$
		if (projectID.isEmpty()) {
			onInitializeFailure(NLS.bind(Messages.MicroclimateServerBehaviour_ErrMissingAttribute,
					MicroclimateServer.ATTR_PROJ_ID));
			return;
		}

		String mcConnectionBaseUrl = getServer().getAttribute(MicroclimateServer.ATTR_MCC_URL, ""); //$NON-NLS-1$
		if (mcConnectionBaseUrl.isEmpty()) {
			onInitializeFailure(NLS.bind(Messages.MicroclimateServerBehaviour_ErrMissingAttribute,
					MicroclimateServer.ATTR_MCC_URL));
			return;
		}
		MicroclimateConnection mcConnection = MicroclimateConnectionManager.getActiveConnection(mcConnectionBaseUrl);
		if (mcConnection == null) {
			onMicroclimateDisconnect(mcConnectionBaseUrl);
			//onInitializeFailure("Couldn't connect to Microclimate at " + mcConnectionBaseUrl +
			//		", try re-creating the connection.");
			return;
		}

		app = mcConnection.getAppByID(projectID);
		if (app == null) {
			// TODO the user doesn't want to have to look up the project by ID
			onInitializeFailure(NLS.bind(Messages.MicroclimateServerBehaviour_MissingProjectID,
					projectID, mcConnection.baseUrl));
			onProjectDisableOrDelete();
			return;
		}

		setInitialState();

		// Set up our server consoles
		consoles = MicroclimateConsoleFactory.createApplicationConsoles(app);

		try {
			// Right now, the project will always be in run mode. In the future, we will have to detect which mode
			// it is already in, so we can connect the debugger if required.
			getServer().getLaunchConfiguration(true, null).launch(ILaunchManager.RUN_MODE, null);
		} catch (CoreException e) {
			MCLogger.logError("Error doing initial launch", e);
		}
	}

	private void onInitializeFailure(String failMsg) {
		String msg = NLS.bind(Messages.MicroclimateServerBehaviour_ErrCreatingServerDialogMsg, failMsg);
		MCLogger.logError(msg);
		setServerState(IServer.STATE_UNKNOWN);
		MCUtil.openDialog(true, Messages.MicroclimateServerBehaviour_ErrCreatingServerDialogTitle, msg);
	}

	@Override
	public IStatus canStop() {
		// TODO when FW supports this
		return new Status(IStatus.ERROR, MicroclimateCorePlugin.PLUGIN_ID, 0, "Not yet supported", null); //$NON-NLS-1$
	}

	@Override
	public void stop(boolean force) {
		setServerState(IServer.STATE_STOPPED);
		// TODO when FW supports this
	}

	@Override
	public IStatus canPublish() {
		return new Status(IStatus.ERROR, MicroclimateCorePlugin.PLUGIN_ID, 0,
				Messages.MicroclimateServerBehaviour_ServerDoesntSupportPublish, null);
	}

	@Override
	public void dispose() {
		MCLogger.log("Dispose " + getServer().getName()); //$NON-NLS-1$

		if (ILaunchManager.DEBUG_MODE.equals(getServer().getMode()) && getServer().getServerState() == IServer.STATE_STARTED) {
			MicroclimateApplication app = getApp();
			MCUtil.openDialog(false,
					NLS.bind(Messages.MicroclimateServerBehaviour_DeletingServerModeSwitchTitle, getServer().getName()),
					Messages.MicroclimateServerBehaviour_DeletingServerModeSwitchMsg);
			try {
				app.mcConnection.requestProjectRestart(app, ILaunchManager.RUN_MODE);
			} catch (Exception e) {
				MCLogger.logError("Restart in run mode for the " + app.name + " application failed.", e); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		DebugPlugin.getDefault().getLaunchManager().removeLaunch(getServer().getLaunch());

		// required to stop the auto publish thread
		setServerState(IServer.STATE_STOPPED);

		if (consoles != null) {
			for (IOConsole console : consoles) {
				console.destroy();
			}
		}
	}

	public boolean isStarted() {
		return getServer().getServerState() == IServer.STATE_STARTED;
	}

	public MicroclimateApplication getApp() {
		return app;
	}

	private void setInitialState() {
		try {
			JSONObject projectStatus = app.mcConnection.requestProjectStatus(app);

			if (projectStatus == null) {
				onProjectDisableOrDelete();
				return;
			}

			// Check if the project is open
			if (projectStatus.has(MCConstants.KEY_OPEN_STATE) &&
					projectStatus.getString(MCConstants.KEY_OPEN_STATE)
						.equals(MCConstants.VALUE_STATE_CLOSED)) {

				// Project has been disabled
				onProjectDisableOrDelete();
			}
			else {
				// Project is running
				// Pass this off to the regular server state updater
				updateServerState(projectStatus);
			}
		}
		catch(IOException | JSONException e) {
			MCLogger.logError("Error setting project initial state", e); //$NON-NLS-1$
			MCUtil.openDialog(true,
					Messages.MicroclimateServerBehaviour_ErrSettingInitialStateDialogTitle,
					e.getMessage());
			setServerState(IServer.STATE_UNKNOWN);
		}
	}

	public void updateServerState(JSONObject projectChangedEvent)
			throws JSONException {

		clearSuffix();
		String appStatus = ""; //$NON-NLS-1$
		if (projectChangedEvent.has(MCConstants.KEY_APP_STATUS)) {
			appStatus = projectChangedEvent.getString(MCConstants.KEY_APP_STATUS);
			onAppStateUpdate(appStatus);
		}

		// Update build status if the project is not started or starting.
		if (!appStatus.equals(AppState.STARTED.appState) && !appStatus.equals(AppState.STARTING.appState) &&
				projectChangedEvent.has(MCConstants.KEY_BUILD_STATUS)) {

			String detail = ""; //$NON-NLS-1$
			if (projectChangedEvent.has(MCConstants.KEY_DETAILED_BUILD_STATUS)) {
				detail = projectChangedEvent.getString(MCConstants.KEY_DETAILED_BUILD_STATUS);
			}

			onBuildStateUpdate(projectChangedEvent.getString(MCConstants.KEY_BUILD_STATUS), detail);
		}
	}

	private void onAppStateUpdate(String appStatus) {
		int state = AppState.convert(appStatus);
		if (state != getServer().getServerState()) {
			MCLogger.log("Update state of " + getServer().getName() + " to " + appStatus); //$NON-NLS-1$ //$NON-NLS-2$
			setServerState(state);
		}
	}

	private void onBuildStateUpdate(String buildStatus, String buildStatusDetail) {
		MCLogger.log(getServer().getName() +
				" has build status " + buildStatus + 	//$NON-NLS-1$
				", detail " + buildStatusDetail); 		//$NON-NLS-1$

		final String status = BuildStatus.toUserFriendly(buildStatus, buildStatusDetail);
		setSuffix(status, IServer.STATE_STOPPED, false);
	}

	public void onProjectDisableOrDelete() {
		setSuffix(Messages.MicroclimateServerBehaviour_ProjectMissingServerSuffix, IServer.STATE_STOPPED, true);
	}

	public void onMicroclimateDisconnect(String microclimateUrl) {
		setSuffix(
				NLS.bind(Messages.MicroclimateServerBehaviour_ConnectionLostServerSuffix, microclimateUrl),
				IServer.STATE_UNKNOWN, true);
	}

	public void onMicroclimateReconnect() {
		clearSuffix();
		setInitialState();
	}

	/**
	 * The 'stopped reason' is appended to the Server name in the Servers view, if it is set.
	 * It is meant to hold an error message, or a 'building' state, etc.
	 * It is intended to be used if the server is not started because it is waiting for something,
	 * or because of an error.
	 */
	private synchronized void setSuffix(String suffix, int newServerState, boolean isError) {
		// MCLogger.log("SetSuffix for server " + getServer().getName() + " to " + suffix);
		this.suffix = suffix;
		this.isErrored = isError;

		forceRefreshServerDecorator(newServerState);
	}

	/**
	 * Make sure to set the server state after calling this, by either setInitialState or updateServerState.
	 */
	private synchronized void clearSuffix() {
		suffix = null;
		isErrored = false;
		// This state will be overwritten immediately after.
		forceRefreshServerDecorator(IServer.STATE_STOPPED);
	}

	public synchronized String getSuffix() {
		return suffix;
	}

	public synchronized boolean isErrored() {
		return isErrored;
	}

	/**
	 * Hack to refresh the server decorator by forcing a state change -
	 * change to a state different from the desired one, then set the desired state after.
	 */
	private void forceRefreshServerDecorator(int newServerState) {
		if (newServerState == IServer.STATE_UNKNOWN) {
			setServerState(IServer.STATE_STOPPED);
		}
		else {
			setServerState(IServer.STATE_UNKNOWN);
		}
		setServerState(newServerState);
	}

	@Override
	public void restart(String launchMode) throws CoreException {
		if (!isStarted()) {
			MCLogger.logError("Cannot restart because server is not in started state");
			MCUtil.openDialog(true,
					Messages.MicroclimateServerBehaviour_CantRestartDialogTitle,
					Messages.MicroclimateServerBehaviour_CantRestartDialogMsg);
			return;
        }

		MCLogger.log(String.format("Restarting %s in %s mode", getServer().getHost(), launchMode)); //$NON-NLS-1$

		int currentState = getServer().getServerState();
		MCLogger.log("Current status = " + AppState.convert(currentState)); //$NON-NLS-1$

		try {
			app.mcConnection.requestProjectRestart(app, launchMode);
		} catch (JSONException | IOException e) {
			MCLogger.logError("Error initiating project restart", e); //$NON-NLS-1$
			MCUtil.openDialog(true,
					Messages.MicroclimateServerBehaviour_ErrInitiatingRestartDialogTitle,
					e.getMessage());
			return;
		}

		// Restarts are only valid if the server is Started. So, we go from Started -> Stopped -> Starting,
		// then connect the debugger if required (Liberty won't leave 'Starting' state until the debugger is connected),
		// then go from Starting -> Started.
		// TODO: progress monitor
		waitForState(getStopTimeoutMs(), null, IServer.STATE_STOPPED, IServer.STATE_STARTING);

		ILaunchConfiguration launchConfig = getServer().getLaunchConfiguration(true, null);
		if (launchConfig == null) {
			MCLogger.logError("LaunchConfig was null!"); //$NON-NLS-1$
			return;
		}

		launchConfig.launch(launchMode, null);
	}

	/**
	 * Set the server into Debug or Run mode, and attach the debugger if necessary.
	 */
	public void doLaunch(ILaunchConfiguration launchConfig, String launchMode, ILaunch launch,
			IProgressMonitor monitor) {

		if (ILaunchManager.DEBUG_MODE.equals(launchMode)) {
			boolean starting = waitForState(getStartTimeoutMs(), monitor, IServer.STATE_STARTING);
			if (!starting) {
				// TODO I haven't seen this happen, but we should probably display something to the user in this case.
				// What could cause this to happen?
				MCLogger.logError("Server did not enter Starting state"); //$NON-NLS-1$
				return;
			}

			MCLogger.log("Preparing for debug mode"); //$NON-NLS-1$
			try {
				IDebugTarget debugTarget = MicroclimateDebugConnector.connectDebugger(this, launch, monitor);
				if (debugTarget != null) {
					setMode(ILaunchManager.DEBUG_MODE);
					MCLogger.log("Debugger connect success. Server should go into Debugging state soon."); //$NON-NLS-1$
				}
				else {
					MCLogger.logError("Debugger connect failure"); //$NON-NLS-1$

					MCUtil.openDialog(true,
							Messages.MicroclimateServerBehaviour_DebuggerConnectFailureDialogTitle,
							Messages.MicroclimateServerBehaviour_DebuggerConnectFailureDialogMsg);
				}
			} catch (IllegalConnectorArgumentsException | CoreException | IOException e) {
				MCLogger.logError(e);

			}
		} else {
			setMode(ILaunchManager.RUN_MODE);
		}
	}

	public void reconnectDebug(IProgressMonitor monitor) {
		ILaunchConfiguration launchConfig;
		try {
			launchConfig = getServer().getLaunchConfiguration(true, null);
			if (launchConfig == null) {
				MCLogger.logError("LaunchConfig was null!"); //$NON-NLS-1$
				return;
			}
			launchConfig.launch(ILaunchManager.DEBUG_MODE, null);
		} catch (CoreException e) {
			MCLogger.logError("Launch config is null", e); //$NON-NLS-1$
		}
	}

	/**
	 * Wait for the server to enter the given state(s). The server state is updated by the MonitorThread.
	 *
	 * @return true if the server entered the state in time, false otherwise.
	 */
	private boolean waitForState(int timeoutMs, IProgressMonitor monitor, int... desiredStates) {
		final long startTime = System.currentTimeMillis();
		final int pollRateMs = 1000;

		// This is just for logging
		String desiredStatesStr = ""; //$NON-NLS-1$
		if (desiredStates.length == 0) {
			MCLogger.logError("No states passed to waitForState"); //$NON-NLS-1$
			return false;
		}
		desiredStatesStr = AppState.convert(desiredStates[0]);
		for (int i = 1; i < desiredStates.length; i++) {
			desiredStatesStr += " or " + AppState.convert(desiredStates[i]); //$NON-NLS-1$
		}
		// End logging-only

		while((System.currentTimeMillis() - startTime) < timeoutMs) {
			if (monitor != null && monitor.isCanceled()) {
				MCLogger.log("User cancelled waiting for server to be " + desiredStatesStr); //$NON-NLS-1$
				break;
			}

			try {
				MCLogger.log(String.format("Waiting for %s to be: %s, is currently %s", 		//$NON-NLS-1$
						getServer().getName(), desiredStatesStr, AppState.convert(getServer().getServerState())));

				Thread.sleep(pollRateMs);
			}
			catch(InterruptedException e) {
				MCLogger.logError(e);
			}

			// the ServerMonitorThread will update the state, so we just have to check it.
			for (int desiredState : desiredStates) {
				if (getServer().getServerState() == desiredState) {
					MCLogger.log("Server is done switching to " + AppState.convert(desiredState)); //$NON-NLS-1$
					return true;
				}
			}
		}

		MCLogger.logError("Server did not enter state(s): " + desiredStatesStr + " in " + timeoutMs + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return false;
	}

	public int getStartTimeoutMs() {
		return getServer().getStartTimeout() * 1000;
	}

	public int getStopTimeoutMs() {
		return getServer().getStopTimeout() * 1000;
	}

}
