package com.ibm.microclimate.core.internal.server.debug;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IServer;

import com.ibm.microclimate.core.MicroclimateCorePlugin;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.Messages;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.server.MicroclimateServerBehaviour;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;

@SuppressWarnings("restriction")
public class MicroclimateDebugConnector {
	/**
	 * From com.ibm.ws.st.core.internal.launch.BaseLibertyLaunchConfiguration.connectAndWait
	 */
    public static IDebugTarget connectDebugger(MicroclimateServerBehaviour server, ILaunch launch, IProgressMonitor monitor)
    		throws IllegalConnectorArgumentsException, CoreException, IOException {

    	MCLogger.log("Beginning to try to connect debugger"); //$NON-NLS-1$

		// Here, we have to wait for the projectRestartResult event
		// since its payload tells us the debug port to use
		int debugPort = waitForDebugPort(server.getApp(), server.getStartTimeoutMs());

    	if (debugPort == -1) {
    		MCLogger.logError("Couldn't get debug port for MC Server, or it was never set"); //$NON-NLS-1$
    		return null;
    	}

    	MCLogger.log("Debugging on port " + debugPort); //$NON-NLS-1$

		int timeout = MicroclimateCorePlugin.getDefault().getPreferenceStore()
				.getInt(MicroclimateCorePlugin.DEBUG_CONNECT_TIMEOUT_PREFSKEY)
				* 1000;
		MCLogger.log("Debugger connect timeout is " + timeout + "ms"); //$NON-NLS-1$ //$NON-NLS-2$

		// Now prepare the Debug Connector, and try to attach it to the server's JVM
		AttachingConnector connector = LaunchUtilities.getAttachingConnector();
		if (connector == null) {
			MCLogger.logError("Could not create debug connector"); //$NON-NLS-1$
		}

		Map<String, Connector.Argument> connectorArgs = connector.defaultArguments();
        connectorArgs = LaunchUtilities.configureConnector(connectorArgs, server.getServer().getHost(), debugPort);

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
						MCLogger.log("User cancelled debugger connecting"); //$NON-NLS-1$
						return null;
					}
					try {
						vm = connector.attach(connectorArgs);
						itr = 0;
						ex = null;
					} catch (Exception e) {
						ex = e;
						if (itr % 8 == 0) {
							MCLogger.log("Waiting for debugger attach."); //$NON-NLS-1$
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
					LaunchUtilities.setDebugTimeout(vm);

					// This appears in the Debug view
					final String debugName = getDebugLaunchName(server.getServer(), debugPort);

					debugTarget = LaunchUtilities
							.createLocalJDTDebugTarget(launch, debugPort, null, vm, debugName, false);

					monitor.worked(1);
					monitor.done();
				}
				return debugTarget;
			} catch (InterruptedIOException e) {
				// timeout, consult status handler if there is one
				IStatus status = new Status(IStatus.ERROR, MicroclimateCorePlugin.PLUGIN_ID,
						IJavaLaunchConfigurationConstants.ERR_VM_CONNECT_TIMEOUT, "", e); //$NON-NLS-1$

				IStatusHandler handler = DebugPlugin.getDefault().getStatusHandler(status);

				retry = false;
				if (handler == null) {
					// if there is no handler, throw the exception
					throw new CoreException(status);
				}

				Object result = handler.handleStatus(status, server);
				if (result instanceof Boolean) {
					retry = ((Boolean) result).booleanValue();
				}
			}
		} while (retry);
		return null;
	}

	private static String getDebugLaunchName(IServer server, int debugPort) {
		return NLS.bind(Messages.MicroclimateServerBehaviour_DebugLaunchConfigName,
				new Object[] {
						server.getName(),
						server.getHost(),
						debugPort
				});
	}

    /**
     * The debug port is set by FileWatcher emitting a 'projectRestartResult' event (see MicroclimateSocket class).
     * Make sure to call app.invalidatePorts() when the app is restarted, otherwise this might return an outdated port.
     *
     * @return The debug port, or -1 if waiting for the restart event times out.
     */
	private static int waitForDebugPort(MicroclimateApplication app, int timeoutMs) {
		final long startTime = System.currentTimeMillis();

		while (System.currentTimeMillis() < (startTime + timeoutMs)) {
			MCLogger.log("Waiting for restart success socket event to set debug port"); //$NON-NLS-1$
			try {
				Thread.sleep(2500);
			} catch (InterruptedException e) {
				MCLogger.logError(e);
			}

			// Make sure that app.invalidatePorts() was called when it needed to be,
			// otherwise this might return old info
			int port = app.getDebugPort();
			if (port != -1) {
				MCLogger.log("Debug port was retrieved successfully: " + port); //$NON-NLS-1$
				return port;
			}
		}
		MCLogger.logError("Timed out waiting for restart success"); //$NON-NLS-1$
		return -1;
	}
}
