package com.ibm.microclimate.ui.internal.prefs;

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

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateConnection;
import com.ibm.microclimate.core.internal.MicroclimateConnectionManager;
import com.ibm.microclimate.ui.Activator;
import com.ibm.microclimate.ui.internal.wizards.NewMicroclimateConnectionWizard;
import com.ibm.microclimate.ui.internal.wizards.WizardLauncher;

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
		setMessage("Manage Microclimate Connections");
	}

	@Override
	public void init(IWorkbench arg0) {
		// Note that ConfigurationScope is used. This means that our list of MCConnections is shared
		// between different workspaces.
		setPreferenceStore(new ScopedPreferenceStore(ConfigurationScope.INSTANCE, MC_CONNECTIONS_PREFSKEY));
	}

	@Override
	protected Control createContents(Composite parent) {

		parent.setLayout(new GridLayout(1, true));

		Composite composite = new Composite(parent, SWT.CENTER);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, true, true));
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		composite.setLayoutData(data);

		Label existingConnections = new Label(composite, SWT.NONE);
		existingConnections.setText("Create or remove connections:");
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1);
		existingConnections.setLayoutData(gridData);

		connectionsTable = new Table(composite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		connectionsTable.setLinesVisible(true);
		connectionsTable.setHeaderVisible(true);
		gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2);
		gridData.widthHint = 450;
		gridData.heightHint = 300;
		connectionsTable.setLayoutData(gridData);

		TableColumn addresses = new TableColumn(connectionsTable, SWT.BORDER);
		addresses.setText("URL");
		addresses.setWidth(gridData.widthHint / 2);

		TableColumn enabled = new TableColumn(connectionsTable, SWT.BORDER);
		enabled.setText("Linked Projects");
		enabled.setWidth(gridData.widthHint - addresses.getWidth());

		Button addButton = new Button(composite, SWT.PUSH);
		addButton.setText("Add...");
		gridData = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
		addButton.setLayoutData(gridData);
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent se) {
				Wizard wizard = new NewMicroclimateConnectionWizard(false);
				WizardLauncher.launchWizardWithoutSelection(wizard);
			}
		});

		Button removeButton = new Button(composite, SWT.PUSH);
		removeButton.setText("Remove");
		gridData = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
		removeButton.setLayoutData(gridData);
		removeButton.addSelectionListener(new SelectionAdapter() {
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
					// TODO give them the option to delete the servers for them
					MessageDialog.openError(getShell(), "Could not remove connection",
							"At least one of the selected connections has projects linked in the workspace.\n"
									+ "Unlink any projects by deleting the corresponding servers "
									+ "before removing the Microclimate Connection.");
				}

				refreshConnectionsList();
			}
		});

		connectionsTable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				int count = connectionsTable.getSelectionCount();
				if (count > 0) {
					removeButton.setEnabled(true);
				} else {
					removeButton.setEnabled(false);
				}
			}
		});

		refreshConnectionsList();
		addButton.setEnabled(true);
		removeButton.setEnabled(false);

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

		return linkedAppsBuilder.length() > 0 ? linkedAppsBuilder.toString() : "none";
	}
}
