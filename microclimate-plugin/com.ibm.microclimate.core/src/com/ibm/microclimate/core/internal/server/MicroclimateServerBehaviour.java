package com.ibm.microclimate.core.internal.server;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.debug.core.JDIDebugPlugin;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerPort;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.json.JSONException;
import org.json.JSONObject;

import com.ibm.microclimate.core.Activator;
import com.ibm.microclimate.core.MCLogger;
import com.ibm.microclimate.core.internal.HttpUtil;
import com.ibm.microclimate.core.internal.HttpUtil.HttpResult;
import com.ibm.microclimate.core.internal.MCSocket;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.MicroclimateConnection;
import com.ibm.microclimate.core.internal.MicroclimateConnectionManager;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;

@SuppressWarnings("restriction")
public class MicroclimateServerBehaviour extends ServerBehaviourDelegate {

	private MicroclimateApplication app;
	private MicroclimateServerMonitorThread monitorThread;

	@Override
	public void initialize(IProgressMonitor monitor) {
		// TODO investigate initialize being called on DISPOSE - then a second (useless) monitor thread is leaked!

		MCLogger.log("Initializing MicroclimateServerBehaviour for " + getServer().getName());

		String projectID = getServer().getAttribute(MicroclimateServer.ATTR_PROJ_ID, "");
		if (projectID.isEmpty()) {
			onInitializeFailure("No " + MicroclimateServer.ATTR_PROJ_ID + " attribute");
			return;
		}

		String mcConnectionBaseUrl = getServer().getAttribute(MicroclimateServer.ATTR_MCC_URL, "");
		if (mcConnectionBaseUrl.isEmpty()) {
			onInitializeFailure("No " + MicroclimateServer.ATTR_MCC_URL + " attribute");
			return;
		}
		MicroclimateConnection mcConnection = MicroclimateConnectionManager.getConnection(mcConnectionBaseUrl);
		if (mcConnection == null) {
			onInitializeFailure("Couldn't get MCConnection with baseUrl " + mcConnectionBaseUrl);
			return;
		}

		app = mcConnection.getAppByID(projectID);
		if (app == null) {
			onInitializeFailure("Couldn't find app with ID " + projectID + " on MCConnection "
					+ mcConnection.toString());
			return;
		}

		// Ask the server for an initial state - the monitor thread will handle updates but doesn't know the state
		// until it changes (causing the server to emit an update event)
		monitorThread = new MicroclimateServerMonitorThread(this);
		monitorThread.setInitialState(getInitialState());
		monitorThread.start();
	}

