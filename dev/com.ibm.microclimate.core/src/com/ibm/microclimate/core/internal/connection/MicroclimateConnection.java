/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.core.internal.connection;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.ibm.microclimate.core.internal.console.ProjectLogInfo;
import com.ibm.microclimate.core.internal.console.ProjectTemplateInfo;
import com.ibm.microclimate.core.internal.constants.MCConstants;
import com.ibm.microclimate.core.internal.constants.ProjectType;
import com.ibm.microclimate.core.internal.messages.Messages;

/**
 * Represents a connection to a Microclimate instance
 */
public class MicroclimateConnection {

	public static final String MICROCLIMATE_WORKSPACE_PROPERTY = "com.ibm.microclimate.internal.workspace"; //$NON-NLS-1$
	private static final String UNKNOWN_VERSION = "unknown"; //$NON-NLS-1$
	private static final String BRANCH_VERSION = "\\d{4}_M\\d{1,2}_\\D";
	private static final Pattern pattern = Pattern.compile(BRANCH_VERSION);

	public final URI baseUrl;
	private IPath localWorkspacePath;
	private String versionStr;
	private String connectionErrorMsg = null;
	private String socketNamespace = null;

	private MicroclimateSocket mcSocket;
	
	private volatile boolean isConnected = true;

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
		
		JSONObject env = getEnvData(this.baseUrl);

		this.versionStr = getMCVersion(env);

		if (UNKNOWN_VERSION.equals(versionStr)) {
			onInitFail(NLS.bind(Messages.MicroclimateConnection_ErrConnection_VersionUnknown,
					MCConstants.REQUIRED_MC_VERSION));
		} else if (!isSupportedVersion(versionStr)) {
			onInitFail(NLS.bind(Messages.MicroclimateConnection_ErrConnection_OldVersion,
					versionStr, MCConstants.REQUIRED_MC_VERSION));
		}

		MCLogger.log("Microclimate version is: " + versionStr);			// $NON-NLS-1$

		this.localWorkspacePath = getWorkspacePath(env);
		if (localWorkspacePath == null) {
			// Can't recover from this
			// This should never happen since we have already determined it is a supported version of microclimate.
			onInitFail(Messages.MicroclimateConnection_ErrConnection_WorkspaceErr);
		}
		
		this.socketNamespace = getSocketNamespace(env);
		
		mcSocket = new MicroclimateSocket(this);
		if(!mcSocket.blockUntilFirstConnection()) {
			close();
			throw new MicroclimateConnectionException(mcSocket.socketUri);
		}

		refreshApps(null);

