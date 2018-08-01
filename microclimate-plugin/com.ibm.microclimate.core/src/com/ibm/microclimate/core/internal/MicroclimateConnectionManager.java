package com.ibm.microclimate.core.internal;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MicroclimateConnectionManager {

	private static List<MicroclimateConnection> connections = new ArrayList<MicroclimateConnection>();
	
	public static MicroclimateConnection create(String host, int port) throws ConnectException {
		// Will throw an exception if connection fails
		MicroclimateConnection newConnection = new MicroclimateConnection(host, port);
		
		// Connection succeeded
		if(!connections.contains(newConnection)) {
			connections.add(newConnection);
			System.out.println("Added a new MCConnection: " + newConnection.baseUrl());
		}
		else {
			System.out.println("MCConnection " + newConnection.baseUrl() + " already exists");
		}
		return newConnection;
	}
	
	/**
	 * @return An <b>unmodifiable</b> copy of the list of existing MC connections.
	 */
	public static List<MicroclimateConnection> connections() {
		// System.out.println("Returning " + connections.size() + " connections");
		return Collections.unmodifiableList(connections);
	}
	
	public static MicroclimateConnection getConnection(String baseUrl) {
		for(MicroclimateConnection mcc : connections()) {
			if(mcc.baseUrl().equals(baseUrl)) {
				return mcc;
			}
		}
		return null;
	}
	
	public static int connectionsCount() {
		return connections.size();
	}
	
	public static boolean remove(MicroclimateConnection connection) {
		System.out.println("Trying to remove MCConnection: " + connection.baseUrl());
		return connections.remove(connection);
	}
	
	public static void clear() {
		System.out.println("Clearing " + connections.size() + " connections");
		connections.clear();
	}
}
