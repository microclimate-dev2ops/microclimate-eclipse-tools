/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.core.internal.remote;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ibm.microclimate.core.internal.HttpUtil;
import com.ibm.microclimate.core.internal.HttpUtil.HttpResult;
import com.ibm.microclimate.core.internal.MCLogger;

public class APIUtils {
	
	protected static final int DEFAULT_TIMEOUT = 5000;
	
	protected static final String LOCALHOST = "127.0.0.1";
	protected static final String API_KEY_PROP = "X-API-Key";
	protected static final String GET_PING_REST_PATH = "/rest/system/ping";
	protected static final String POST_SHUTDOWN_REST_PATH = "/rest/system/shutdown";
	protected static final String CONFIG_REST_PATH = "/rest/system/config";
	protected static final String STATUS_REST_PATH = "/rest/system/status";
	protected static final String SCAN_REST_PATH = "/rest/db/scan";
	protected static final String EVENT_REST_PATH = "/rest/events";
	
	private static final String FOLDER_PARAM = "folder";
	private static final String SINCE_PARAM = "since";
	private static final String EVENTS_PARAM = "events";
	private static final String TIMEOUT_PARAM = "timeout";
	private static final String LIMIT_PARAM = "limit";
	
	protected static final String STATUS_ID_KEY = "myID";
	
	public static String getDeviceId(URI baseUri, String apiKey) throws IOException, JSONException {
		final URI uri = baseUri.resolve(STATUS_REST_PATH);
		HttpResult result = HttpUtil.get(uri, null, getRequestProperties(apiKey), null, DEFAULT_TIMEOUT);
		checkResult(uri, result, true);
		JSONObject status = new JSONObject(result.response);
		return status.getString(STATUS_ID_KEY);
	}
	
	public static JSONObject getConfig(URI baseUri, String apiKey) throws IOException, JSONException {
		final URI uri = baseUri.resolve(CONFIG_REST_PATH);
		HttpResult result = HttpUtil.get(uri, null, getRequestProperties(apiKey), null, DEFAULT_TIMEOUT);
		checkResult(uri, result, true);
		return new JSONObject(result.response);
	}
	
	public static void putConfig(URI baseUri, String apiKey, JSONObject config) throws IOException {
		final URI uri = baseUri.resolve(CONFIG_REST_PATH);
		HttpResult result = HttpUtil.post(uri, null, getRequestProperties(apiKey), config);
		checkResult(uri, result);
	}
	
	public static void shutdown(URI baseUri, String apiKey) throws IOException {
		final URI uri = baseUri.resolve(POST_SHUTDOWN_REST_PATH);
		HttpResult result = HttpUtil.post(uri, null, getRequestProperties(apiKey), new JSONObject());
		checkResult(uri, result);
	}
	
	public static boolean ping(URI baseUri, String apiKey, int connectTimeout) {
		final URI uri = baseUri.resolve(GET_PING_REST_PATH);
		try {
			HttpResult result = HttpUtil.get(uri, null, getRequestProperties(apiKey), null, connectTimeout);
			checkResult(uri, result);
		} catch (IOException e) {
			MCLogger.log("Ping failed for connection: " + baseUri + ", with error: " + e.getMessage());
			return false;
		}

		return true;
	}
	
	public static void rescan(URI baseUri, String apiKey, String folder) throws IOException {
		URI uri = baseUri.resolve(SCAN_REST_PATH);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(FOLDER_PARAM, folder);
		HttpResult result = HttpUtil.post(uri, null, getRequestProperties(apiKey), params);
		checkResult(uri, result);
	}
	
	public static JSONArray getEvents(URI baseUri, String apiKey, int sinceId, Set<String> types, int timeout) throws IOException, JSONException {
		return getEvents(baseUri, apiKey, sinceId, types, -1, timeout);
	}
	
	public static JSONArray getEvents(URI baseUri, String apiKey, int sinceId, Set<String> types, int limit, int timeout) throws IOException, JSONException {
		URI uri = baseUri.resolve(EVENT_REST_PATH);
		
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(SINCE_PARAM, String.valueOf(sinceId));
		if (types != null) {
			params.put(EVENTS_PARAM, getTypeList(types));
		}
		if (limit > 0) {
			params.put(LIMIT_PARAM, String.valueOf(limit));
		}
		params.put(TIMEOUT_PARAM, String.valueOf(timeout));
		
		HttpResult result = HttpUtil.get(uri, null, getRequestProperties(apiKey), params, timeout);
		checkResult(uri, result, false);
		if (result.response == null || result.response.isEmpty()) {
			return null;
		}
		return new JSONArray(result.response);
	}
	
	private static void checkResult(URI uri, HttpResult result) throws IOException {
		checkResult(uri, result, false);
	}
	
	private static void checkResult(URI uri, HttpResult result, boolean checkResponse) throws IOException {
		if (!result.isGoodResponse) {
			String msg = String.format("Received bad response %d with error message %s for request: %s", //$NON-NLS-1$
					result.responseCode, result.error, uri);
			MCLogger.logError(msg);
			throw new IOException(msg);
		}
		
		if (checkResponse && (result.response == null || result.response.isEmpty())) {
			String msg = "Received good return code but response is empty for request: " + uri;
			MCLogger.logError(msg);
			throw new IOException(msg);
		}
	}

	private static Map<String, String> getRequestProperties(String apiKey) {
		Map<String, String> requestProperties = new HashMap<String, String>();
		requestProperties.put(API_KEY_PROP, apiKey);
		return requestProperties;
	}
	
	private static String getTypeList(Set<String> types) {
		StringBuilder builder = new StringBuilder();
		boolean start = true;
		for (String type : types) {
			if (start) {
				start = false;
			} else {
				builder.append(",");
			}
			builder.append(type);
		}
		return builder.toString();
	}
	
}
