package com.ibm.microclimate.core.internal;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Display;

public class MicroclimateConnectionManager {
	
	private static MicroclimateConnectionManager instance;

	public static final String CONNECTION_LIST_PREFSKEY = "mcc-connections";

	private List<MicroclimateConnection> connections = new ArrayList<MicroclimateConnection>();

	private IPreferenceStore preferenceStore;
		
	private MicroclimateConnectionManager() {
		if(instance != null) {
			System.err.println("MULTIPLE SINGLETON INSTANCES OF MCCM");
		}
		instance = this;
		
		System.out.println("Init microclimateConnectionManager");
		preferenceStore = com.ibm.microclimate.core.Activator.getDefault().getPreferenceStore();
		loadFromPreferences();
		
		com.ibm.microclimate.core.Activator.getDefault().getPreferenceStore()
		.addPropertyChangeListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                if (event.getProperty() == MicroclimateConnectionManager.CONNECTION_LIST_PREFSKEY) {
                	System.out.println("Loading prefs in MCCM");
                    loadFromPreferences();
                }
            }
        });
	}
	
	private static MicroclimateConnectionManager instance() {
		if (instance == null) {
			instance = new MicroclimateConnectionManager();
		}
		return instance;
	}
	
	public static MicroclimateConnection create(String host, int port) throws ConnectException {
		// Will throw an exception if connection fails
		MicroclimateConnection newConnection = new MicroclimateConnection(host, port);
		// Connection succeeded
		instance().add(newConnection);
		
		return newConnection;
	}
	
	private void add(MicroclimateConnection connection/*, boolean writeToPrefs*/) {
		if(!connections.contains(connection)) {
			connections.add(connection);
			System.out.println("Added a new MCConnection: " + connection.baseUrl());
		}
		else {
			System.out.println("MCConnection " + connection.baseUrl() + " already exists");
		}
		
		/*
		if(writeToPrefs) {
			writeToPreferences();
		}*/
	}
	
	/**
	 * @return An <b>unmodifiable</b> copy of the list of existing MC connections.
	 */
	public static List<MicroclimateConnection> connections() {
		// System.out.println("Returning " + connections.size() + " connections");
		// Have to do this every time?
		// loadFromPreferences();
		return Collections.unmodifiableList(instance.connections);
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
		return instance().connections.size();
	}
	
	public static boolean remove(MicroclimateConnection connection) {
		System.out.println("Trying to remove MCConnection: " + connection.baseUrl());
		return instance().connections.remove(connection);
	}
	
	public static void clear() {
		System.out.println("Clearing " + instance().connections.size() + " connections");
		instance().connections.clear();
	}
	
	private void writeToPreferences() {
		StringBuilder prefsBuilder = new StringBuilder();
		
		for(MicroclimateConnection mcc : connections()) {
			// This is safe so long as there are no newlines in mcc.toString().
			prefsBuilder.append(mcc.toString()).append('\n');
		}		
		System.out.println("Writing connections to preferences: " + prefsBuilder.toString());

		preferenceStore.setValue(CONNECTION_LIST_PREFSKEY, prefsBuilder.toString());
	}
	
	private void loadFromPreferences() {
		clear();
		
		String storedConnections = preferenceStore.getString(CONNECTION_LIST_PREFSKEY).trim();
		System.out.println("Reading connections from preferences: \"" + storedConnections + "\"");
		
		for(String line : storedConnections.split("\n")) {
			if(line.trim().isEmpty()) { 
				// handles empty preferences
				continue;
			}
			
			try {
				add(MicroclimateConnection.fromString(line));
			}
			catch(ConnectException e) {
				// Probably we should keep the connection info, but mark it as 'inactive' or similar
				e.printStackTrace();
				MessageDialog.openError(Display.getDefault().getActiveShell(), 
						"Failed to connect to Microclimate instance", e.getMessage());
			}
		}
		
		writeToPreferences();
	}
}
