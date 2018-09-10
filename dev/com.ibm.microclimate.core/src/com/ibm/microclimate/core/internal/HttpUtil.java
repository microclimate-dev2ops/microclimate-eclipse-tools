/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core.internal;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;

import org.json.JSONObject;

/**
 * Static utilities to allow easy HTTP communication, and make diagnosing and handling errors a bit easier.
 *
 * @author timetchells@ibm.com
 *
 */
public class HttpUtil {

	private HttpUtil() {}

	public static class HttpResult {
		public final int responseCode;
		public final boolean isGoodResponse;

		// Can be null
		public final String response;
		// Can be null
		public final String error;

		public HttpResult(HttpURLConnection connection) throws IOException {
			responseCode = connection.getResponseCode();
			isGoodResponse = responseCode > 199 && responseCode < 300;

			// Read error first because sometimes if there is an error, connection.getInputStream() throws an exception
			InputStream eis = connection.getErrorStream();
			if (eis != null) {
				error = MCUtil.readAllFromStream(eis);
			}
			else {
				error = null;
			}

			if (!isGoodResponse) {
				MCLogger.logError("Received bad response code " + responseCode + " from "
						+ connection.getURL() + " - Error:\n" + error);
			}

			InputStream is = connection.getInputStream();
			if (is != null) {
				response = MCUtil.readAllFromStream(is);
			}
			else {
				response = null;
			}
		}
	}

	public static HttpResult get(URI uri) throws IOException {
		HttpURLConnection connection = null;
		BufferedReader in = null;

		try {
			connection = (HttpURLConnection) uri.toURL().openConnection();
			// MCLogger.log("GET " + url);

			connection.setRequestMethod("GET");
			connection.setReadTimeout(5000);

			return new HttpResult(connection);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					MCLogger.logError(e);
				}
			}
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	public static HttpResult post(URI uri, JSONObject payload) throws IOException {
		HttpURLConnection connection = null;
		BufferedReader in = null;

		MCLogger.log("POST " + payload.toString() + " TO " + uri);
		try {
			connection = (HttpURLConnection) uri.toURL().openConnection();

			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setDoOutput(true);

			DataOutputStream payloadStream = new DataOutputStream(connection.getOutputStream());
			payloadStream.write(payload.toString().getBytes());

			return new HttpResult(connection);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					MCLogger.logError(e);
				}
			}
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
}