	private void onInitializeFailure(String failMsg) {
		MCLogger.logError("Creating Microclimate server failed at initialization: " + failMsg);

		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.openError(Display.getDefault().getActiveShell(),
						"Error creating Microclimate server", failMsg);
			}
		});
	}

	private int getInitialState() {
		// TODO hardcoded filewatcher URL - should be getting this from Portal using mcConnection.baseUrl
		String statusUrl = "http://localhost:9091/api/v1/projects/status/?type=appState&projectID=%s";
		statusUrl = String.format(statusUrl, app.projectID);

		HttpResult result;
		try {
			result = HttpUtil.get(statusUrl);
		} catch (IOException e) {
			MCLogger.logError(e);
			return IServer.STATE_UNKNOWN;
		}

		if (!result.isGoodResponse) {
			MCLogger.logError("Received bad response from server: " + result.responseCode);
			MCLogger.logError("Error message: " + result.error);
			return IServer.STATE_UNKNOWN;
		}
		else if (result.response == null) {
			MCLogger.logError("Good response code, but null response when getting initial state");
			return IServer.STATE_UNKNOWN;
		}

		try {
			JSONObject appStateJso = new JSONObject(result.response);
			if (appStateJso.has(MCSocket.KEY_APP_STATUS)) {
				String status = appStateJso.getString(MCSocket.KEY_APP_STATUS);
				MCLogger.log("Initial server state is " + status);

				// MCLogger.log("Update app state to " + status);
				return MicroclimateServerBehaviour.appStatusToServerState(status);
			}
		} catch (JSONException e) {
			MCLogger.logError("JSON exception when getting initial state", e);
		}
		return IServer.STATE_UNKNOWN;
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
		monitorThread.interrupt();
		monitorThread = null;
	}

	/**
	 * Wrapper for protected setServerState, to be called by the monitor thread
	 */
	void setServerState_(int serverState) {
		if (getServer().getServerState() != serverState) {
			MCLogger.log("Updating state of " + getServer().getName() + " to " + serverStateToAppStatus(serverState));
			setServerState(serverState);
		}
	}

	MicroclimateApplication getApp() {
		return app;
	}

	@Override
	public void restart(String launchMode) throws CoreException {
		MCLogger.log("Restarting " + getServer().getHost() + " in " + launchMode + " mode");

		int currentState = monitorThread.getLastKnownState();
		MCLogger.log("Current status = " + serverStateToAppStatus(currentState));

		if (IServer.STATE_STARTING == currentState) {
			waitForStarted(null);
			// throw new CoreException "Wait for Started before restarting?"
		}

		ILaunchConfiguration launchConfig = getServer().getLaunchConfiguration(true, null);
		if (launchConfig == null) {
			MCLogger.logError("LaunchConfig was null!");
			return;
		}

		// TODO progress mon
		launchConfig.launch(launchMode, null);
	}

	/**
	 * Request the MC server to restart in the given mode. Then wait for it to stop and start again.
	 * @param launchMode
	 * @param monitor
	 */
	void doRestart(ILaunchConfiguration launchConfig, String launchMode, ILaunch launch, IProgressMonitor monitor) {
        String url = "http://localhost:9091/api/v1/projects/action";

		JSONObject restartProjectPayload = new JSONObject();
		try {
			restartProjectPayload
					.put("action", "restart")
					.put("mode", launchMode)
					.put("projectID", app.projectID);
		} catch (JSONException e) {
			MCLogger.logError(e);
			return;
		}

		try {
			// returns an operationID if success - what to do with it?
			HttpUtil.post(url, restartProjectPayload);
		} catch (IOException e) {
			MCLogger.logError("Error POSTing restart request", e);
			return;
		}
		// MCLogger.log(response);

		// It takes a moment for the project to update. We don't want to be too quick and
		// see that the state is 'started', when it actually hasn't 'stopped' yet.
		// TODO here we should actually wait for the server to projectStatusChanged - new status 'stopped' - event
		waitForState(getStopTimeoutMs(), monitor, IServer.STATE_STOPPED, IServer.STATE_STARTING);

		MCLogger.log("Server should be stopped");

		boolean starting = waitForState(getStartTimeoutMs(), monitor, IServer.STATE_STARTING);
		if (!starting) {
			MCLogger.logError("Server did not enter Starting state");
			return;
		}

		if (ILaunchManager.DEBUG_MODE.equals(launchMode)) {
			MCLogger.log("Attaching debugger");
			try {
				attachDebugger(launch, monitor);
			} catch (IllegalConnectorArgumentsException | CoreException | IOException e) {
				MCLogger.logError(e);
			}
		}

		waitForStarted(monitor);
		MCLogger.log("Server is done restarting");
	}

	private boolean waitForStarted(IProgressMonitor monitor) {
		return waitForState(getStartTimeoutMs(), monitor, IServer.STATE_STARTED);
	}

	private boolean waitForState(int timeoutMs, IProgressMonitor monitor, int... desiredStates) {
		long startTime = System.currentTimeMillis();
		int pollRateMs = 1000;

		String desiredStatesStr = "";
		if (desiredStates.length == 0) {
			MCLogger.logError("No states passed to waitForState");
			return false;
		}
		desiredStatesStr = serverStateToAppStatus(desiredStates[0]);
		for (int i = 1; i < desiredStates.length; i++) {
			desiredStatesStr += " or " + serverStateToAppStatus(desiredStates[i]);
		}

		while((System.currentTimeMillis() - startTime) < timeoutMs) {
			if (monitor != null && monitor.isCanceled()) {
				MCLogger.log("User cancelled waiting for server to be " + desiredStatesStr);
				break;
			}

			try {
				MCLogger.log("Waiting for server to be: " + desiredStatesStr + ", is currently " +
						serverStateToAppStatus(getServer().getServerState()));

				Thread.sleep(pollRateMs);
			}
			catch(InterruptedException e) {
				MCLogger.logError(e);
			}

			// the ServerMonitorThread will update the state
			for (int desiredState : desiredStates) {
				if (getServer().getServerState() == desiredState) {
					MCLogger.log("Server is done switching to " + serverStateToAppStatus(desiredState));
					return true;
				}
			}

		}

		MCLogger.logError("Server did not enter state(s): " + desiredStatesStr + " in " + timeoutMs + "ms");
		setServerState(IServer.STATE_UNKNOWN);
		return false;
	}

	private void attachDebugger(ILaunch launch, IProgressMonitor monitor) throws IllegalConnectorArgumentsException, CoreException, IOException {
        IDebugTarget debugTarget;
        debugTarget = connectAndWait(launch,  monitor);

        if (debugTarget != null) {
            IJavaDebugTarget jdt = debugTarget.getAdapter(IJavaDebugTarget.class);
            MCLogger.log(jdt.getName());
        }
	}

	// from com.ibm.ws.st.core.internal.launch.BaseLibertyLaunchConfiguration
    private IDebugTarget connectAndWait(ILaunch launch, IProgressMonitor monitor)
    		throws IllegalConnectorArgumentsException, CoreException, IOException {

    	/*
		int port = LaunchUtilities.findFreePort();
		if (port == -1) {
			throw new IOException("Couldn't get a free port to connect to the debugger");
		}*/
    	int debugPort = -1;
    	ServerPort[] ports = getServer().getServerPorts(monitor);
    	for (ServerPort port : ports) {
    		if (port.getName().toLowerCase().contains("debug")) {
    			debugPort = port.getPort();
    		}
    	}

    	if (debugPort == -1) {
    		MCLogger.logError("Couldn't get debug port for MC Server, or it was never set");
    		return null;
    	}

    	MCLogger.log("Debugging on port " + debugPort);

		int timeout = getStartTimeoutMs();

		AttachingConnector connector = LaunchUtilities.getAttachingConnector();
		if (connector == null) {
			MCLogger.logError("Could not create debug connector");
		}

		Map<String, Connector.Argument> connectorArgs = connector.defaultArguments();
        connectorArgs = LaunchUtilities.configureConnector(connectorArgs, getServer().getHost(), debugPort);

		boolean retry = false;
		do {
			try {
				VirtualMachine vm = null;
				Exception ex = null;
				int itr = timeout * 4; // We want to check 4 times per second

				if (itr <= 0) {
					itr = 2;
				}

				while (itr-- > 0) {
					if (monitor.isCanceled()) {
						MCLogger.log("User cancelled debugger connecting");
						return null;
					}
					try {
						vm = connector.attach(connectorArgs);
						itr = 0;
						ex = null;
					} catch (Exception e) {
						ex = e;
						if (itr % 8 == 0) {
							MCLogger.log("Waiting for debugger attach.");
						}
					}
					try {
						Thread.sleep(250);
					} catch (InterruptedException e1) {
						// do nothing
					}
				}


				if (ex instanceof IllegalConnectorArgumentsException) {
					throw (IllegalConnectorArgumentsException) ex;
				}

				if (ex instanceof InterruptedIOException) {
					throw (InterruptedIOException) ex;
				}

				if (ex instanceof IOException) {
					throw (IOException) ex;
				}

				IDebugTarget debugTarget = null;
				if (vm != null) {
					setDebugTimeout(vm);
					debugTarget = createLocalJDTDebugTarget(launch, debugPort, null, vm);
					monitor.worked(1);
					monitor.done();
				}
				return debugTarget;
			} catch (InterruptedIOException e) {
				// timeout, consult status handler if there is one
				IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, IJavaLaunchConfigurationConstants.ERR_VM_CONNECT_TIMEOUT, "", e);
				IStatusHandler handler = DebugPlugin.getDefault().getStatusHandler(status);

				retry = false;
				if (handler == null) {
					// if there is no handler, throw the exception
					throw new CoreException(status);
				}

				Object result = handler.handleStatus(status, this);
				if (result instanceof Boolean) {
					retry = ((Boolean) result).booleanValue();
				}
			}
		} while (retry);
		return null;
	}

    /**
     * Creates a new debug target for the given virtual machine and system process
     * that is connected on the specified port for the given launch.
     *
     * @param launch launch to add the target to
     * @param port port the VM is connected to
     * @param process associated system process
     * @param vm JDI virtual machine
     * @return the {@link IDebugTarget}
     */
    private IDebugTarget createLocalJDTDebugTarget(ILaunch launch, int port, IProcess process, VirtualMachine vm) {
        String name = "Debugging " + getServer().getName() + " at " + "localhost:" + port;

        return JDIDebugModel.newDebugTarget(launch, vm, name, process, true, false, true);
    }


    /**
     * Set the debug request timeout of a vm in ms, if supported by the vm implementation.
     */
    private static void setDebugTimeout(VirtualMachine vm) {
        IEclipsePreferences node = InstanceScope.INSTANCE.getNode(JDIDebugPlugin.getUniqueIdentifier());
        int timeOut = node.getInt(JDIDebugModel.PREF_REQUEST_TIMEOUT, 0);
        if (timeOut <= 0) {
			return;
		}
        if (vm instanceof org.eclipse.jdi.VirtualMachine) {
            org.eclipse.jdi.VirtualMachine vm2 = (org.eclipse.jdi.VirtualMachine) vm;
            vm2.setRequestTimeout(timeOut);
        }
    }

	int getStartTimeoutMs() {
		return getServer().getStartTimeout() * 1000;
	}

	int getStopTimeoutMs() {
		return getServer().getStopTimeout() * 1000;
	}

	/**
	 * Convert a Microclimate App State string into the corresponding IServer.STATE constant.
	 */
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

	/**
	 * Convert an IServer.STATE constant into the corresponding Microclimate App State string.
	 */
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
