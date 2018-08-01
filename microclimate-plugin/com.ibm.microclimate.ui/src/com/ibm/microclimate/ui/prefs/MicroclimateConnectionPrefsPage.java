package com.ibm.microclimate.ui.prefs;

import java.util.List;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
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

import com.ibm.microclimate.core.internal.MicroclimateConnection;
import com.ibm.microclimate.core.internal.MicroclimateConnectionManager;
import com.ibm.microclimate.ui.wizards.NewMicroclimateConnectionWizard;
import com.ibm.microclimate.ui.wizards.WizardUtil;

public class MicroclimateConnectionPrefsPage extends PreferencePage implements IWorkbenchPreferencePage {
	
	public static final String MC_CONNECTIONS_PREFSKEY = "com.ibm.microclimate.ui.prefs.connections";

	private static MicroclimateConnectionPrefsPage instance;
	
	private Table connectionsTable;
	
	private List<MicroclimateConnection> connections;
	
	public MicroclimateConnectionPrefsPage() {
		if(instance != null) {
			// TODO figure out if this class can be used as a singleton or not
			System.err.println("ERROR: Multiple instances of supposed singleton");
		}
		instance = this;
	}
	
	public static MicroclimateConnectionPrefsPage instance() {
		return instance;
	}

	@Override
	public void init(IWorkbench arg0) {
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
				WizardUtil.launchWizardWithoutSelection(wizard);
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
				int[] selected = connectionsTable.getSelectionIndices();
				for(int i : selected) {
					if(!MicroclimateConnectionManager.remove(connections.get(i))) {
						System.err.println("Connection with index " + i + " was already removed");
					}
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
		addresses.setText("Address");
		addresses.setWidth(tableGridData.widthHint / 2);
		
		/*
		TableColumn lastUsed = new TableColumn(connectionsTable, SWT.BORDER);
		lastUsed.setText("Last Used");
		lastUsed.setWidth(tableGridData.widthHint / 3);
		*/
		
		TableColumn enabled = new TableColumn(connectionsTable, SWT.BORDER);
		enabled.setText("Any other info?");
		enabled.setWidth(tableGridData.widthHint - addresses.getWidth());		
		
		return parent;
	}
	
	/**
	 * Rather than having other classes call this, it would be better if there was a way to run this refresh 
	 * whenever the prefs window regained focus - 
	 * or of course just add a Refresh button, but that would be annoying for a user.
	 */
	public void refreshConnectionsList() {
		// Update the cached connections when we update the table, so that they always match
		connections = MicroclimateConnectionManager.connections();
		
		connectionsTable.removeAll();
		
		for(MicroclimateConnection mcc : connections) {
			TableItem ti = new TableItem(connectionsTable, SWT.NONE);
			ti.setText(new String[] { mcc.baseUrl(), "???" });
		}
	}
}
