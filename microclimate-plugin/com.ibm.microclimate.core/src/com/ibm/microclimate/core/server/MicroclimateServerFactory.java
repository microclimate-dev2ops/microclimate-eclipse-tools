package com.ibm.microclimate.core.server;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;

import com.ibm.microclimate.core.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;

/**
 *
 * @author timetchells@ibm.com
 */
public class MicroclimateServerFactory {

	/**
	 * Creates a MicroclimateServer from the given MicroclimateApplication and returns it.
	 */
	public static IServer create(MicroclimateApplication app) throws CoreException {
		final String serverName = getServerNameFor(app);

		IServerType mcServerType = null;

		for (IServerType type : ServerCore.getServerTypes()) {
			if (MicroclimateServer.SERVER_ID.equals(type.getId())) {
				mcServerType = type;
			}
		}

		if (mcServerType == null) {
			MCLogger.logError("Didn't find MC Server Type!");
			throw new CoreException(new Status(IStatus.ERROR, com.ibm.microclimate.core.Activator.PLUGIN_ID,
					"Missing Server Type: " + MicroclimateServer.SERVER_ID));
		}

		IServerWorkingCopy newServerWc = mcServerType.createServer(null, null, null, null);		// TODO progress mon?
		newServerWc.setHost(app.host);
		newServerWc.setName(serverName);

		// We can't pass Objects to the server attribute framework - only primitives
		// so we provide the info needed for the Server to look up the relevant Microclimate app
		newServerWc.setAttribute(MicroclimateServer.ATTR_APP_URL, app.rootUrl.toString());
		newServerWc.setAttribute(MicroclimateServer.ATTR_PROJ_ID, app.projectID);

		// The server will determine the corresponding MCConnection from the baseUrl
		newServerWc.setAttribute(MicroclimateServer.ATTR_MCC_URL, app.mcConnection.baseUrl);

		// Store the name of the Eclipse project linked to this server. This will be used by the
		// launch configuration to locate the source code when debugging
		newServerWc.setAttribute(MicroclimateServer.ATTR_ECLIPSE_PROJECT_NAME, app.name);

		// Creates the server in the Servers view
		return newServerWc.saveAll(true, null);
	}

	private static String getServerNameFor(MicroclimateApplication app) {
		return "Microclimate Application: " + app.name;
	}
}
