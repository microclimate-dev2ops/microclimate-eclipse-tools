package com.ibm.microclimate.core.internal;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import javax.json.JsonObject;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class MicroclimateConnection {

	private String host;
	private int port;

	private String baseUrl;
	private IPath localWorkspacePath;

	MicroclimateConnection (String host, int port) throws ConnectException {
		String baseUrl_ = buildUrl(host, port);

		if(!test(baseUrl_)) {
			throw new ConnectException(
					String.format("Connecting to Microclimate instance at \"%s\" failed", baseUrl_));
		}

		this.host = host;
		this.port = port;
		this.baseUrl = baseUrl_;
		// TODO
		this.localWorkspacePath = new Path("/Users/tim/programs/microclimate/");
	}

	@Override
	public boolean equals(Object other) {
		if(!(other instanceof MicroclimateConnection)) {
			return false;
		}

		MicroclimateConnection otherMcc = (MicroclimateConnection) other;
		return otherMcc.baseUrl.equals(baseUrl);
	}

	private static String buildUrl(String host, int port) {
		return String.format("http://%s:%d/", host, port);
	}

	private static boolean test(String baseUrl) {
		String getResult = null;
		try {
			getResult = get(baseUrl);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//System.out.println("From " + url + " got:");
		//System.out.println(getResult);

		return getResult != null && getResult.contains("Microclimate");
	}

	public List<MicroclimateApplication> apps() {
		String projectsUrl = baseUrl + "api/v1/projects";

		String projectsResponse = null;
		try {
			projectsResponse = get(projectsUrl);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if(projectsResponse == null) {
			System.err.println("Received null response from projects endpoint");
			return Collections.emptyList();
		}

		return MicroclimateApplication.buildFromProjectsJson(this, projectsResponse);
	}

	// Temporary
	// TODO replace with socket communication
	public static String get(String url) throws IOException {
		HttpURLConnection connection = null;
		BufferedReader in = null;

		try {
			connection = (HttpURLConnection) new URL(url).openConnection();
			System.out.println("GET " + url);

			connection.setRequestMethod("GET");
			connection.setReadTimeout(2000);
			int responseCode = connection.getResponseCode();

			if (responseCode > 199 && responseCode < 300) {
				return readAllFromStream(connection.getInputStream());
			}
			else {
				System.err.println("Received bad response code: " + responseCode);
				InputStream errorStream = connection.getErrorStream();
				if(errorStream != null) {
					System.err.println(readAllFromStream(errorStream));
				}
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (connection != null) {
				connection.disconnect();
			}
		}
		return null;
	}

	// Temporary
	// TODO replace with socket communication
	public static String post(String url, JsonObject payload) {
		HttpURLConnection connection = null;
		BufferedReader in = null;

		System.out.println("POST " + payload.toString() + " TO " + url);
		try {
			connection = (HttpURLConnection) new URL(url).openConnection();

			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setDoOutput(true);

			DataOutputStream payloadStream = new DataOutputStream(connection.getOutputStream());
			payloadStream.write(payload.toString().getBytes());

			int responseCode = connection.getResponseCode();

			if (responseCode > 199 && responseCode < 300) {
				return readAllFromStream(connection.getInputStream());
			}
			else {
				System.err.println("Received bad response code: " + responseCode);
				InputStream errorStream = connection.getErrorStream();
				if(errorStream != null) {
					return readAllFromStream(errorStream);
					// System.err.println();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (connection != null) {
				connection.disconnect();
			}
		}
		return null;
	}

	private static String readAllFromStream(InputStream stream) {
		Scanner s = new Scanner(stream);
		s.useDelimiter("\\A");
		String result = s.hasNext() ? s.next() : "";
		s.close();
		return result;
	}

	// Getters

	public String host() {
		return host;
	}

	public String baseUrl() {
		return baseUrl;
	}

	public IPath localWorkspacePath() {
		return localWorkspacePath;
	}

	// Note that toString and fromString are used to save and load connections from the preferences store
	// in MicroclimateConnectionManager

	private static final String HOST_KEY = "$host", PORT_KEY = "$port";

	@Override
	public String toString() {
		return String.format("%s %s=%s %s=%s", MicroclimateConnection.class.getSimpleName(),
				HOST_KEY, host, PORT_KEY, port);
	}

	public static MicroclimateConnection fromString(String str) throws ConnectException {

		// Note that NumberFormat and StringIndexOutOfBounds are handled by caller

		int hostIndex = str.indexOf(HOST_KEY);
		String afterHostKey = str.substring(hostIndex + HOST_KEY.length() + 1);

		// The hostname is between HOST_KEY and the PORT_KEY, also trim the space between them
		String host = afterHostKey.substring(0, afterHostKey.indexOf(PORT_KEY)).trim();

		int portIndex = str.indexOf(PORT_KEY);
		String portStr = str.substring(portIndex + PORT_KEY.length() + 1);

		int port = Integer.parseInt(portStr);

		return new MicroclimateConnection(host, port);
	}
}