		MCLogger.log("Created " + this); //$NON-NLS-1$
	}
	
	public String getSocketNamespace() {
		return socketNamespace;
	}
	
	public MicroclimateSocket getMCSocket() {
		return mcSocket;
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
		if (mcSocket != null) {
			mcSocket.close();
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
		
		Matcher matcher = pattern.matcher(versionStr);
		if (matcher.matches()) {
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
	
	public boolean checkVersion(int requiredVersion, String requiredVersionBr) {
		if (UNKNOWN_VERSION.equals(versionStr)) {
			return false;
		}
		
		if (MCConstants.VERSION_LATEST.equals(versionStr)) {
			// Development build - possible other values to check for?
			return true;
		}
		
		Matcher matcher = pattern.matcher(versionStr);
		if (matcher.matches()) {
			String actualYear = versionStr.substring(0, 4);
			String requiredYear = requiredVersionBr.substring(0, 4);
			try {
				if (Integer.parseInt(actualYear) >= (Integer.parseInt(requiredYear))) {
					int index = versionStr.lastIndexOf('_');
					String actualIteration = versionStr.substring(6, index);
					index = requiredVersionBr.lastIndexOf('_');
					String requiredIteration = requiredVersionBr.substring(6, index);
					if (Integer.parseInt(actualIteration) >= Integer.parseInt(requiredIteration)) {
						return true;
					}
				}
			} catch (NumberFormatException e) {
				MCLogger.logError("Failed to parse the actual version: " + versionStr + ", or the required version: " + requiredVersionBr);
			}
			return false;
		}
		
		try {
			return Integer.parseInt(versionStr) >= requiredVersion;
		} catch(NumberFormatException e) {
			MCLogger.logError("Couldn't parse version number from " + versionStr); //$NON-NLS-1$
		}
		
		return false;
	}
	
	public String getConnectionErrorMsg() {
		return this.connectionErrorMsg;
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
	
	private static String getSocketNamespace(JSONObject env) throws JSONException {
		if (env.has(MCConstants.KEY_ENV_MC_SOCKET_NAMESPACE)) {
			Object nsObj = env.get(MCConstants.KEY_ENV_MC_SOCKET_NAMESPACE);
			if (nsObj instanceof String) {
				String namespace = (String)nsObj;
				if (!namespace.isEmpty()) {
					return namespace;
				}
			}
		}
		return null;
	}

	/**
	 * Refresh this connection's apps using the Microclimate project list endpoint.
	 * If projectID is not null then only refresh the corresponding application.
	 */
	public void refreshApps(String projectID) {

		final URI projectsURL = baseUrl.resolve(MCConstants.APIPATH_PROJECT_LIST);

		try {
			String projectsResponse = HttpUtil.get(projectsURL).response;
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
	
	public Set<String> getAppIds() {
		synchronized(appMap) {
			return new HashSet<String>(appMap.keySet());
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
		synchronized(appMap) {
			return appMap.get(projectID);
		}
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
	
	public void requestProjectOpenClose(MicroclimateApplication app, boolean enable)
			throws JSONException, IOException {
		
		String action = enable ? MCConstants.APIPATH_OPEN : MCConstants.APIPATH_CLOSE;

		String restartEndpoint = MCConstants.APIPATH_PROJECT_LIST + "/" 	//$NON-NLS-1$
				+ app.projectID + "/" 										//$NON-NLS-1$
				+ action;

		URI url = baseUrl.resolve(restartEndpoint);

		// This initiates the restart
		HttpResult result = HttpUtil.put(url);
		if (!result.isGoodResponse) {
			final String msg = String.format("Received bad response from server %d with error message %s", //$NON-NLS-1$
					result.responseCode, result.error);
			throw new IOException(msg);
		}
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
	
	public JSONObject requestProjectMetricsStatus(MicroclimateApplication app) throws IOException, JSONException {
		if (!app.mcConnection.checkVersion(1905, "2019_M5_E")) {
			return null;
		}
		String endpoint = MCConstants.APIPATH_PROJECT_LIST + "/" 	//$NON-NLS-1$
				+ app.projectID + "/" 								//$NON-NLS-1$
				+ MCConstants.APIPATH_METRICS_STATUS;

		URI uri = baseUrl.resolve(endpoint);
		HttpResult result = HttpUtil.get(uri);
		checkResult(result, uri, true);
		return new JSONObject(result.response);
	}

	/**
	 * Request a build on an application
	 * @param app The app to build
	 */
	public void requestProjectBuild(MicroclimateApplication app, String action)
			throws JSONException, IOException {

		String buildEndpoint = MCConstants.APIPATH_PROJECT_LIST + "/" 	//$NON-NLS-1$
				+ app.projectID + "/" 									//$NON-NLS-1$
				+ MCConstants.APIPATH_BUILD;

		URI url = baseUrl.resolve(buildEndpoint);

		JSONObject buildPayload = new JSONObject();
		buildPayload.put(MCConstants.KEY_ACTION, action);

		// This initiates the build
		HttpUtil.post(url, buildPayload);
	}
	
	public List<ProjectLogInfo> requestProjectLogs(MicroclimateApplication app) throws JSONException, IOException {
		List<ProjectLogInfo> logList = new ArrayList<ProjectLogInfo>();
		if (!app.mcConnection.checkVersion(1905, "2019_M5_E")) {
			return logList;
		}
		
		String endpoint = MCConstants.APIPATH_PROJECT_LIST + "/"	//$NON-NLS-1$
				+ app.projectID + "/"								//$NON-NLS-1$
				+ MCConstants.APIPATH_LOGS;
		
		URI uri = baseUrl.resolve(endpoint);
		HttpResult result = HttpUtil.get(uri);
		checkResult(result, uri, true);
        
		JSONObject logs = new JSONObject(result.response);
		JSONArray buildLogs = logs.getJSONArray(MCConstants.KEY_LOG_BUILD);
		logList.addAll(getLogs(buildLogs, MCConstants.KEY_LOG_BUILD));
		JSONArray appLogs = logs.getJSONArray(MCConstants.KEY_LOG_APP);
		logList.addAll(getLogs(appLogs, MCConstants.KEY_LOG_APP));
		return logList;
	}
	
	private List<ProjectLogInfo> getLogs(JSONArray logs, String type) throws JSONException {
		List<ProjectLogInfo> logList = new ArrayList<ProjectLogInfo>();
		if (logs != null) {
			for (int i = 0; i < logs.length(); i++) {
				JSONObject log = logs.getJSONObject(i);
				if (log.has(MCConstants.KEY_LOG_NAME)) {
					String logName = log.getString(MCConstants.KEY_LOG_NAME);
					String workspacePath = null;
					if (log.has(MCConstants.KEY_LOG_WORKSPACE_PATH)) {
						workspacePath = log.getString(MCConstants.KEY_LOG_WORKSPACE_PATH);
					}
					ProjectLogInfo logInfo = new ProjectLogInfo(type, logName, workspacePath);
					logList.add(logInfo);
				} else {
					MCLogger.log("An item in the log list does not have the key: " + MCConstants.KEY_LOG_NAME);
				}
			}
		}
		return logList;
	}
	
	public void requestEnableLogStream(MicroclimateApplication app, ProjectLogInfo logInfo) throws IOException {
		String endpoint = MCConstants.APIPATH_PROJECT_LIST + "/" 	//$NON-NLS-1$
				+ app.projectID + "/" 								//$NON-NLS-1$
				+ MCConstants.APIPATH_LOGS + "/"					//$NON-NLS-1$
				+ logInfo.type + "/"								//$NON-NLS-1$
				+ logInfo.logName;
		
		URI uri = baseUrl.resolve(endpoint);
		HttpResult result = HttpUtil.post(uri);
        checkResult(result, uri, false);
	}
	
	public void requestDisableLogStream(MicroclimateApplication app, ProjectLogInfo logInfo) throws IOException {
		String endpoint = MCConstants.APIPATH_PROJECT_LIST + "/" 	//$NON-NLS-1$
				+ app.projectID + "/" 								//$NON-NLS-1$
				+ MCConstants.APIPATH_LOGS + "/"					//$NON-NLS-1$
				+ logInfo.type + "/"								//$NON-NLS-1$
				+ logInfo.logName;
		
		URI uri = baseUrl.resolve(endpoint);
		HttpResult result = HttpUtil.delete(uri);
        checkResult(result, uri, false);
	}
	
	public void requestValidate(MicroclimateApplication app) throws JSONException, IOException {
		boolean projectIdInPath = checkVersion(1901, "2019_M1_E");
		
		String endpoint;
		if (projectIdInPath) {
			endpoint = MCConstants.APIPATH_PROJECT_LIST + "/"	//$NON-NLS-1$
					+ app.projectID + "/"	//$NON-NLS-1$
					+ MCConstants.APIPATH_VALIDATE;
		} else {
			endpoint = MCConstants.APIPATH_BASE	+ "/"	//$NON-NLS-1$
					+ MCConstants.APIPATH_VALIDATE;
					
		}
		
		URI url = baseUrl.resolve(endpoint);
		
		JSONObject buildPayload = new JSONObject();
		if (!projectIdInPath) {
			buildPayload.put(MCConstants.KEY_PROJECT_ID, app.projectID);
		}
		buildPayload.put(MCConstants.KEY_PROJECT_TYPE, app.projectType.type);
		
		HttpResult result = HttpUtil.post(url, buildPayload);
		if (!result.isGoodResponse) {
			final String msg = String.format("Received bad response from server %d with error message %s", //$NON-NLS-1$
					result.responseCode, result.error);
			throw new IOException(msg);
		}
	}
	
	public void requestValidateGenerate(MicroclimateApplication app) throws JSONException, IOException {
		boolean projectIdInPath = checkVersion(1901, "2019_M1_E");
		
		String endpoint;
		if (projectIdInPath) {
			endpoint = MCConstants.APIPATH_PROJECT_LIST + "/"	//$NON-NLS-1$
					+ app.projectID + "/"	//$NON-NLS-1$
					+ MCConstants.APIPATH_VALIDATE_GENERATE;
		} else {
			endpoint = MCConstants.APIPATH_BASE	+ "/"	//$NON-NLS-1$
					+ MCConstants.APIPATH_VALIDATE_GENERATE;
					
		}
		
		URI url = baseUrl.resolve(endpoint);
		
		JSONObject buildPayload = new JSONObject();
		if (!projectIdInPath) {
			buildPayload.put(MCConstants.KEY_PROJECT_ID, app.projectID);
		}
		buildPayload.put(MCConstants.KEY_PROJECT_TYPE, app.projectType.type);
		buildPayload.put(MCConstants.KEY_AUTO_GENERATE, true);
		
		HttpResult result = HttpUtil.post(url, buildPayload);
		if (!result.isGoodResponse) {
			final String msg = String.format("Received bad response from server %d with error message %s", //$NON-NLS-1$
					result.responseCode, result.error);
			throw new IOException(msg);
		}
		
		// Perform validation again to clear the errors/warnings that have been fixed
		requestValidate(app);
	}
	
	public JSONObject requestProjectCapabilities(MicroclimateApplication app) throws IOException, JSONException {
		final URI statusUrl = baseUrl.resolve(MCConstants.APIPATH_PROJECT_LIST + "/" + app.projectID + "/" + MCConstants.APIPATH_CAPABILITIES);

		HttpResult result = HttpUtil.get(statusUrl);

		if (!result.isGoodResponse) {
			final String msg = String.format("Received bad response from server %d with error message %s", //$NON-NLS-1$
					result.responseCode, result.error);
			throw new IOException(msg);
		} else if (result.response == null) {
			// I don't think this will ever happen.
			throw new IOException("Server returned good response code, but empty content when getting project capabilities"); //$NON-NLS-1$
		}

		JSONObject capabilities = new JSONObject(result.response);
		return capabilities;
	}
	
	public List<ProjectTemplateInfo> requestProjectTemplates() throws IOException, JSONException {
		List<ProjectTemplateInfo> templates = new ArrayList<ProjectTemplateInfo>();
		final URI uri = baseUrl.resolve(MCConstants.APIPATH_BASEV2 + "/" + MCConstants.APIPATH_PROJECT_TYPES);
		HttpResult result = HttpUtil.get(uri);
		checkResult(result, uri, true);
		
		JSONArray templateArray = new JSONArray(result.response);
		for (int i = 0; i < templateArray.length(); i++) {
			templates.add(new ProjectTemplateInfo(templateArray.getJSONObject(i)));
		}
		
		return templates;
	}
	
	public void requestProjectCreate(ProjectTemplateInfo templateInfo, String name)
			throws JSONException, IOException {
		
		// Special case node.js projects which don't have a template
		if (ProjectType.LANGUAGE_NODEJS.equals(templateInfo.getLanguage())) {
			requestNodeProjectCreate(name);
			return;
		}

		String endpoint = MCConstants.APIPATH_BASEV2 + "/" + MCConstants.APIPATH_PROJECTS;

		URI uri = baseUrl.resolve(endpoint);

		JSONObject createProjectPayload = new JSONObject();
		createProjectPayload.put(MCConstants.KEY_NAME, name);
		createProjectPayload.put(MCConstants.KEY_EXTENSION, templateInfo.getExtension());

		HttpResult result = HttpUtil.post(uri, createProjectPayload);
		checkResult(result, uri, false);
	}
	
	private void checkResult(HttpResult result, URI uri, boolean checkContent) throws IOException {
		if (!result.isGoodResponse) {
			final String msg = String.format("Received bad response code %d for uri %s with error message %s", //$NON-NLS-1$
					result.responseCode, uri, result.error);
			throw new IOException(msg);
		} else if (checkContent && result.response == null) {
			// I don't think this will ever happen.
			throw new IOException("Server returned good response code, but the content of the result is null for uri: " + uri); //$NON-NLS-1$
		}
	}
	
	public boolean isConnected() {
		return isConnected;
	}

	/**
	 * Called by the MicroclimateSocket when the socket.io connection goes down.
	 */
	public synchronized void onConnectionError() {
		MCLogger.log("MCConnection to " + baseUrl + " lost"); //$NON-NLS-1$ //$NON-NLS-2$
		isConnected = false;
		synchronized(appMap) {
			appMap.clear();
		}
		MCUtil.updateConnection(this);
	}

	/**
	 * Called by the MicroclimateSocket when the socket.io connection is working.
	 */
	public synchronized void clearConnectionError() {
		MCLogger.log("MCConnection to " + baseUrl + " restored"); //$NON-NLS-1$ //$NON-NLS-2$
		
		// Reset any cached information in case it has changed
		try {
			JSONObject envData = getEnvData(baseUrl);
			String version = getMCVersion(envData);
			if (UNKNOWN_VERSION.equals(versionStr)) {
				MCLogger.logError("Failed to get the Microclimate version after reconnect");
				this.connectionErrorMsg = NLS.bind(Messages.MicroclimateConnection_ErrConnection_VersionUnknown, MCConstants.REQUIRED_MC_VERSION);
				MCUtil.updateConnection(this);
				return;
			}
			if (!isSupportedVersion(version)) {
				MCLogger.logError("The detected version of Microclimate after reconnect is not supported: " + version);
				this.connectionErrorMsg = NLS.bind(Messages.MicroclimateConnection_ErrConnection_OldVersion, versionStr, MCConstants.REQUIRED_MC_VERSION);
				MCUtil.updateConnection(this);
				return;
			}
			this.versionStr = version;
			IPath path = getWorkspacePath(envData);
			if (path == null) {
				// This should not happen since the version was ok
				MCLogger.logError("Failed to get the local workspace path after reconnect");
				this.connectionErrorMsg = Messages.MicroclimateConnection_ErrConnection_WorkspaceErr;
				MCUtil.updateConnection(this);
				return;
			}
			this.localWorkspacePath = path;
			
			String socketNS = getSocketNamespace(envData);
			if ((socketNS != null && !socketNS.equals(this.socketNamespace)) || (this.socketNamespace != null && !this.socketNamespace.equals(socketNS))) {
				// The socket namespace has changed so need to recreate the socket
				this.socketNamespace = socketNS;
				mcSocket.close();
				mcSocket = new MicroclimateSocket(this);
				if(!mcSocket.blockUntilFirstConnection()) {
					// Still not connected
					MCLogger.logError("Failed to create a new socket with updated URI: " + mcSocket.socketUri);
					// Clear the message so that it just shows the basic disconnected message
					this.connectionErrorMsg = null;
					return;
				}
			}
		} catch (Exception e) {
			MCLogger.logError("An exception occurred while trying to update the connection information", e);
			this.connectionErrorMsg = Messages.MicroclimateConnection_ErrConnection_UpdateCacheException;
			MCUtil.updateConnection(this);
			return;
		}
		
		this.connectionErrorMsg = null;
		isConnected = true;
		refreshApps(null);
		MCUtil.updateConnection(this);
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
		if (type.isType(ProjectType.TYPE_LIBERTY)) {
			requestMicroprofileProjectCreate(name);
		} else if (type.isType(ProjectType.TYPE_SPRING)) {
			requestSpringProjectCreate(name);
		} else if (type.isType(ProjectType.TYPE_NODEJS)) {
			requestNodeProjectCreate(name);
		} else {
			MCLogger.log("Creation of projects with type " + type + " is not supported.");  //$NON-NLS-1$ //$NON-NLS-2$
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
	
	public void requestNodeProjectCreate(String name)
			throws JSONException, IOException {

		String createEndpoint = MCConstants.APIPATH_PROJECT_LIST;

		URI uri = baseUrl.resolve(createEndpoint);

		JSONObject createProjectPayload = new JSONObject();
		createProjectPayload.put(MCConstants.KEY_NAME, name);
		createProjectPayload.put(MCConstants.KEY_LANGUAGE, "nodejs");

		HttpResult result = HttpUtil.post(uri, createProjectPayload);
		checkResult(result, uri, false);
	}

	public void requestProjectDelete(String projectId)
			throws JSONException, IOException {

		String endpoint = MCConstants.APIPATH_PROJECT_LIST + "/" + projectId;

		URI uri = baseUrl.resolve(endpoint);

		HttpResult result = HttpUtil.delete(uri);
		checkResult(result, uri, false);
	}

	public IPath getWorkspacePath() {
		return localWorkspacePath;
	}
	
	public URI getNewProjectURI() {
		return getProjectURI(MCConstants.QUERY_NEW_PROJECT);
	}
	
	public URI getImportProjectURI() {
		return getProjectURI(MCConstants.QUERY_IMPORT_PROJECT);
	}
	
	private URI getProjectURI(String projectQuery) {
		try {
			URI uri = baseUrl;
			String query = projectQuery + "=" + MCConstants.VALUE_TRUE;
			uri = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), query, uri.getFragment());
			return uri;
		} catch (Exception e) {
			MCLogger.logError("Failed to get the project URI for the query: " + projectQuery, e);  //$NON-NLS-1$
		}
		return null;
	}
	
	public URL getAppMonitorURL(MicroclimateApplication app) {
		return getAppViewURL(app, MCConstants.VIEW_MONITOR);
	}
	
	public URL getAppOverviewURL(MicroclimateApplication app) {
		return getAppViewURL(app, MCConstants.VIEW_OVERVIEW);
	}
	
	public URL getAppViewURL(MicroclimateApplication app, String view) {
		try {
			URI uri = baseUrl;
			String query = MCConstants.QUERY_PROJECT + "=" + app.projectID;
			query = query + "&" + MCConstants.QUERY_VIEW + "=" + view;
			uri = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), query, uri.getFragment());
			return uri.toURL();
		} catch (Exception e) {
			MCLogger.logError("Failed to get the URL for the " + view + " view and the " + app.name + "application.", e);  //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$ 
		}
		return null;
	}
}
