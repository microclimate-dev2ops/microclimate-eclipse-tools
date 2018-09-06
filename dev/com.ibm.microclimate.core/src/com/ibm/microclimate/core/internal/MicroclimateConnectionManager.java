package com.ibm.microclimate.core.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

/**
 * Singleton class to keep track of the list of current Microclimate Connections,
 * and manage persisting them to and from the Preferences.
 *
 * @author timetchells@ibm.com
 *
 */
public class MicroclimateConnectionManager {

	// Singleton instance. Never access this directly. Use the instance() method.
	private static MicroclimateConnectionManager instance;

	public static final String CONNECTION_LIST_PREFSKEY = "mcc-connections";

	private List<MicroclimateConnection> connections = new ArrayList<>();

	private MicroclimateConnectionManager() {
		instance = this;

		// MCLogger.log("Init MicroclimateConnectionManager");
		loadFromPreferences();

		// Add a preference listener to reload the cached list of connections each time it's modified.
		com.ibm.microclimate.core.MicroclimateCorePlugin.getDefault().getPreferenceStore()
			.addPropertyChangeListener(new IPropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent event) {
				    if (event.getProperty() == MicroclimateConnectionManager.CONNECTION_LIST_PREFSKEY) {
				    	// MCLogger.log("Loading prefs in MCCM");
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

	/**
	 * Adds the given connection to the list of connections.
	 */
	public static void add(MicroclimateConnection connection) {
		if (connection == null) {
			MCLogger.logError("Null connection passed to be added");
			return;
		}

		instance().connections.add(connection);
		MCLogger.log("Added a new MCConnection: " + connection.baseUrl);
		instance().writeToPreferences();
	}

	/**
	 * @return An <b>unmodifiable</b> copy of the list of existing MC connections.
	 */
	public static List<MicroclimateConnection> connections() {
		return Collections.unmodifiableList(instance().connections);
	}

	public static MicroclimateConnection getConnection(String baseUrl) {
		for(MicroclimateConnection mcc : connections()) {
			if(mcc.baseUrl.equals(baseUrl)) {
				return mcc;
			}
		}
		return null;
	}

	public static int connectionsCount() {
		return instance().connections.size();
	}

	/**
	 * Try to remove the given connection. Removal will fail if the connection is still in use (ie. has a linked app).
	 * @return
	 * 	true if the connection was removed,
	 * 	false if not because it didn't exist.
	 */
	public static boolean remove(MicroclimateConnection connection) {
		connection.close();
		boolean removeResult = instance().connections.remove(connection);
		if (!removeResult) {
			MCLogger.logError("Tried to remove MCConnection " + connection.baseUrl + ", but it didn't exist");
		}
		instance().writeToPreferences();
		return removeResult;
	}

	/**
	 * Deletes all of the instance's connections. Does NOT write to preferences after doing so.
	 */
	public static void clear() {
		MCLogger.log("Clearing " + instance().connections.size() + " connections");

		Iterator<MicroclimateConnection> it = instance().connections.iterator();

		while(it.hasNext()) {
			MicroclimateConnection connection = it.next();
			connection.close();
			it.remove();
		}
	}

	// Preferences serialization

	private void writeToPreferences() {
		StringBuilder prefsBuilder = new StringBuilder();

		for(MicroclimateConnection mcc : connections()) {
			prefsBuilder.append(mcc.toPrefsString()).append('\n');
		}
		MCLogger.log("Writing connections to preferences: " + prefsBuilder.toString());

		com.ibm.microclimate.core.MicroclimateCorePlugin.getDefault().getPreferenceStore()
				.setValue(CONNECTION_LIST_PREFSKEY, prefsBuilder.toString());
	}

	private void loadFromPreferences() {
		clear();

		String storedConnections = com.ibm.microclimate.core.MicroclimateCorePlugin.getDefault()
				.getPreferenceStore()
				.getString(CONNECTION_LIST_PREFSKEY).trim();

		MCLogger.log("Reading connections from preferences: \"" + storedConnections + "\"");

		StringBuilder failedConnectionsBuilder = new StringBuilder();
		for(String line : storedConnections.split("\n")) {
			if(line.trim().isEmpty()) {
				continue;
			}

			try {
				add(MicroclimateConnection.fromPrefsString(line));
			}
			catch(StringIndexOutOfBoundsException | NumberFormatException e) {
				MCLogger.logError(e);
				MCLogger.logError("Stored MCConnection preference line did not match expected format:\n" + line);
			}
			catch(Exception e) {
				// TODO Probably we should keep the connection info, but mark it as 'inactive' or similar
				// right now it will just be deleted (because it's never added back to the connections list)
				MCLogger.logError(e);

				failedConnectionsBuilder.append(e.getMessage()).append('\n');
			}
		}

		String failedConnections = failedConnectionsBuilder.toString();
		if (!failedConnections.isEmpty()) {
			MCUtil.openDialog(true, "Failed to connect to Microclimate instance(s)", failedConnections);
		}

		// writeToPreferences();
	}
}
