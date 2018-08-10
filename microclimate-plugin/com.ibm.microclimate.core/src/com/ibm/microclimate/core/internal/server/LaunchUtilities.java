package com.ibm.microclimate.core.internal.server;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jdi.Bootstrap;
import org.eclipse.jdt.launching.JavaRuntime;

import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;

// From com.ibm.ws.st.core.internal.launch.LaunchUtilities
public class LaunchUtilities {
    /**
     * Returns the default 'com.sun.jdi.SocketAttach' connector.
     *
     * @return the {@link AttachingConnector}
     */
    public static AttachingConnector getAttachingConnector() {
        List<?> connectors = Bootstrap.virtualMachineManager().attachingConnectors();
        for (int i = 0; i < connectors.size(); i++) {
            AttachingConnector c = (AttachingConnector) connectors.get(i);
            if ("com.sun.jdi.SocketAttach".equals(c.name())) {
				return c;
			}
        }

        return null;
    }

    public static Map<String, Connector.Argument> configureConnector(
    		Map<String, Connector.Argument> argsToConfigure, String host, int portNumber) {

        Connector.StringArgument hostArg = (Connector.StringArgument) argsToConfigure.get("hostname");
        hostArg.setValue(host);

        Connector.IntegerArgument portArg = (Connector.IntegerArgument) argsToConfigure.get("port");
        portArg.setValue(portNumber);

        Connector.IntegerArgument timeoutArg = (Connector.IntegerArgument) argsToConfigure.get("timeout");
        if (timeoutArg != null) {
            int timeout = Platform.getPreferencesService().getInt(
                                                                  "org.eclipse.jdt.launching",
                                                                  JavaRuntime.PREF_CONNECT_TIMEOUT,
                                                                  JavaRuntime.DEF_CONNECT_TIMEOUT,
                                                                  null);
            timeoutArg.setValue(timeout);
        }

        return argsToConfigure;
    }
}
