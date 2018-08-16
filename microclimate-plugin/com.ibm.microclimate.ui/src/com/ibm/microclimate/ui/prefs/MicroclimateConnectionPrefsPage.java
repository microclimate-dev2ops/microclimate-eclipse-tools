package com.ibm.microclimate.ui.prefs;

import java.util.List;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.ibm.microclimate.core.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateConnection;
import com.ibm.microclimate.core.internal.MicroclimateConnectionManager;
import com.ibm.microclimate.ui.Activator;
import com.ibm.microclimate.ui.wizards.NewMicroclimateConnectionWizard;
import com.ibm.microclimate.ui.wizards.WizardLauncher;

/**
 * This preferences page lists the current Microclimate connections, allows adding new ones, and removing existing ones.
 * It can be launched through Preferences, or from the LinkMicroclimateProjectPage.
 *
 * @author timetchells@ibm.com
 *
 */
public class MicroclimateConnectionPrefsPage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String
			MC_CONNECTIONS_PREFSKEY = "com.ibm.microclimate.ui.prefs.connections",
			PAGE_ID = "MicroclimateConnectionsPage";		// must match the value in plugin.xml

	//private static MicroclimateConnectionPrefsPage instance;

	private Table connectionsTable;

	private List<MicroclimateConnection> connections;

	public MicroclimateConnectionPrefsPage() {
		super("Microclimate Connections", Activator.getDefaultIcon());
	}

	@Override
	public void init(IWorkbench arg0) {
		// Note that ConfigurationScope is used. This means that our list of MCConnections is shared
		// between different workspaces.
		setPreferenceStore(new ScopedPreferenceStore(ConfigurationScope.INSTANCE, MC_CONNECTIONS_PREFSKEY));
		setDescription("Microclimate connection preferences description");
	}

	@Override
	protected Control createContents(Composite parent) {

		parent.setLayout(new GridLayout(1, true));

		Composite top = new Composite(parent, SWT.LEFT);
		top.setLayout(new GridLayout(3, false));

		Button addNewBtn = new Button(top, SWT.BORDER);
		addNewBtn.setText("Add New Connection");
		addNewBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent se) {
				Wizard wizard = new NewMicroclimateConnectionWizard(false);
				WizardLauncher.launchWizardWithoutSelection(wizard);
			}
		});

		/*
		Button disableSelectedBtn = new Button(top, SWT.BORDER);
		disableSelectedBtn.setText("Disable Selected");
		*/

		Label spacer = new Label(top, SWT.NONE);
		GridData spacerData = new GridData();
		spacerData.widthHint = 160;
		spacer.setLayoutData(spacerData);

		Button removeSelectedBtn = new Button(top, SWT.BORDER);
		removeSelectedBtn.setText("Remove Selected");
		removeSelectedBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent se) {

				boolean connectionIsInUse = false;

				int[] selected = connectionsTable.getSelectionIndices();
				for(int i : selected) {
					MicroclimateConnection connection = connections.get(i);
					if (connection.hasLinkedApp()) {
						// You cannot remove it in this case.
						connectionIsInUse = true;
					}
					else {
						// Do the remove.
						if(!MicroclimateConnectionManager.remove(connection)) {
							MCLogger.logError("Connection with index " + i + " was already removed");
						}
					}
				}

				if (connectionIsInUse) {
					// TODO is this actually a problem?
					MessageDialog.openError(getShell(), "Could not remove connection",
							"At least one of the selected connections has projects linked in the workspace.\n"
									+ "Unlink any projects by deleting the corresponding servers "
									+ "before removing the Microclimate Connection.");
				}

				refreshConnectionsList();
			}
		});

		Composite bottom = new Composite(parent, SWT.CENTER);
		bottom.setLayout(new GridLayout(1, false));
		bottom.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, true, true));

		Label existingConnections = new Label(bottom, SWT.NONE);
		existingConnections.setText("Connections:");

		connectionsTable = new Table(bottom, SWT.BORDER | SWT.MULTI);
		GridData tableGridData = new GridData(GridData.FILL_BOTH, GridData.FILL_BOTH);
		tableGridData.widthHint = 450;
		tableGridData.heightHint = 300;
		connectionsTable.setLayoutData(tableGridData);
		connectionsTable.setHeaderVisible(true);

		TableColumn addresses = new TableColumn(connectionsTable, SWT.BORDER);
		addresses.setText("URL");
		addresses.setWidth(tableGridData.widthHint / 2);

		/*
		TableColumn lastUsed = new TableColumn(connectionsTable, SWT.BORDER);
		lastUsed.setText("Last Used");
		lastUsed.setWidth(tableGridData.widthHint / 3);
		*/

		TableColumn enabled = new TableColumn(connectionsTable, SWT.BORDER);
		enabled.setText("Linked Projects");
		enabled.setWidth(tableGridData.widthHint - addresses.getWidth());

		refreshConnectionsList();

		com.ibm.microclimate.core.Activator.getDefault().getPreferenceStore()
			.addPropertyChangeListener(new IPropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent event) {
				    if (event.getProperty() == MicroclimateConnectionManager.CONNECTION_LIST_PREFSKEY) {
				    	MCLogger.log("Reloading preferences in MCCPP");
				    	// calling refreshConnectionsList here results in WidgetDisposed exception if
				    	// the window is not in focus
				        refreshConnectionsList();
				    }
				}
			});

		return parent;
	}

	private void refreshConnectionsList() {
		// Update the cached connections when we update the table, so that they always match
		connections = MicroclimateConnectionManager.connections();

		if (!connectionsTable.isDisposed()) {
			connectionsTable.removeAll();
		}

		for(MicroclimateConnection mcc : connections) {
			try {
				TableItem ti = new TableItem(connectionsTable, SWT.NONE);

				ti.setText(new String[] { mcc.baseUrl, getLinkedAppsForConnection(mcc) });
			}
			catch(SWTException e) {
				// suppress widget disposed exception - It gets thrown if the window is out of focus,
				// but then the table populates just fine anyway, so I don't know why it is a problem.
				if (!"Widget is disposed".equals(e.getMessage())) {
					throw e;
				}
			}
		}
	}

	private static String getLinkedAppsForConnection(MicroclimateConnection mcc) {
		StringBuilder linkedAppsBuilder = new StringBuilder();
		final String separator = ", ";

		mcc.getLinkedApps().stream()
				.forEachOrdered(app -> linkedAppsBuilder.append(app.name).append(separator));

		// Remove the last separator
		if (linkedAppsBuilder.length() > separator.length()) {
			linkedAppsBuilder.setLength(linkedAppsBuilder.length() - separator.length());
		}

		return linkedAppsBuilder.length() > 0 ? linkedAppsBuilder.toString() : "None";
	}
}
