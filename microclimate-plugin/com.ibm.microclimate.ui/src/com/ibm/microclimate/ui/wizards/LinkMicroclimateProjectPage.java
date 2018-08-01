package com.ibm.microclimate.ui.wizards;

import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
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

import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.MicroclimateConnection;
import com.ibm.microclimate.core.internal.MicroclimateConnectionManager;

public class LinkMicroclimateProjectPage extends WizardPage {
	
	private MicroclimateConnection mcConnection;
	private List<MicroclimateApplication> mcApps;
	
	private Table projectsTable;

	protected LinkMicroclimateProjectPage() {
		super("Link Microclimate Project");
		setTitle("Link Microclimate Project Title");
		setDescription("Link Microclimate Project Description");
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite shell = new Composite(parent, SWT.NULL);
		shell.setLayout(new GridLayout(4, true));
		
		Label connectionsLabel = new Label(shell, SWT.BORDER);
		connectionsLabel.setText("Microclimate Connection:");
		connectionsLabel.setLayoutData(new GridData(GridData.END, GridData.BEGINNING, false, false));
		
		Combo connectionsCombo = new Combo(shell, SWT.READ_ONLY);
		connectionsCombo.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false));
		
		connectionsCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent se) {
				Combo combo = (Combo) se.getSource();
				setMCConnection(combo);
			}
		});		

		populateConnectionsCombo(connectionsCombo);
		// Initially select the first mcConnection
		connectionsCombo.select(0);
		setMCConnection(connectionsCombo);
		
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
		manageConnectionsBtn.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
		manageConnectionsBtn.setText("Manage Connections");
		
		manageConnectionsBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent se) {
				MessageDialog.openInformation(getShell(), "Manage Connections", 
						"Manage Connections Preferences Page goes here");
			}
		});
		
		Label spacer = new Label(shell, SWT.NONE);
		
		projectsTable = new Table(shell, SWT.BORDER | SWT.SINGLE);
		GridData tableLayout = new GridData(GridData.BEGINNING, GridData.CENTER, true, true, 2, 1);
		tableLayout.minimumWidth = 400;
		tableLayout.minimumHeight = 200;
		projectsTable.setLayoutData(tableLayout);
		projectsTable.setHeaderVisible(true);
		projectsTable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent se) {
				// Selecting a project allows the user to proceed in the wizard.
				getWizard().getContainer().updateButtons();
				System.out.println("Now selected project #" + projectsTable.getSelectionIndex());
			}
		});
		
		TableColumn nameColumn = new TableColumn(projectsTable, SWT.BORDER);
		nameColumn.setText("Project Name");
		nameColumn.setWidth((int)(tableLayout.minimumWidth / 1.33));
		TableColumn languageColumn = new TableColumn(projectsTable, SWT.BORDER);
		languageColumn.setText("Language");
		languageColumn.setWidth(tableLayout.minimumWidth - nameColumn.getWidth());
		
		// Since we called buildConnectionsCombo already, mcConnection must be set
		populateProjectsTable();
		
		Label spacer2 = new Label(shell, SWT.NONE);
		
		shell.pack();
		setControl(shell);
	}
	
	private void populateConnectionsCombo(Combo connectionsCombo) {
		List<MicroclimateConnection> connections = MicroclimateConnectionManager.connections();
		if(connections.isEmpty()) {
			System.err.println("ERROR no connections but there should be at least one!!");
		}
		
		for(MicroclimateConnection mcc : connections) {
			connectionsCombo.add(mcc.baseUrl());
		}
	}
	
	/**
	 * Using the existing mcConnection, populate the projects table with a list of projects from that connection
	 */
	private void populateProjectsTable() {
		// Cache the mcApps here so that we can be sure the contents of mcApps match the contents of the table
		mcApps = mcConnection.apps();
		projectsTable.removeAll();
		
		if(mcConnection == null) {
			System.err.println("ERROR: Tried to populate projects table before setting mcConnection");
			return;
		}
		
		for(MicroclimateApplication mcApp : mcApps) {
			TableItem ti = new TableItem(projectsTable, SWT.NONE);
			
			String lang = mcApp.language();
			// uppercase the first letter because it looks nicer
			lang = lang.substring(0, 1).toUpperCase() + lang.substring(1);			
			ti.setText(new String[] { mcApp.name(), lang });
		}
	}
	
	private void setMCConnection(Combo mcConnectionSelector) {
		MicroclimateConnection connection = MicroclimateConnectionManager.getConnection(mcConnectionSelector.getText());
		
		if(connection == null) {
			System.err.println("Null MCConnection! How?!");
		}
		else {
			mcConnection = connection;
		}
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