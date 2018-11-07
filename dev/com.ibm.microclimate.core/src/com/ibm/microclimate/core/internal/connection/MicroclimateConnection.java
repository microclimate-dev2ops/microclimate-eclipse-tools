/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core.internal.connection;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ibm.microclimate.core.internal.HttpUtil;
import com.ibm.microclimate.core.internal.HttpUtil.HttpResult;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.MicroclimateApplicationFactory;
import com.ibm.microclimate.core.internal.constants.MCConstants;
import com.ibm.microclimate.core.internal.constants.ProjectType;
import com.ibm.microclimate.core.internal.messages.Messages;

/**
 * Represents a connection to a Microclimate instance
 */
public class MicroclimateConnection {

	public static final String MICROCLIMATE_WORKSPACE_PROPERTY = "com.ibm.microclimate.internal.workspace"; //$NON-NLS-1$
	private static final String UNKNOWN_VERSION = "unknown"; //$NON-NLS-1$

	public final URI baseUrl;
	public final IPath localWorkspacePath;
	public final int mcVersion;

	public final MicroclimateSocket mcSocket;

	private Map<String, MicroclimateApplication> appMap = new LinkedHashMap<String, MicroclimateApplication>();

	public static URI buildUrl(String host, int port) throws URISyntaxException {
		return new URI("http", null, host, port, null, null, null); //$NON-NLS-1$
	}

	public MicroclimateConnection (URI uri) throws IOException, URISyntaxException, JSONException {
		if (!uri.toString().endsWith("/")) { //$NON-NLS-1$
			uri = uri.resolve("/"); //$NON-NLS-1$
		}
		this.baseUrl = uri;

		if (MicroclimateConnectionManager.getActiveConnection(uri.toString()) != null) {
			onInitFail(NLS.bind(Messages.MicroclimateConnection_ErrConnection_AlreadyExists, baseUrl));
		}

		// Must set host, port fields before doing this
		mcSocket = new MicroclimateSocket(this);
		if(!mcSocket.blockUntilFirstConnection()) {
			close();
			throw new MicroclimateConnectionException(mcSocket.socketUri);
		}

		JSONObject env = getEnvData(this.baseUrl);

		String version = getMCVersion(env);

		if (UNKNOWN_VERSION.equals(version)) {
			onInitFail(NLS.bind(Messages.MicroclimateConnection_ErrConnection_VersionUnknown,
					MCConstants.REQUIRED_MC_VERSION));
		}
		else if (!isSupportedVersion(version)) {
			onInitFail(NLS.bind(Messages.MicroclimateConnection_ErrConnection_OldVersion,
					version, MCConstants.REQUIRED_MC_VERSION));
		}

		if (MCConstants.VERSION_LATEST.equals(version)) {
			// There's not much we can do here but assume everything is supported.
			this.mcVersion = Integer.MAX_VALUE;
			MCLogger.log("Can't determine Microclimate version from dev build");			// $NON-NLS-1$
		}
		else {
			// isSupportedVersion already tried to parse this as an int, so we know this will succeed
			this.mcVersion = Integer.parseInt(version);
		}
		MCLogger.log("Microclimate version is " + mcVersion);			// $NON-NLS-1$

		this.localWorkspacePath = getWorkspacePath(env);
		if (localWorkspacePath == null) {
			// Can't recover from this
			// This should never happen since we have already determined it is a supported version of microclimate.
			onInitFail(Messages.MicroclimateConnection_ErrConnection_WorkspaceErr);
		}

		refreshApps(null);

		MCLogger.log("Created " + this); //$NON-NLS-1$
	}

	private void onInitFail(String msg) throws ConnectException {
		MCLogger.log("Initializing MicroclimateConnection failed: " + msg); //$NON-NLS-1$
		close();
		throw new ConnectException(msg);
	}

	/**
	 * Call this when the connection is removed.
	 */
	public void close() {
		MCLogger.log("Closing " + this); //$NON-NLS-1$
		if (mcSocket != null && mcSocket.socket != null) {
			if (mcSocket.socket.connected()) {
				mcSocket.socket.disconnect();
			}
			mcSocket.socket.close();
		}
		for (MicroclimateApplication app : appMap.values()) {
			app.dispose();
		}
	}

	private static JSONObject getEnvData(URI baseUrl) throws JSONException, IOException {
		final URI envUrl = baseUrl.resolve(MCConstants.APIPATH_ENV);

		String envResponse = null;
		try {
			envResponse = HttpUtil.get(envUrl).response;
		} catch (IOException e) {
			MCLogger.logError("Error contacting Environment endpoint", e); //$NON-NLS-1$
			MCUtil.openDialog(true, Messages.MicroclimateConnection_ErrContactingServerDialogTitle, Messages.MicroclimateConnection_ErrContactingServerDialogMsg + envUrl);
			throw e;
		}

		return new JSONObject(envResponse);
	}

