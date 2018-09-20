/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core.internal.connection;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;
import org.json.JSONException;

import com.ibm.microclimate.core.MicroclimateCorePlugin;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.constants.MCConstants;
import com.ibm.microclimate.core.internal.messages.Messages;
import com.ibm.microclimate.core.internal.server.MicroclimateServer;;

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

	public static final String CONNECTION_LIST_PREFSKEY = "mcc-connections"; //$NON-NLS-1$

	private List<MicroclimateConnection> connections = new ArrayList<>();
	// this list tracks the URLs of connections that have never successfully connected
	private List<String> brokenConnections = new ArrayList<>();

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
			MCLogger.logError("Null connection passed to be added"); //$NON-NLS-1$
			return;
		}

		instance().connections.add(connection);
		MCLogger.log("Added a new MCConnection: " + connection.baseUrl); //$NON-NLS-1$
		instance().writeToPreferences();
	}

	/**
	 * @return An <b>unmodifiable</b> copy of the list of existing MC connections.
	 */
	public synchronized static List<MicroclimateConnection> activeConnections() {
		return Collections.unmodifiableList(instance().connections);
	}

	public synchronized static MicroclimateConnection getActiveConnection(String baseUrl) {
		for(MicroclimateConnection mcc : activeConnections()) {
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
	synchronized static boolean remove(String baseUrl) {
		boolean removeResult = false;

		MicroclimateConnection connection = MicroclimateConnectionManager.getActiveConnection(baseUrl.toString());
		if (connection != null) {
			connection.close();
			removeResult = instance().connections.remove(connection);
		}
		else {
			removeResult = instance().brokenConnections.remove(baseUrl);
		}

		if (!removeResult) {
			MCLogger.logError("Tried to remove MCConnection " + baseUrl + ", but it didn't exist"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		instance().writeToPreferences();
		return removeResult;
	}

	/**
	 * Deletes all of the instance's connections. Does NOT write to preferences after doing so.
	 */
	public synchronized static void clear() {
		MCLogger.log("Clearing " + instance().connections.size() + " connections"); //$NON-NLS-1$ //$NON-NLS-2$

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
	public synchronized static List<String> brokenConnections() {
		return Collections.unmodifiableList(instance().brokenConnections);
	}

	public synchronized static String getBrokenConnection(String url) {
		for (String brokenConnectionUrl : brokenConnections()) {
			if (brokenConnectionUrl.toString().equals(url)) {
				return brokenConnectionUrl;
			}
		}
		return null;
	}

	/*
	public synchronized static boolean connectionExists(String baseUrl) {
		return getActiveConnection(baseUrl) != null || getBrokenConnection(baseUrl) != null;
	}
	*/

	// Preferences serialization

	private void writeToPreferences() {
		StringBuilder prefsBuilder = new StringBuilder();

		for (MicroclimateConnection mcc : activeConnections()) {
			prefsBuilder.append(mcc.toPrefsString()).append('\n');
		}
		for (String mcc : brokenConnections()) {
			prefsBuilder.append(mcc).append('\n');
		}

		MCLogger.log("Writing connections to preferences: " + prefsBuilder.toString()); //$NON-NLS-1$

		MicroclimateCorePlugin.getDefault().getPreferenceStore()
				.setValue(CONNECTION_LIST_PREFSKEY, prefsBuilder.toString());
	}

	private void loadFromPreferences() {
		clear();

		String storedConnections = MicroclimateCorePlugin.getDefault()
				.getPreferenceStore()
				.getString(CONNECTION_LIST_PREFSKEY).trim();

		MCLogger.log("Reading connections from preferences: \"" + storedConnections + "\""); //$NON-NLS-1$ //$NON-NLS-2$

		for(String line : storedConnections.split("\n")) { //$NON-NLS-1$
			line = line.trim();
			if(line.isEmpty()) {
				continue;
			}

			try {
				// Assume all connections are active. If they are broken they will be handled in the catch below.
				MicroclimateConnection connection = MicroclimateConnection.fromPrefsString(line);
				add(connection);
			}
			catch (MicroclimateConnectionException mce) {
				// The MC instance we wanted to connect to is down.
				brokenConnections.add(mce.connectionUrl.toString());
				MicroclimateReconnectJob.createAndStart(mce.connectionUrl);
			}
			catch (JSONException | IOException | URISyntaxException e) {
				MCLogger.logError("Error loading MCConnection from preferences", e); //$NON-NLS-1$
			}
		}

	}

	/**
	 * Proceeding with deleting this connection will delete all its linked servers too.
	 * Make this clear to the user, then delete the servers if they still wish to delete the connection.
	 *
	 * @return If the connection was ultimately deleted.
	 */
	public static boolean removeConnection(String mcConnectionUrl) {
		final Set<IServer> linkedServers = getServers(mcConnectionUrl);

		if (linkedServers == null || linkedServers.isEmpty()) {
			// don't have to do any special handling
			MicroclimateConnectionManager.remove(mcConnectionUrl);
			return true;
		}

		final String[] buttons = new String[] {
				Messages.MicroclimateConnectionManager_CancelBtn,
				Messages.MicroclimateConnectionManager_DeleteServersBtn
		};

		final int deleteBtnIndex = 1;

		final String message = NLS.bind(Messages.MicroclimateConnectionManager_ServersLinkedDialogMsg,
				getServerNames(linkedServers, false), mcConnectionUrl);

		AtomicBoolean deleted = new AtomicBoolean(false);

		Runnable confirmDeleteRunnable = new Runnable() {
			@Override
			public void run() {
				MessageDialog dialog = new MessageDialog(
						Display.getDefault().getActiveShell(),
						Messages.MicroclimateConnectionManager_ServersLinkedDialogTitle,
						Display.getDefault().getSystemImage(SWT.ICON_WARNING),
						message, MessageDialog.WARNING, buttons,
						// Below is the index of the initially selected button - This unfortunately is always
						// the rightmost button in the dialog. So it's not possible to have the normal order
						// (ie Cancel to the left of OK) but also have Cancel selected initially.
						deleteBtnIndex);

				final boolean deleteConfirm = dialog.open() == deleteBtnIndex;

				if (deleteConfirm) {
					for (IServer server : linkedServers) {
						try {
							server.delete();
						}
						catch (CoreException e) {
							MCLogger.logError("Error deleting server when deleting MCConnection", e); //$NON-NLS-1$
							MCUtil.openDialog(true,
									Messages.MicroclimateConnectionManager_ErrDeletingServerDialogTitle + server.getName(),
									e.getMessage());
						}
					}

					MicroclimateConnectionManager.remove(mcConnectionUrl);
				}
				deleted.set(deleteConfirm);

				synchronized(deleted) {
					deleted.notify();
				}
			}
		};

		boolean onUIThread = Display.getDefault().getThread().equals(Thread.currentThread());
		MCLogger.log("On UI thread = " + onUIThread); //$NON-NLS-1$

		MCLogger.log("Waiting for user to confirm delete linked server"); //$NON-NLS-1$
		if (onUIThread) {
			confirmDeleteRunnable.run();
		}
		else {
			Display.getDefault().asyncExec(confirmDeleteRunnable);

			// If we're not on the UI thread, the confirm delete executes async, and we have to wait for it to finish
			try {
				synchronized(deleted) {
					deleted.wait();
				}
			} catch (InterruptedException e) {
				MCLogger.logError(e);
			}
		}

		final boolean result = deleted.get();
		MCLogger.log("User deleted linked server(s)? " + result); //$NON-NLS-1$
		return result;
	}

	private static Set<IServer> getServers(String mcConnectionUrl) {
		Set<IServer> servers = new HashSet<>();
		for (IServer server : ServerCore.getServers()) {
			if (mcConnectionUrl.equals(server.getAttribute(MicroclimateServer.ATTR_MCC_URL, ""))) { //$NON-NLS-1$
				servers.add(server);
			}
		}
		return servers;
	}

	private static String getServerNames(Set<IServer> servers, boolean appNameOnly) {

		return MCUtil.toCommaSeparatedString(
				servers.stream()
					.map((server) -> {
						String serverName = server.getName();
						if (appNameOnly) {
							// Remove the base name from the start
							serverName = serverName.substring(MCConstants.MC_SERVER_BASE_NAME.length(),
									server.getName().length());
						}
						return serverName;
					})
					.collect(Collectors.toSet())
				);
	}

	public static String getLinkedAppNames(String mcConnectionUrl) {
		return getServerNames(getServers(mcConnectionUrl), true);
	}
}
