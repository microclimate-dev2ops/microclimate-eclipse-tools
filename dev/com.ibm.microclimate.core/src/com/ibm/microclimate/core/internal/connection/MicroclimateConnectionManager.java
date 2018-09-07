package com.ibm.microclimate.core.internal.connection;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.json.JSONException;

import com.ibm.microclimate.core.MicroclimateCorePlugin;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;;

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
	// this list tracks the URLs of connections that have never successfully connected
	private List<URI> brokenConnections = new ArrayList<>();

	private MicroclimateConnectionManager() {
		instance = this;

		// MCLogger.log("Init MicroclimateConnectionManager");
		loadFromPreferences();

		// Add a preference listener to reload the cached list of connections each time it's modified.
		MicroclimateCorePlugin.getDefault().getPreferenceStore()
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
	public synchronized static void add(MicroclimateConnection connection) {
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
	public synchronized static List<MicroclimateConnection> connections() {
		return Collections.unmodifiableList(instance().connections);
	}



	public synchronized static MicroclimateConnection getConnection(String baseUrl) {
		for(MicroclimateConnection mcc : connections()) {
			if(mcc.baseUrl.toString().equals(baseUrl)) {
				return mcc;
			}
		}
		return null;
	}

	public synchronized static int activeConnectionsCount() {
		return instance().connections.size();
	}

	/**
	 * Try to remove the given connection. Removal will fail if the connection is still in use (ie. has a linked app).
	 * @return
	 * 	true if the connection was removed,
	 * 	false if not because it didn't exist.
	 */
	public synchronized static boolean remove(MicroclimateConnection connection) {
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
	public synchronized static void clear() {
		MCLogger.log("Clearing " + instance().connections.size() + " connections");

		Iterator<MicroclimateConnection> it = instance().connections.iterator();

		while(it.hasNext()) {
			MicroclimateConnection connection = it.next();
			connection.close();
			it.remove();
		}
	}

	/**
	 * @return An <b>unmodifiable</b> copy of the list of broken MC Connection URLs.
	 */
	public synchronized static List<URI> brokenConnections() {
		return Collections.unmodifiableList(instance().brokenConnections);
	}

	public synchronized static URI getBrokenConnection(String url) {
		for (URI brokenConnectionUri : brokenConnections()) {
			if (brokenConnectionUri.toString().equals(url)) {
				return brokenConnectionUri;
			}
		}
		return null;
	}

	public synchronized static boolean removeBrokenConnection(URI url) {
		return instance().brokenConnections.remove(url);
	}

	// Preferences serialization

	private void writeToPreferences() {
		StringBuilder prefsBuilder = new StringBuilder();

		for(MicroclimateConnection mcc : connections()) {
			prefsBuilder.append(mcc.toPrefsString()).append('\n');
		}
		MCLogger.log("Writing connections to preferences: " + prefsBuilder.toString());

		MicroclimateCorePlugin.getDefault().getPreferenceStore()
				.setValue(CONNECTION_LIST_PREFSKEY, prefsBuilder.toString());
	}

	private void loadFromPreferences() {
		clear();

		String storedConnections = MicroclimateCorePlugin.getDefault()
				.getPreferenceStore()
				.getString(CONNECTION_LIST_PREFSKEY).trim();

		MCLogger.log("Reading connections from preferences: \"" + storedConnections + "\"");

		for(String line : storedConnections.split("\n")) {
			line = line.trim();
			if(line.isEmpty()) {
				continue;
			}

			try {
				MicroclimateConnection connection = MicroclimateConnection.fromPrefsString(line);
				add(connection);
			}
			catch (MicroclimateConnectionException mce) {
				brokenConnections.add(mce.connectionUrl);
				spawnReconnectJob(mce.connectionUrl);
			} catch (JSONException | IOException | URISyntaxException e) {
				MCLogger.logError("Error loading MCConnection from preferences", e);
			}
		}
	}

	private static void spawnReconnectJob(URI url) {
		String msg = "Reconnecting to Microclimate at " + url;

		Job reconnectJob = Job.create(msg, new ICoreRunnable() {
			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				// each re-connect attempt takes 2 seconds because that's how long the socket tries to connect for
				// so, we delay for 4 seconds, try to connect for 2 seconds, repeat.
				final int delay = 4000;
				monitor.beginTask("Reconnecting to Microclimate at " + url, 100);

				int i = 0;
				while(!monitor.isCanceled()) {
					try {
						Thread.sleep(delay);
						i++;

						if (i % 5 == 0) {
							MCLogger.log("Trying to reconnect to Microclimate at " + url);
						}

						MicroclimateConnection newConnection = new MicroclimateConnection(url);
						if (newConnection != null) {
							// connection re-established!
							MCLogger.log("Successfully re-connected to Microclimate at " + url);
							instance().brokenConnections.remove(url);
							instance().connections.add(newConnection);
							break;
						}
					}
					catch (InterruptedException e) {
						MCLogger.logError(e);
					}
					catch(MicroclimateConnectionException e) {
						// nothing, the connection just failed. we'll try again.
					}
					catch (Exception e) {
						// If any exception other than a MCConnectionException (handled by tryReconnect) occurs,
						// it is most likely that this connection will never succeed.
						MCLogger.logError(e);
						monitor.setCanceled(true);

						MCUtil.openDialog(true, "Error reconnecting to Microclimate",
								"Could not reconnect to " + url + ".\n" +
								"Please re-create this connection in the Microclimate Connection preferences.");
					}
				}

				monitor.done();
			}
		});

		reconnectJob.schedule();
	}
}
