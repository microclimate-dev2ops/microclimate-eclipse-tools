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
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.ibm.microclimate.core.MicroclimateCorePlugin;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
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
	private MicroclimateApplication appToLink;

	private IProject selectedProject;

	private Composite parent;

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

	protected LinkMicroclimateProjectPage(IProject selectedProject) {
		super("Link to Microclimate Project");

		this.selectedProject = selectedProject;
		setCustomTitle();

		setDescription("Select the Eclipse project you wish to link to a Microclimate project.");
	}

	private void setCustomTitle() {
		setTitle("Link " + selectedProject.getName() + " to Microclimate Project");
	}

	@Override
	public void createControl(Composite parent) {
		// getShell().setSize(600, 400);

		this.parent = parent;

		final int parentGridWidth = 3;

		GridLayout parentLayout = new GridLayout(parentGridWidth, false);
		// parentLayout.horizontalSpacing = SWTUtil.convertHorizontalDLUsToPixels(parent, 4);
		// parentLayout.verticalSpacing = SWTUtil.convertVerticalDLUsToPixels(parent, 4);
		// parentLayout.marginWidth = 0;
		// parentLayout.marginHeight = 0;
		parent.setLayout(parentLayout);
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label connectionsLabel = new Label(parent, SWT.NONE);
		connectionsLabel.setText("Microclimate Connection:");

		final GridData labelData = new GridData(GridData.FILL, GridData.CENTER, false, false);
		connectionsLabel.setLayoutData(labelData);

		connectionsCombo = new Combo(parent, SWT.READ_ONLY);
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

		Button manageConnectionsBtn = new Button(parent, SWT.PUSH);
		manageConnectionsBtn.setText("Manage");
		final GridData buttonData = new GridData(GridData.CENTER, GridData.CENTER, false, false);
		buttonData.widthHint = 125;
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

		Label eclipseProj = new Label(parent, SWT.NONE);
		eclipseProj.setText("Eclipse Project:");
		eclipseProj.setLayoutData(labelData);

		projectsCombo = new Combo(parent, SWT.READ_ONLY);
		projectsCombo.setLayoutData(comboData);
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
				}
			}
		});

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

		Label spacer = new Label(parent, SWT.NONE);
		spacer.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1));;

		// new row
		mcProjInfoTitle = new Label(parent, SWT.NONE);
		mcProjInfoTitle.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 2, 1));
		mcProjInfoTitle.setText("The selected project will be linked to the following Microclimate project:");

		refreshProjectsBtn = new Button(parent, SWT.PUSH);
		refreshProjectsBtn.setText("Refresh");
		refreshProjectsBtn.setLayoutData(buttonData);

		refreshProjectsBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent se) {
				populateAppToLinkDetails();
			}
		});

		// Create the bottom part of the wizard which contains info about the selected project,
		// if the project has a corresponding project in Microclimate.

		/*
		final Font boldFont = FontDescriptor.createFrom(getFont())
				.setStyle(SWT.BOLD)
				.createFont(parent.getDisplay());
		*/

		projInfoNameLabel = createProjInfoLabel(parent, "Name:");
		projInfoName = new Label(parent, SWT.NONE);
		GridData infoData = new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1);
		projInfoName.setLayoutData(infoData);

		projInfoTypeLabel = createProjInfoLabel(parent, "Type:");
		projInfoType = new Label(parent, SWT.NONE);
		projInfoType.setLayoutData(infoData);

		projInfoUrlLabel = createProjInfoLabel(parent, "URL:");
		projInfoUrl = new Label(parent, SWT.NONE);
		projInfoUrl.setLayoutData(infoData);

		projInfoPathLabel = createProjInfoLabel(parent, "Path:");

		populateAppToLinkDetails();

		MicroclimateCorePlugin.getDefault().getPreferenceStore().addPropertyChangeListener(
				new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
			    if (event.getProperty().equals(MicroclimateConnectionManager.CONNECTION_LIST_PREFSKEY)
			    		&& !connectionsCombo.isDisposed()) {
			    	populateConnectionsCombo();
			    	populateAppToLinkDetails();
			    }
			}
		});

		parent.pack();
		setControl(parent);
	}

	/**
	 * Create a project info label with the given text. These all have the same style.
	 */
	private Label createProjInfoLabel(Composite parent, String text) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(text);
		label.setAlignment(SWT.LEFT);
		//label.setFont(font);

		GridData gridData = new GridData(GridData.END, GridData.FILL, true, false);
		//gridData.horizontalIndent = 150;		// arbitrary indent
		label.setLayoutData(gridData);

		return label;
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

		if(previousCount == 0 || connectionsCombo.getItemCount() == 1) {
			// automatically select the first item.
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

			String path = appToLink.fullLocalPath.toString();

			// The path section can be split into multiple lines (over multiple GridLayout rows) if the path is too long

			// arbitrary max path length that looks good
			final int pathLineLength = 64;

			String[] subPaths = MCUtil.splitStringIntoArray(path, pathLineLength);

			projInfoPaths = new Text[subPaths.length];
			projInfoSpacers = new Label[subPaths.length - 1];

			GridData spacerData = new GridData(GridData.HORIZONTAL_ALIGN_FILL, GridData.FILL, true, false);
			GridData pathData = new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1);

			for (int i = 0; i < subPaths.length; i++) {
				// we don't need a spacer for the first line because the "Path:" label is there
				if (i > 0) {
					Label spacer = new Label(parent, SWT.NONE);
					spacer.setLayoutData(spacerData);
					projInfoSpacers[i - 1] = spacer;
				}
				Text text = new Text(parent, SWT.READ_ONLY);
				text.setText(subPaths[i]);
				text.setLayoutData(pathData);
				text.setBackground(parent.getBackground());
				text.clearSelection();
				projInfoPaths[i] = text;
			}
		}

		// forces a redraw of the new labels and texts
		parent.layout();

		// Refresh the error message and wizard buttons
		// if notLinkableReason is null, the user can finish the wizard.
		String notLinkableReason = getAppNotLinkableMsg(appToLink, selectedProject);
		setErrorMessage(notLinkableReason);

		getWizard().getContainer().updateButtons();
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
		return getErrorMessage() == null;
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
					+ "\n Make sure it is enabled and started, then refresh the project info.";
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