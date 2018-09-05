package com.ibm.microclimate.ui.internal.wizards;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PreferencesUtil;

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

	private Combo connectionsCombo;
	private Combo projectsCombo;

	private Composite projToLinkInfoParent;

	private Label mcProjInfoTitle;
	private Button refreshProjectsBtn;

	private Label projInfoName;
	private Label projInfoType;
	private Label projInfoUrl;
	private Label projInfoPath;

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

		final int parentGridWidth = 3;
		final int firstColWidth = 100;

		parent.setLayout(new GridLayout(parentGridWidth, false));
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.minimumWidth = firstColWidth;
		parent.setLayoutData(gridData);

		Label connectionsLabel = new Label(parent, SWT.NONE);
		connectionsLabel.setText("Microclimate Connection:");
		connectionsLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		connectionsCombo = new Combo(parent, SWT.READ_ONLY);
		gridData = new GridData(GridData.FILL, GridData.FILL, true, false);
		connectionsCombo.setLayoutData(gridData);

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
		manageConnectionsBtn.setText("Manage Connections");
		GridData buttonData = new GridData(GridData.FILL, GridData.CENTER, true, false);
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

		projectsCombo = new Combo(parent, SWT.READ_ONLY);
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

		refreshProjectsBtn = new Button(parent, SWT.PUSH);
		refreshProjectsBtn.setText("Refresh Project Info");
		refreshProjectsBtn.setLayoutData(buttonData);

		refreshProjectsBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent se) {
				populateAppToLinkDetails();
			}
		});

		// Create the bottom part of the wizard which contains info about the selected project,
		// if the project has a corresponding project in Microclimate.
		projToLinkInfoParent = new Composite(parent, SWT.NONE);
		projToLinkInfoParent.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, parentGridWidth, 1));
		projToLinkInfoParent.setLayout(new GridLayout(parentGridWidth, false));

		final Font boldFont = FontDescriptor.createFrom(getFont())
				.setStyle(SWT.BOLD)
				.createFont(parent.getDisplay());

		createProjInfoLabel("Name:", firstColWidth, boldFont);
		projInfoName = new Label(projToLinkInfoParent, SWT.NONE);
		GridData infoData = new GridData(GridData.BEGINNING, GridData.FILL, true, false, 2, 1);
		projInfoName.setLayoutData(infoData);

		createProjInfoLabel("Type:", firstColWidth, boldFont);
		projInfoType = new Label(projToLinkInfoParent, SWT.NONE);
		projInfoType.setLayoutData(infoData);

		createProjInfoLabel("URL:", firstColWidth, boldFont);
		projInfoUrl = new Label(projToLinkInfoParent, SWT.NONE);
		projInfoUrl.setLayoutData(infoData);

		createProjInfoLabel("Path:", firstColWidth, boldFont);
		projInfoPath = new Label(projToLinkInfoParent, SWT.NONE);
		projInfoPath.setLayoutData(infoData);

		populateAppToLinkDetails();

		com.ibm.microclimate.core.Activator.getDefault().getPreferenceStore()
		.addPropertyChangeListener(new IPropertyChangeListener() {
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
	private Label createProjInfoLabel(String text, int width, Font font) {
		Label label = new Label(projToLinkInfoParent, SWT.NONE);
		label.setText(text);
		label.setAlignment(SWT.RIGHT);
		label.setFont(font);

		GridData gridData = new GridData(GridData.END, GridData.FILL, true, false);
		gridData.minimumWidth = width;
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
		projToLinkInfoParent.setVisible(hasApp);
		refreshProjectsBtn.setEnabled(hasApp);

		if (hasApp) {
			// Populate the app info label

			MCLogger.log("Found app with matching path " + appToLink.name);

			mcProjInfoTitle.setText(selectedProject.getName() +
					" will be linked to the following Microclimate project:");

			String baseUrl = "Not running";
			if (appToLink.getBaseUrl() != null) {
				baseUrl = appToLink.getBaseUrl().toString();
			}

			projInfoName.setText(appToLink.name);
			projInfoType.setText(appToLink.getUserFriendlyType());
			projInfoUrl.setText(baseUrl);
			projInfoPath.setText(appToLink.fullLocalPath.toString());
		}
		else {
			// There is no corresponding project in Microclimate.
			mcProjInfoTitle.setText("");
		}

		projToLinkInfoParent.layout();

		// Refresh the error message and wizard buttons
		String notLinkableReason = getAppNotLinkableMsg(appToLink, selectedProject);
		setErrorMessage(notLinkableReason);

		getWizard().getContainer().updateButtons();
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