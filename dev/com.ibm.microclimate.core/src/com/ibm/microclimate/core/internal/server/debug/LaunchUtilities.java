/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core.internal.server.debug;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdi.Bootstrap;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.debug.core.JDIDebugPlugin;
import org.eclipse.jdt.launching.JavaRuntime;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;

/**
 * Taken largely from com.ibm.ws.st.core.internal.launch.LaunchUtilities
 * and com.ibm.ws.st.core.internal.launch.BaseLibertyLaunchConfiguration
 *
 * Static utilities to support launching and debugging Microclimate application servers.
 *
 * @author timetchells@ibm.com
 *
 */
@SuppressWarnings("restriction")
public class LaunchUtilities {

	private LaunchUtilities() { }

    /**
     * Returns the default 'com.sun.jdi.SocketAttach' connector.
     *
     * @return the {@link AttachingConnector}
     */
	public static AttachingConnector getAttachingConnector() {
        List<?> connectors = Bootstrap.virtualMachineManager().attachingConnectors();
        for (int i = 0; i < connectors.size(); i++) {
            AttachingConnector c = (AttachingConnector) connectors.get(i);
            if ("com.sun.jdi.SocketAttach".equals(c.name())) { //$NON-NLS-1$
				return c;
			}
        }

        return null;
    }

	/**
	 * @return argsToConfigure updated with the new arguments.
	 */
    public static Map<String, Connector.Argument> configureConnector(
    		Map<String, Connector.Argument> argsToConfigure, String host, int portNumber) {

        Connector.StringArgument hostArg = (Connector.StringArgument) argsToConfigure.get("hostname"); //$NON-NLS-1$
        hostArg.setValue(host);

        Connector.IntegerArgument portArg = (Connector.IntegerArgument) argsToConfigure.get("port"); //$NON-NLS-1$
        portArg.setValue(portNumber);

        Connector.IntegerArgument timeoutArg = (Connector.IntegerArgument) argsToConfigure.get("timeout"); //$NON-NLS-1$
        if (timeoutArg != null) {
            int timeout = Platform.getPreferencesService().getInt(
                                                                  "org.eclipse.jdt.launching", //$NON-NLS-1$
                                                                  JavaRuntime.PREF_CONNECT_TIMEOUT,
                                                                  JavaRuntime.DEF_CONNECT_TIMEOUT,
                                                                  null);
            timeoutArg.setValue(timeout);
        }

        return argsToConfigure;
    }

    /**
     * Creates a new debug target for the given virtual machine and system process
     * that is connected on the specified port for the given launch.
     *
     * @param launch launch to add the target to
     * @param port port the VM is connected to
     * @param process associated system process - Can be null if stdout/err don't need to be hooked up.
     * @param vm JDI virtual machine
     * @return the {@link IDebugTarget}
     */
    public static IDebugTarget createLocalJDTDebugTarget(ILaunch launch, int port, IProcess process, VirtualMachine vm,
    		String name, boolean allowTerminate) {

    	return JDIDebugModel.newDebugTarget(launch, vm, name, process, allowTerminate, true, true);
    }


    /**
     * Set the debug request timeout of a vm in ms, if supported by the vm implementation.
     */
    public static void setDebugTimeout(VirtualMachine vm) {
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
}
