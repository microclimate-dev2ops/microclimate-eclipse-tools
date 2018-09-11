/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.wizards;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.ibm.microclimate.core.MicroclimateCorePlugin;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnectionManager;
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
	private MicroclimateApplication appToLink;

	private IProject selectedProject;
	private boolean includeConnectionWidgets = false;

	private Composite parent;
	private Composite projectComposite;

	private Combo connectionsCombo;
	private Combo projectsCombo;

	private Label mcProjInfoTitle;
	private Button refreshProjectsBtn;

	private Label projInfoNameLabel;
	private Label projInfoTypeLabel;
	private Label projInfoUrlLabel;
	private Label projInfoPathLabel;

	private Label projInfoName;
	private Label projInfoType;
	private Label projInfoUrl;

	private Text[] projInfoPaths = new Text[0];
	private Label[] projInfoSpacers = new Label[0];

	protected LinkMicroclimateProjectPage(IProject selectedProject, boolean includeConnectionWidgets) {
		super("Link to Microclimate Project");

		this.selectedProject = selectedProject;
		this.includeConnectionWidgets = includeConnectionWidgets;
		setCustomTitle();

		setDescription("Select the Eclipse project you wish to link to a Microclimate project.");
	}

	private void setCustomTitle() {
		setTitle("Link " + selectedProject.getName() + " to Microclimate Project");
	}

	@Override
	public void createControl(Composite parent) {
		// Give a minimum size that fits everything comfortably but doesn't require resizing when components change.
		getShell().setMinimumSize(600, 450);

		final int gridWidth = 3;

		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(gridWidth, false);
		layout.verticalSpacing = 8;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		this.parent = composite;

		if (includeConnectionWidgets) {
			Label connectionsLabel = new Label(composite, SWT.NONE);
			connectionsLabel.setText("Microclimate Connection:");
			connectionsLabel.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));

			connectionsCombo = new Combo(composite, SWT.READ_ONLY);
			final GridData comboData = new GridData(GridData.FILL, GridData.FILL, true, false);
			connectionsCombo.setLayoutData(comboData);

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

			Button manageConnectionsBtn = new Button(composite, SWT.PUSH);
			manageConnectionsBtn.setText("Manage");
			final GridData buttonData = new GridData(GridData.FILL, GridData.FILL, false, false);
			buttonData.widthHint = 100;
			manageConnectionsBtn.setLayoutData(buttonData);

			manageConnectionsBtn.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent se) {
					PreferenceDialog prefsDialog = PreferencesUtil
							.createPreferenceDialogOn(getShell(), MicroclimateConnectionPrefsPage.PAGE_ID, null, null);
					prefsDialog.setBlockOnOpen(true);
					prefsDialog.open();
				}
			});

			Label spacer = new Label(composite, SWT.NONE);
			spacer.setText("");
			spacer.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 3, 1));
		}

		Label eclipseProj = new Label(composite, SWT.NONE);
		eclipseProj.setText("Eclipse Project:");
		eclipseProj.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));

		projectsCombo = new Combo(composite, SWT.READ_ONLY);
		projectsCombo.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
		projectsCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent se) {
				// user has selected a different project
				String projectName = projectsCombo.getText();

				selectedProject = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
				MCLogger.log("New selected project is " + selectedProject.getName());
				if (selectedProject == null) {
					MCLogger.logError("Eclipse project with name " + projectName + " not found!");
				}
				else {
					setCustomTitle();
					populateAppToLinkDetails();
					getWizard().getContainer().updateButtons();
				}
			}
		});

		Label spacer = new Label(composite, SWT.NONE);
		spacer.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false));;

		mcProjInfoTitle = new Label(composite, SWT.NONE);
		mcProjInfoTitle.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false, 2, 1));
		mcProjInfoTitle.setText("The selected project will be linked to the following Microclimate project:");

		refreshProjectsBtn = new Button(composite, SWT.PUSH);
		refreshProjectsBtn.setText("Refresh");
		final GridData buttonData = new GridData(GridData.FILL, GridData.FILL, false, false);
		buttonData.widthHint = 100;
		refreshProjectsBtn.setLayoutData(buttonData);

		refreshProjectsBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent se) {
				populateAppToLinkDetails();
				getWizard().getContainer().updateButtons();
			}
		});


		// Create the bottom part of the wizard which contains info about the selected project,
		// if the project has a corresponding project in Microclimate.

		/*
		final Font boldFont = FontDescriptor.createFrom(getFont())
				.setStyle(SWT.BOLD)
				.createFont(parent.getDisplay());
		*/

		projectComposite = new Composite(composite, SWT.BORDER);
		layout = new GridLayout(2, false);
		layout.marginLeft = 15;
		projectComposite.setLayout(layout);
		projectComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));

		projInfoNameLabel = createProjInfoLabel(projectComposite, "Name:");
		projInfoName = new Label(projectComposite, SWT.NONE);
		projInfoName.setText("");
		GridData infoData = new GridData(GridData.FILL, GridData.FILL, true, false);
		projInfoName.setLayoutData(infoData);

		projInfoTypeLabel = createProjInfoLabel(projectComposite, "Type:");
		projInfoType = new Label(projectComposite, SWT.NONE);
		projInfoType.setText("");
		projInfoType.setLayoutData(infoData);

		projInfoUrlLabel = createProjInfoLabel(projectComposite, "URL:");
		projInfoUrl = new Label(projectComposite, SWT.NONE);
		projInfoUrl.setText("");
		projInfoUrl.setLayoutData(infoData);

		projInfoPathLabel = createProjInfoLabel(projectComposite, "Path:");

		populateProjectsCombo();

		// Initially select the project used to launch this wizard.
		if (projectsCombo.getItemCount() > 0) {
			projectsCombo.select(0);
			for (int i = 0; i < projectsCombo.getItemCount(); i++) {
				if (projectsCombo.getItem(i).equals(selectedProject.getName())) {
					projectsCombo.select(i);
				}
			}
		}

		populateAppToLinkDetails();

		if (includeConnectionWidgets) {
			MicroclimateCorePlugin.getDefault().getPreferenceStore().addPropertyChangeListener(
					new IPropertyChangeListener() {

				@Override
				public void propertyChange(PropertyChangeEvent event) {
				    if (event.getProperty().equals(MicroclimateConnectionManager.CONNECTION_LIST_PREFSKEY)
				    		&& !connectionsCombo.isDisposed()) {
				    	populateConnectionsCombo();
				    	populateAppToLinkDetails();
				    	getWizard().getContainer().updateButtons();
				    }
				}
			});
		}

		getShell().pack();
		setControl(composite);
	}

	/**
	 * Create a project info label with the given text. These all have the same style.
	 */
	private Label createProjInfoLabel(Composite parent, String text) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(text);
