package com.ibm.microclimate.ui.internal.wizards;

import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.SearchPattern;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.MicroclimateConnection;
import com.ibm.microclimate.core.internal.MicroclimateConnectionManager;
import com.ibm.microclimate.ui.internal.prefs.MicroclimateConnectionPrefsPage;

/**
 * This wizard page allows the user to select a Microclimate Connection, then displays a list of applications running
 * on that MC instance. The user can then select one of these apps to link to the selected Eclipse project.
 *
 * @author timetchells@ibm.com
 *
 */
public class LinkMicroclimateProjectPage extends WizardPage {

	// User's currently selected Microclimate Connection - can be null
	private MicroclimateConnection mcConnection;
	// List of Applications from the current mcConnection
	private List<MicroclimateApplication> mcApps;

	private final String selectedProjectName;

	private Combo connectionsCombo;
	private Table projectsTable;
	private Text filterText;

	protected SearchPattern pattern = new SearchPattern(SearchPattern.RULE_PATTERN_MATCH | SearchPattern.RULE_PREFIX_MATCH | SearchPattern.RULE_BLANK_MATCH);

	protected LinkMicroclimateProjectPage(String selectedProjectName) {
		super("Link " + selectedProjectName + " to Microclimate Project");
		setTitle("Link " + selectedProjectName + " to Microclimate Project");
		setDescription("Link your Eclipse project to a project in Microclimate");

		this.selectedProjectName = selectedProjectName;
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout(3, false));
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		composite.setLayoutData(gridData);

		Label connectionsLabel = new Label(composite, SWT.NONE);
		connectionsLabel.setText("Microclimate Connection:");
		gridData = new GridData(SWT.FILL, SWT.CENTER, false, false);
		connectionsLabel.setLayoutData(gridData);

		connectionsCombo = new Combo(composite, SWT.READ_ONLY);
		gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		connectionsCombo.setLayoutData(gridData);

		connectionsCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent se) {
				// Combo combo = (Combo) se.getSource();
				setMCConnection();
			}
		});

		Button manageConnectionsBtn = new Button(composite, SWT.PUSH);
		manageConnectionsBtn.setText("Manage Connections");
		gridData = new GridData(SWT.FILL, SWT.FILL, false, false);
		manageConnectionsBtn.setLayoutData(gridData);

		manageConnectionsBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent se) {
				PreferenceDialog prefsDialog = PreferencesUtil
						.createPreferenceDialogOn(getShell(), MicroclimateConnectionPrefsPage.PAGE_ID, null, null);
				prefsDialog.setBlockOnOpen(true);
				prefsDialog.open();
			}
		});

		populateConnectionsCombo();
		// Initially select the first mcConnection
		connectionsCombo.select(0);
		setMCConnection();

		// Add spacing
		Label spacer = new Label(composite, SWT.NONE);
		gridData = new GridData(GridData.FILL, GridData.FILL, false, false, 3, 1);
		spacer.setLayoutData(gridData);

		Label projectsLabel = new Label(composite, SWT.NONE);
		projectsLabel.setText("Select project:");
		gridData = new GridData(GridData.FILL, GridData.CENTER, false, false, 3, 1);
		projectsLabel.setLayoutData(gridData);

		Composite projectsComposite = new Composite(composite, SWT.NONE);
		projectsComposite.setLayout(new GridLayout(2, false));
		gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
		projectsComposite.setLayoutData(gridData);

        filterText = new Text(projectsComposite, SWT.SEARCH);
        gridData = new GridData(GridData.FILL, GridData.CENTER, true, false, 1, 1);
        filterText.setLayoutData(gridData);
        filterText.setMessage("type filter text");

        // Use up the second column
        new Label(projectsComposite, SWT.NONE);

		projectsTable = new Table(projectsComposite, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		projectsTable.setLinesVisible(true);
		projectsTable.setHeaderVisible(true);
		gridData = new GridData(GridData.FILL, GridData.FILL, true, true);
		gridData.minimumWidth = 600;
		gridData.minimumHeight = 200;
		projectsTable.setLayoutData(gridData);
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
		nameColumn.setWidth((int)(gridData.minimumWidth / 2.75));
		TableColumn typeColumn = new TableColumn(projectsTable, SWT.BORDER);
		typeColumn.setText("Type");
		typeColumn.setWidth(nameColumn.getWidth() / 2);
		TableColumn urlColumn = new TableColumn(projectsTable, SWT.BORDER);
		urlColumn.setText("URL");
		urlColumn.setWidth(gridData.minimumWidth - nameColumn.getWidth() - typeColumn.getWidth());

		Button refreshProjectsBtn = new Button(projectsComposite, SWT.PUSH);
		refreshProjectsBtn.setText("Refresh Projects");
		gridData = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
		refreshProjectsBtn.setLayoutData(gridData);

		refreshProjectsBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent se) {
				populateProjectsTable();
			}
		});

        filterText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                populateProjectsTable();
            }
        });

		// Since we called buildConnectionsCombo already, mcConnection must be set
		populateProjectsTable();

		com.ibm.microclimate.core.Activator.getDefault().getPreferenceStore()
		.addPropertyChangeListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
			    if (event.getProperty() == MicroclimateConnectionManager.CONNECTION_LIST_PREFSKEY
			    		&& !connectionsCombo.isDisposed()) {
			    	populateConnectionsCombo();
			    	populateProjectsTable();
			    }
			}
		});

		composite.pack();
		setControl(composite);
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
			connectionsCombo.add(mcc.baseUrl);
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
			// Don't display this if the Preferences page is already open - ie only display it
			// if this wizard is the active shell.
			if (Display.getDefault().getActiveShell().equals(getShell())) {
				showNoConnectionsMsg();
			}
			return;
		}

		// Cache the mcApps here so that we can be sure the contents of mcApps match the contents of the table
		mcApps = mcConnection.getApps();
		// Sort the apps so that unlinkable apps show up at the bottom of the table.
		mcApps.sort((app1, app2) -> {
			if (app1.isLinkable()) {
				if (app2.isLinkable()) {
					// both are linkable
					return 0;
				}
				else {
					// app1 should be sorted first
					return -1;
				}
			}
			else if(app2.isLinkable()) {
				// app2 should be sorted first
				return 1;
			}
			else {
				// neither are linkable
				return 0;
			}
		});

		String filter = filterText.isDisposed() ? null : filterText.getText();

		if (filter != null && !filter.isEmpty()) {
			pattern.setPattern("*" + filter + "*");
		}
		for(MicroclimateApplication app : mcApps) {
			if (filter != null && !filter.isEmpty() && !pattern.matches(app.name)) {
				continue;
			}
			TableItem ti = new TableItem(projectsTable, SWT.NONE);

			String type = app.projectType;
			// uppercase the first letter because it looks nicer
			type = type.substring(0, 1).toUpperCase() + type.substring(1);

			String baseUrlStr;
			if (app.isRunning()) {
				baseUrlStr = app.getBaseUrl().toString();
			}
			else {
				baseUrlStr = "Not Running";
			}

			ti.setText(new String[] { app.name, type, baseUrlStr });

			// Gray out invalid projects
			if (!app.isLinkable()) {
				ti.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
			}
		}

		// Help the user by selecting a project initially
		TableItem[] items = projectsTable.getItems();
		// Select the first one by default
		if (items.length > 0) {
			projectsTable.select(0);
		}
		// Select the project in the table whose name matches the project that was used to launch this wizard
		for (int i = 0; i < items.length; i++) {
			TableItem ti = items[i];
			if (ti.getText(0).equals(selectedProjectName)) {
				projectsTable.select(i);
				break;
			}
		}
	}

	private void showNoConnectionsMsg() {
		MessageDialog.openError(getShell(), "No Active Microclimate Connections",
				"You must create and select a Microclimate connection "
				+ "in order to import projects from Microclimate. "
				+ "Click \"Manage Connections\" to add a new connection.");
	}

	MicroclimateApplication getSelectedApp() {
		int selectionIndex = projectsTable.getSelectionIndex();
		if(selectionIndex != -1) {
			return mcApps.get(selectionIndex);
		}
		return null;
	}

	boolean canFinish() {
		// Can finish if any valid project is selected
		MicroclimateApplication selectedApp = getSelectedApp();
		if (selectedApp != null) {
			if (selectedApp.isLinkable()) {
				setErrorMessage(null);
				return true;
			}
			// Check out MicroclimateApplication.isLinkable for reasons why this project is not valid,
			// and give messages for each possible reason.
			else if (selectedApp.isLinked()) {
				setErrorMessage("Invalid project selected - This project is already linked to server \""
						+ selectedApp.getLinkedServer().getServer().getName() + "\".");
			}
			else if (!selectedApp.isRunning()) {
				// TODO this really shouldn't be a problem. A user could create a server for a stopped project,
				// but then we'd have to give them a way to start the project from Eclipse.
				setErrorMessage("Invalid project selected - This project is not running. "
						+ "Make sure it is not disabled, wait for it to start, and refresh the list.");
			}
			else if (!selectedApp.isLibertyProject()) {
				setErrorMessage("Invalid project selected - Only Liberty projects are supported at this time.");
			}
			else {
				// should never happen - handle all possible reasons for invalidity above
				setErrorMessage("Invalid project selected");
			}
		}

		return false;
	}
}