	private static String getMCVersion(JSONObject env) {
		if (!env.has(MCConstants.KEY_ENV_MC_VERSION)) {
			MCLogger.logError("Missing version from env data"); //$NON-NLS-1$
			return UNKNOWN_VERSION;
		}

		try {
			String versionStr = env.getString(MCConstants.KEY_ENV_MC_VERSION);
			MCLogger.log("Microclimate Version is: " + versionStr); //$NON-NLS-1$
			return versionStr;
		} catch (JSONException e) {
			// we already checked for this key so this will not happen.
			MCLogger.logError(e);
			return UNKNOWN_VERSION;
		}
	}

	private static boolean isSupportedVersion(String versionStr) {
		if (UNKNOWN_VERSION.equals(versionStr)) {
			return false;
		}

		if (MCConstants.VERSION_LATEST.equals(versionStr)) {
			// Development build - possible other values to check for?
			return true;
		}

		// The version will have a format like '1809', which corresponds to v18.09
		try {
			int version = Integer.parseInt(versionStr);
			return version >= MCConstants.REQUIRED_MC_VERSION;
		}
		catch(NumberFormatException e) {
			MCLogger.logError("Couldn't parse version number from " + versionStr); //$NON-NLS-1$
			return false;
		}
	}

	private static Path getWorkspacePath(JSONObject env) throws JSONException {
		// Try the internal system property first
		String path = System.getProperty(MICROCLIMATE_WORKSPACE_PROPERTY, null);
		if (path != null && !path.isEmpty()) {
			return new Path(path);
		}

		if (!env.has(MCConstants.KEY_ENV_WORKSPACE_LOC)) {
			MCLogger.logError("Missing workspace location from env data"); //$NON-NLS-1$
			return null;
		}
		String workspaceLoc = env.getString(MCConstants.KEY_ENV_WORKSPACE_LOC);
		if (MCUtil.isWindows() && workspaceLoc.startsWith("/")) { //$NON-NLS-1$
			String device = workspaceLoc.substring(1, 2);
			workspaceLoc = device + ":" + workspaceLoc.substring(2); //$NON-NLS-1$
		}
		return new Path(workspaceLoc);
	}

	/**
	 * Refresh this connection's apps using the Microclimate project list endpoint.
	 * If projectID is not null then only refresh the corresponding application.
	 */
	public void refreshApps(String projectID) {

		final URI projectsURL = baseUrl.resolve(MCConstants.APIPATH_PROJECT_LIST);

		String projectsResponse = null;
		try {
			projectsResponse = HttpUtil.get(projectsURL).response;
		} catch (IOException e) {
			MCLogger.logError("Error contacting Projects endpoint", e);
			MCUtil.openDialog(true, Messages.MicroclimateConnection_ErrContactingServerDialogTitle,
					NLS.bind(Messages.MicroclimateConnection_ErrContactingServerDialogMsg, projectsURL));
			return;
		}

		try {
			MicroclimateApplicationFactory.getAppsFromProjectsJson(this, projectsResponse, projectID);
			MCLogger.log("App list update success"); //$NON-NLS-1$
		}
		catch(Exception e) {
			MCUtil.openDialog(true, Messages.MicroclimateConnection_ErrGettingProjectListTitle, e.getMessage());
		}
	}
	
	public void addApp(MicroclimateApplication app) {
		synchronized(appMap) {
			appMap.put(app.projectID, app);
		}
	}

	public List<MicroclimateApplication> getApps() {
		synchronized(appMap) {
			return new ArrayList<MicroclimateApplication>(appMap.values());
		}
	}

	public MicroclimateApplication removeApp(String projectID) {
		synchronized(appMap) {
			return appMap.remove(projectID);
		}
	}

	/**
	 * @return The app with the given ID, if it exists in this Microclimate instance, else null.
	 */
	public synchronized MicroclimateApplication getAppByID(String projectID) {
		MicroclimateApplication app = null;
		synchronized(appMap) {
			app = appMap.get(projectID);
		}
		return app;
	}

	public MicroclimateApplication getAppByName(String name) {
		synchronized(appMap) {
			for (MicroclimateApplication app : getApps()) {
				if (app.name.equals(name)) {
					return app;
				}
			}
		}
		MCLogger.log("No application found for name " + name); //$NON-NLS-1$
		return null;
	}

	public void requestProjectRestart(MicroclimateApplication app, String launchMode)
			throws JSONException, IOException {

		String restartEndpoint = MCConstants.APIPATH_PROJECT_LIST + "/" 	//$NON-NLS-1$
				+ app.projectID + "/" 										//$NON-NLS-1$
				+ MCConstants.APIPATH_RESTART;

        URI url = baseUrl.resolve(restartEndpoint);

		JSONObject restartProjectPayload = new JSONObject();
		restartProjectPayload.put(MCConstants.KEY_START_MODE, launchMode);

		// This initiates the restart
		HttpResult result = HttpUtil.post(url, restartProjectPayload);
		if (!result.isGoodResponse) {
			final String msg = String.format("Received bad response from server %d with error message %s", //$NON-NLS-1$
					result.responseCode, result.error);
			throw new IOException(msg);
		}
		app.invalidatePorts();
	}

