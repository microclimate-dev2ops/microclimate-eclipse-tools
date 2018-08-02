package com.ibm.microclimate.core.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class MicroclimateConnection {
	
	//private String host;
	//private int port;
	
	private String baseUrl;
	
	MicroclimateConnection (String host, int port) throws ConnectException {
		this(buildUrl(host, port));
	}
	
	MicroclimateConnection (String baseUrl) throws ConnectException {
		if(!test(baseUrl)) {
			throw new ConnectException(
					String.format("Connecting to Microclimate instance at \"%s\" failed", baseUrl));
		}
		
		//this.host = host;
		//this.port = port;
		this.baseUrl = baseUrl;
	}
	
	@Override
	public boolean equals(Object other) {
		if(!(other instanceof MicroclimateConnection)) {
			return false; 
		}
		
		MicroclimateConnection otherMcc = (MicroclimateConnection) other;
		return otherMcc.baseUrl.equals(baseUrl);
	}
	
	// Note that toString and fromString are used to save and load connections from the preferences store
	// in MicroclimateConnectionManager
	
	private static final String BASE_URL_KEY = "baseUrl";
	
	@Override
	public String toString() {
		return String.format("MicroclimateConnection %s=%s", BASE_URL_KEY, baseUrl);
	}
	
	public static MicroclimateConnection fromString(String str) throws ConnectException {
		// Extract the baseUrl "baseUrl=xyz"	
		return new MicroclimateConnection(str.substring(str.indexOf(BASE_URL_KEY) + BASE_URL_KEY.length() + 1));
	}
	
	public List<MicroclimateApplication> apps() {
		String projectsUrl = baseUrl + "api/v1/projects";
		String projectsResponse = sendGet(projectsUrl);
		
		if(projectsResponse == null) {
			System.err.println("Received null response from projects endpoint");
			return Collections.emptyList();
		}
		
		List<MicroclimateApplication> result = new ArrayList<MicroclimateApplication>();
		try {
			JsonReader jsReader = Json.createReader(new StringReader(projectsResponse));
			JsonArray appArray = jsReader.readArray();
			
			for(int i = 0; i < appArray.size(); i++) {
				JsonObject app = appArray.getJsonObject(i);
				String id 	= app.getString("projectID");
				String name = app.getString("name");
				String lang = app.getString("language");
				
				result.add(new MicroclimateApplication(this, id, name, lang));
			}
		}
		catch(JsonException e) {
			e.printStackTrace();
		}
		
		return result;
		
	}
	
	private static boolean test(String baseUrl) {
		String getResult = sendGet(baseUrl);
		
		//System.out.println("From " + url + " got:");
		//System.out.println(getResult);
		
		return getResult != null && getResult.contains("Microclimate");
	}
	
	private static String buildUrl(String host, int port) {
		return String.format("http://%s:%d/", host, port);
	}
	
	// Temporary
	// TODO replace with socket communication
	private static String sendGet(String url) {
		HttpURLConnection connection = null;
		BufferedReader in = null;
		
		try {
			connection = (HttpURLConnection) new URL(url).openConnection();
			
			connection.setRequestMethod("GET");
			int responseCode = connection.getResponseCode();
			
			if (responseCode == HttpURLConnection.HTTP_OK) { 
				in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String inputLine;
				StringBuffer res = new StringBuffer();

				while ((inputLine = in.readLine()) != null) {
					res.append(inputLine);
				}
			
				if (res != null) {
					return(res.toString());
				}
			}
		} catch (Exception e) {
			// TODO: logging
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

	public String baseUrl() {
		return baseUrl;
	}
}
