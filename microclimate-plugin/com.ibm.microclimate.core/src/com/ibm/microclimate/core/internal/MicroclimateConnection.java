package com.ibm.microclimate.core.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;

public class MicroclimateConnection {
	
	String host;
	String port;
	
	public MicroclimateConnection getMicroclimateConnection(String host, String port) {
		// TODO: Get a microclimate connection instance
		this.host = host;
		this.port = port;
		return null;
	}
	
	public List<MicroclimateApplication> getApps() {
		// TODO: get the current list of applications
		return Collections.emptyList();
	}
	
	// Temporary
	protected String sendGet(String url) {
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

}