	/**
	 * Get the project status endpoint, and filter the response for the JSON corresponding to the given project.
	 * @return
	 * 	The JSON containing the status info for the given project,
	 * 	or null if the project is not found in the status info.
	 */
	public JSONObject requestProjectStatus(MicroclimateApplication app) throws IOException, JSONException {
		final URI statusUrl = baseUrl.resolve(MCConstants.APIPATH_PROJECT_LIST);

		HttpResult result = HttpUtil.get(statusUrl);

		if (!result.isGoodResponse) {
			final String msg = String.format("Received bad response from server %d with error message %s", //$NON-NLS-1$
					result.responseCode, result.error);
			throw new IOException(msg);
		}
		else if (result.response == null) {
			// I don't think this will ever happen.
			throw new IOException("Server returned good response code, but null response when getting initial state"); //$NON-NLS-1$
		}

		JSONArray allProjectStatuses = new JSONArray(result.response);
		for (int i = 0; i < allProjectStatuses.length(); i++) {
			JSONObject projectStatus = allProjectStatuses.getJSONObject(i);
			if (projectStatus.getString(MCConstants.KEY_PROJECT_ID).equals(app.projectID)) {
				// Success - found the project of interest
				return projectStatus;
			}
		}

		MCLogger.log("Didn't find status info for project " + app.name); //$NON-NLS-1$
		return null;
	}

	/**
	 * Request a build on an application
	 * @param app The app to build
	 */
	public void requestProjectBuild(MicroclimateApplication app)
			throws JSONException, IOException {

		String buildEndpoint = MCConstants.APIPATH_PROJECT_LIST + "/" 	//$NON-NLS-1$
				+ app.projectID + "/" 									//$NON-NLS-1$
				+ MCConstants.APIPATH_BUILD;

		URI url = baseUrl.resolve(buildEndpoint);

		JSONObject buildPayload = new JSONObject();
		buildPayload.put(MCConstants.KEY_ACTION, MCConstants.VALUE_ACTION_BUILD);

		// This initiates the build
		HttpUtil.post(url, buildPayload);
	}


	/**
	 * Called by the MicroclimateSocket when the socket.io connection goes down.
	 */
	public synchronized void onConnectionError() {
		MCLogger.log("MCConnection to " + baseUrl + " lost"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Called by the MicroclimateSocket when the socket.io connection is working.
	 */
	public synchronized void clearConnectionError() {
		MCLogger.log("MCConnection to " + baseUrl + " restored"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public String toString() {
		return String.format("%s @ baseUrl=%s workspacePath=%s numApps=%d", //$NON-NLS-1$
				MicroclimateConnection.class.getSimpleName(), baseUrl, localWorkspacePath, appMap.size());
	}

	// Note that toPrefsString and fromPrefsString are used to save and load connections from the preferences store
	// in MicroclimateConnectionManager, so be careful modifying these.

	public String toPrefsString() {
		// No newlines allowed!
		return baseUrl.toString();
	}
	
	public void requestProjectCreate(ProjectType type, String name)
			throws JSONException, IOException {
		switch(type) {
		case LIBERTY:
			requestMicroprofileProjectCreate(name);
			break;
		case SPRING:
			requestSpringProjectCreate(name);
			break;
		default:
			MCLogger.log("Creation of projects with type " + type + " is not supported.");
		}	
	}

	public void requestMicroprofileProjectCreate(String name)
			throws JSONException, IOException {

		String createEndpoint = MCConstants.APIPATH_PROJECT_LIST;

        URI url = baseUrl.resolve(createEndpoint);

		JSONObject createProjectPayload = new JSONObject();
		createProjectPayload.put(MCConstants.KEY_NAME, name);
		createProjectPayload.put(MCConstants.KEY_LANGUAGE, "java");
		createProjectPayload.put(MCConstants.KEY_FRAMEWORK, "microprofile");
		createProjectPayload.put(MCConstants.KEY_CONTEXTROOT, name);

		HttpUtil.post(url, createProjectPayload);
	}
	
	public void requestSpringProjectCreate(String name)
			throws JSONException, IOException {

		String createEndpoint = MCConstants.APIPATH_PROJECT_LIST;

        URI url = baseUrl.resolve(createEndpoint);

		JSONObject createProjectPayload = new JSONObject();
		createProjectPayload.put(MCConstants.KEY_NAME, name);
		createProjectPayload.put(MCConstants.KEY_LANGUAGE, "java");
		createProjectPayload.put(MCConstants.KEY_FRAMEWORK, "spring");

		HttpUtil.post(url, createProjectPayload);
	}

	public void requestProjectDelete(String projectId)
			throws JSONException, IOException {

		String createEndpoint = MCConstants.APIPATH_PROJECT_LIST + "/" + projectId;

        URI url = baseUrl.resolve(createEndpoint);

		HttpUtil.delete(url);
	}

	public IPath getWorkspacePath() {
		return localWorkspacePath;
	}

	public boolean supportsProjectType(ProjectType type) {
		return type == ProjectType.LIBERTY ||
				(type == ProjectType.SPRING && mcVersion >= MCConstants.SPRING_MC_VERSION);
	}
}
