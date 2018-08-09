package com.ibm.microclimate.ui.wizards;

import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.ibm.microclimate.core.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.MicroclimateConnection;
import com.ibm.microclimate.core.internal.MicroclimateConnectionManager;
import com.ibm.microclimate.ui.prefs.MicroclimateConnectionPrefsPage;

public class LinkMicroclimateProjectPage extends WizardPage {

	// User's currently selected Microclimate Connection - can be null
	private MicroclimateConnection mcConnection;
	private List<MicroclimateApplication> mcApps;

	private Combo connectionsCombo;
	private Table projectsTable;

	protected LinkMicroclimateProjectPage() {
		super("Link Microclimate Project");
		setTitle("Link Microclimate Project Title");
		setDescription("Link Microclimate Project Description");
	}

	@Override
	public void createControl(Composite parent) {
		Composite shell = new Composite(parent, SWT.NULL);
		shell.setLayout(new GridLayout(4, false));

		Label connectionsLabel = new Label(shell, SWT.BORDER);
		connectionsLabel.setText("Microclimate Connection:");
		connectionsLabel.setLayoutData(new GridData(GridData.END, GridData.BEGINNING, false, false));

		connectionsCombo = new Combo(shell, SWT.READ_ONLY);
		connectionsCombo.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false));

		connectionsCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent se) {
				// Combo combo = (Combo) se.getSource();
				setMCConnection();
			}
		});

		populateConnectionsCombo();
		// Initially select the first mcConnection
		connectionsCombo.select(0);
		setMCConnection();

		Button refreshProjectsBtn = new Button(shell, SWT.NONE);
		refreshProjectsBtn.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
		refreshProjectsBtn.setText("Refresh Projects");

		refreshProjectsBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent se) {
				populateProjectsTable();
			}
		});

		// Label spacer = new Label(shell, SWT.NONE);

		Button manageConnectionsBtn = new Button(shell, SWT.BORDER);
		manageConnectionsBtn.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
		manageConnectionsBtn.setText("Manage Connections");

		manageConnectionsBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent se) {
				PreferenceDialog prefsDialog = PreferencesUtil
						.createPreferenceDialogOn(getShell(), MicroclimateConnectionPrefsPage.PAGE_ID, null, null);
				prefsDialog.setBlockOnOpen(true);
				prefsDialog.open();
			}
		});

		// just for spacing
		new Label(shell, SWT.NONE);

		projectsTable = new Table(shell, SWT.BORDER | SWT.SINGLE);
		GridData tableLayout = new GridData(GridData.BEGINNING, GridData.CENTER, true, true, 4, 1);
		tableLayout.minimumWidth = 600;
		tableLayout.minimumHeight = 200;
		projectsTable.setLayoutData(tableLayout);
		projectsTable.setHeaderVisible(true);
		projectsTable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent se) {
				// Selecting a project allows the user to proceed in the wizard.
				getWizard().getContainer().updateButtons();
				// MCLogger.log("Now selected project #" + projectsTable.getSelectionIndex());
			}
		});

		TableColumn nameColumn = new TableColumn(projectsTable, SWT.BORDER);
		nameColumn.setText("Project Name");
		nameColumn.setWidth((int)(tableLayout.minimumWidth / 2.75));
		TableColumn languageColumn = new TableColumn(projectsTable, SWT.BORDER);
		languageColumn.setText("Language");
		languageColumn.setWidth(nameColumn.getWidth() / 2);
		TableColumn urlColumn = new TableColumn(projectsTable, SWT.BORDER);
		urlColumn.setText("URL");
		urlColumn.setWidth(tableLayout.minimumWidth - nameColumn.getWidth() - languageColumn.getWidth());

		// Since we called buildConnectionsCombo already, mcConnection must be set
		populateProjectsTable();

		new Label(shell, SWT.NONE);

		com.ibm.microclimate.core.Activator.getDefault().getPreferenceStore()
		.addPropertyChangeListener(event -> {
		    if (event.getProperty() == MicroclimateConnectionManager.CONNECTION_LIST_PREFSKEY
		    		&& !connectionsCombo.isDisposed()) {
		    	populateConnectionsCombo();
		    	populateProjectsTable();
		    }
		});

		shell.pack();
		setControl(shell);
	}

	private void populateConnectionsCombo() {
		int previousCount = connectionsCombo.getItemCount();
		connectionsCombo.removeAll();

		List<MicroclimateConnection> connections = MicroclimateConnectionManager.connections();
		if(connections.isEmpty()) {
			mcConnection = null;
			return;
		}

		for(MicroclimateConnection mcc : connections) {
			connectionsCombo.add(mcc.baseUrl());
		}


		if(previousCount == 0) {
			// Previously it was empty. Now, it is not empty. So we should automatically select the first item.
			connectionsCombo.select(0);
			setMCConnection();
		}
	}

	/**
	 * Update the wizard's current mcConnection based on which one is selected in the connectionsCombo
	 */
	private void setMCConnection() {
		String selected = connectionsCombo.getText();
		MicroclimateConnection connection = MicroclimateConnectionManager.getConnection(selected);

		if(connection == null) {
			MCLogger.logError("Failed to get MCConnection object from selected URL: " + selected);
		}
		mcConnection = connection;
	}

	/**
	 * Using the existing mcConnection, populate the projects table with a list of projects from that connection
	 */
	private void populateProjectsTable() {
		projectsTable.removeAll();

		if(mcConnection == null) {
			showNoConnectionsMsg();
			return;
		}

		// Cache the mcApps here so that we can be sure the contents of mcApps match the contents of the table
		try {
			mcApps = mcConnection.apps();
		}
		catch(Exception e) {
			MCLogger.logError(e);
			MessageDialog.openError(getShell(), "Error getting project list", e.getMessage());
		}

		for(MicroclimateApplication mcApp : mcApps) {
			TableItem ti = new TableItem(projectsTable, SWT.NONE);

			String lang = mcApp.language;
			// uppercase the first letter because it looks nicer
			lang = lang.substring(0, 1).toUpperCase() + lang.substring(1);

			ti.setText(new String[] { mcApp.name, lang, mcApp.rootUrl.toString() });
		}
	}

	private void showNoConnectionsMsg() {
		MessageDialog.openError(getShell(), "No Active Microclimate Connections",
				"You must create and select a Microclimate connection "
				+ "in order to import projects from Microclimate. "
				+ "Click \"Managed Connections\" to add a new connection.");
	}

	MicroclimateApplication getSelectedApp() {
		int selectionIndex = projectsTable.getSelectionIndex();
		if(selectionIndex != -1) {
			return mcApps.get(selectionIndex);
		}
		return null;
	}

	boolean canFinish() {
		// Can finish if any project is selected
		return getSelectedApp() != null;
	}
}