//		label.setAlignment(SWT.LEFT);
		//label.setFont(font);

		GridData gridData = new GridData(GridData.BEGINNING, GridData.FILL, false, false);
		//gridData.horizontalIndent = 150;		// arbitrary indent
		label.setLayoutData(gridData);

		return label;
	}

	private void populateConnectionsCombo() {
		int previousCount = connectionsCombo.getItemCount();
		connectionsCombo.removeAll();

		List<MicroclimateConnection> connections = MicroclimateConnectionManager.activeConnections();
		if(connections.isEmpty()) {
			mcConnection = null;
			return;
		}

		for(MicroclimateConnection mcc : connections) {
			connectionsCombo.add(mcc.baseUrl.toString());
		}

		if(previousCount == 0 || connectionsCombo.getItemCount() == 1) {
			// automatically select the first item.
			connectionsCombo.select(0);
			setMCConnection();
		}
	}

	void init(MicroclimateConnection connection) {
		if (connection != null) {
			mcConnection = connection;
		}
		if (mcConnection != null) {
			populateAppToLinkDetails();
		}
	}

	/**
	 * Update the wizard's current mcConnection based on which one is selected in the connectionsCombo
	 */
	private void setMCConnection() {
		String selected = connectionsCombo.getText();
		MicroclimateConnection connection = MicroclimateConnectionManager.getActiveConnection(selected);

		if (connection == null) {
			MCLogger.logError("Failed to get MCConnection object from selected URL: " + selected);
		}
		mcConnection = connection;
	}

	private void populateProjectsCombo() {
		projectsCombo.removeAll();

		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			projectsCombo.add(project.getName());
		}
	}

	private void populateAppToLinkDetails() {
		if (selectedProject == null) {
			MCLogger.logError("Null selectedProject");
			// should never happen?
			setErrorMessage("There was an error getting the selected project. Please re-launch the wizard.");
			return;
		}

		appToLink = findAppToLink();

		// Hide the App Info area if no app matches.
		boolean hasApp = appToLink != null;
		refreshProjectsBtn.setEnabled(hasApp);
		toggleInfoLabels(hasApp);

		// Remove the project path labels - these will be redrawn.
		for (Text pathLabel : projInfoPaths) {
			pathLabel.dispose();
		}
		for (Label spacer : projInfoSpacers) {
			spacer.dispose();
		}

		if (hasApp) {
			// Populate the app info label

			MCLogger.log("Found app with matching path " + appToLink.name);

			projInfoName.setText(appToLink.name);
			projInfoType.setText(appToLink.getUserFriendlyType());

			String baseUrl = "Not running";
			if (appToLink.getBaseUrl() != null) {
				baseUrl = appToLink.getBaseUrl().toString();
			}
			projInfoUrl.setText(baseUrl);

			// The path section can be split into multiple lines (over multiple GridLayout rows) if the path is too long

			// arbitrary max path length that looks good
			final int pathLineLength = 80;

			List<String> subPaths = MCUtil.splitPath(appToLink.fullLocalPath, pathLineLength);

			projInfoPaths = new Text[subPaths.size()];
			projInfoSpacers = new Label[subPaths.size() - 1];

			GridData spacerData = new GridData(GridData.FILL, GridData.FILL, false, false);
			GridData pathData = new GridData(GridData.FILL, GridData.FILL, false, false);

			for (int i = 0; i < subPaths.size(); i++) {
				// we don't need a spacer for the first line because the "Path:" label is there
				if (i > 0) {
					Label spacer = new Label(projectComposite, SWT.NONE);
					spacer.setLayoutData(spacerData);
					projInfoSpacers[i - 1] = spacer;
				}
				Text text = new Text(projectComposite, SWT.READ_ONLY);
				text.setText(subPaths.get(i));
				text.setLayoutData(pathData);
				text.setBackground(projectComposite.getBackground());
				text.clearSelection();
				projInfoPaths[i] = text;
			}
		}

		redrawShell();

		// Refresh the error message and wizard buttons
		// if notLinkableReason is null, the user can finish the wizard.
		String notLinkableReason = getAppNotLinkableMsg(appToLink, selectedProject);
		setErrorMessage(notLinkableReason);
	}

	private void toggleInfoLabels(boolean show) {
		mcProjInfoTitle.setVisible(show);

		projInfoNameLabel.setVisible(show);
		projInfoTypeLabel.setVisible(show);
		projInfoUrlLabel.setVisible(show);
		projInfoPathLabel.setVisible(show);

		projInfoName.setVisible(show);
		projInfoType.setVisible(show);
		projInfoUrl.setVisible(show);
		//projInfoPath.setVisible(show);
	}

	private void redrawShell() {
		Shell shell = parent.getShell();
		shell.layout(true, true);
		shell.pack();

		// By setting the minimum size to the new size, we allow the shell to resize itself if it's required
		// to fit everything we need - but we also minimize resizing because the shell will only get bigger
		shell.setMinimumSize(shell.getSize());
	}

	private MicroclimateApplication findAppToLink() {
		if (mcConnection == null) {
			return null;
		}

		IPath eclipseProjPath = selectedProject.getLocation();
		List<MicroclimateApplication> apps = mcConnection.getApps();
		for (MicroclimateApplication app : apps) {
			if (MCUtil.pathEquals(eclipseProjPath, app.fullLocalPath)) {
				return app;
			}
		}
		MCLogger.log("No MC project found matching Eclipse project path: " + eclipseProjPath);
		return null;
	}

	boolean canFinish() {
		// the error message is set by populateAppToLinkDetails
		return mcConnection != null && getErrorMessage() == null;
	}

	/**
	 * Check if the given app is linkable. If it is, return null.
	 * If not, return a user-friendly string describing why it can't be linked.
	 */
	private String getAppNotLinkableMsg(MicroclimateApplication app, IProject project) {
		if (mcConnection == null) {
			return "No Microclimate Connection. Click \"Manage Connections\" to add a new connection.";
		}
		else if (app == null) {
			return "No Microclimate project matching Eclipse project location\n" + project.getLocation();
		}
		else if (app.isLinkable()) {
			return null;
		}
		// Check out MicroclimateApplication.isLinkable for reasons why this project is not valid,
		// and give messages for each possible reason.
		else if (app.isLinked()) {
			return "Invalid project selected - This project is already linked to server \""
					+ app.getLinkedServer().getServer().getName() + "\".";
		}
		else if (!app.isRunning()) {
			// TODO this really shouldn't be a problem. A user could create a server for a stopped project,
			// but then we'd have to give them a way to start the project from Eclipse.
			return "Invalid project selected - This project is not running. "
					+ "\n Make sure it is enabled and started, then click Refresh.";
		}
		else if (!app.isMicroprofileProject()) {
			return "Invalid project selected - Only Microprofile projects are supported at this time.";
		}
		else {
			// should never happen - handle all possible reasons for invalidity above
			return "Invalid project selected";
		}
	}

	public MicroclimateApplication getAppToLink() {
		return appToLink;
	}

	public IProject getSelectedProject() {
		return selectedProject;
	}
}