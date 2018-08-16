package com.ibm.microclimate.core.internal;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import org.json.JSONObject;

import com.ibm.microclimate.core.MCLogger;

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
				error = readAllFromStream(eis);
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
				response = readAllFromStream(is);
			}
			else {
				response = null;
			}
		}
	}

	public static HttpResult get(String url) throws IOException {
		HttpURLConnection connection = null;
		BufferedReader in = null;

		try {
			connection = (HttpURLConnection) new URL(url).openConnection();
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

	public static HttpResult post(String url, JSONObject payload) throws IOException {
		HttpURLConnection connection = null;
		BufferedReader in = null;

		MCLogger.log("POST " + payload.toString() + " TO " + url);
		try {
			connection = (HttpURLConnection) new URL(url).openConnection();

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

	private static String readAllFromStream(InputStream stream) {
		Scanner s = new Scanner(stream);
		// end-of-stream
		s.useDelimiter("\\A");
		String result = s.hasNext() ? s.next() : "";
		s.close();
		return result;
	}
}
