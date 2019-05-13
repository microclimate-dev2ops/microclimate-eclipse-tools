/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.wizards;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.ibm.microclimate.core.internal.InstallUtil;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.ProcessHelper;
import com.ibm.microclimate.core.internal.ProcessHelper.ProcessResult;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnectionManager;
import com.ibm.microclimate.core.internal.constants.ProjectType;
import com.ibm.microclimate.ui.MicroclimateUIPlugin;

public class LanguageSelectionPage extends WizardPage {

	private MicroclimateConnection connection = null;
	private String language = null;
	private String type = null;

	protected LanguageSelectionPage(MicroclimateConnection connection) {
		super("Select Language");
		setTitle("Language and Type Selection");
		setDescription("Select a language, and if applicable, a project type");
		this.connection = connection;
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.horizontalSpacing = 5;
        layout.verticalSpacing = 7;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        if (connection == null) {
	        setupConnection();
			if (connection == null) {
				setErrorMessage("Could not connect to Codewind. Check logs for more information.");
				setControl(composite);
				return;
			}
        }
        
        Text languageLabel = new Text(composite, SWT.READ_ONLY);
        languageLabel.setText("Choose the project language:");
        languageLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
        
        Table languageTable = new Table (composite, SWT.SINGLE | SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
    	fillLanguageTable(languageTable);
    	languageTable.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
    	
        Text typeLabel = new Text(composite, SWT.READ_ONLY);
        typeLabel.setText("Choose the project type:");
        typeLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
        
    	Table typeTable = new Table(composite, SWT.SINGLE | SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
    	fillTypeTable(typeTable);
    	typeTable.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
    	
    	languageTable.addListener(SWT.Selection, event -> {
    		TableItem item = null;
    		if (event.detail == SWT.CHECK) {
				item = (TableItem)event.item;
				if (item.getChecked()) {
					for (TableItem it : languageTable.getItems()) {
						if (it != item) {
							it.setChecked(false);
						}
					}
				}
			}
    		if (item != null && item.getChecked()) {
    			language = item.getText();
    			if (ProjectType.LANGUAGE_JAVA.equals(item.getText())) {
		        	typeLabel.setVisible(true);
		        	typeTable.setVisible(true);
    			} else {
    				typeLabel.setVisible(false);
    				typeTable.setVisible(false);
    			}
	        } else {
	        	language = null;
	        	typeLabel.setVisible(false);
				typeTable.setVisible(false);
	        }
    		getWizard().getContainer().updateButtons();
    	});
    	
    	typeTable.addListener(SWT.Selection, event -> {
    		TableItem item = null;
    		if (event.detail == SWT.CHECK) {
				item = (TableItem)event.item;
				if (item.getChecked()) {
					for (TableItem it : typeTable.getItems()) {
						if (it != item) {
							it.setChecked(false);
						}
					}
				}
				if (item != null && item.getChecked()) {
					type = item.getText();
				} else {
					type = null;
				}
			} else {
				item = (TableItem)event.item;
				item.setChecked(!item.getChecked());
			}
    		if (item != null && item.getChecked()) {
    			type = item.getText();
	        } else {
	        	type = null;
	        }
    		getWizard().getContainer().updateButtons();
    	});

    	typeLabel.setVisible(false);
    	typeTable.setVisible(false);

		setControl(composite);
	}

	public boolean canFinish() {
		if (language == null) {
			return false;
		}
		if (ProjectType.LANGUAGE_JAVA.equals(language)) {
			return type != null;
		}
		return true;
	}
	
	private void fillLanguageTable(Table languageTable) {
		TableItem item = new TableItem(languageTable, SWT.NONE);
		item.setText(ProjectType.LANGUAGE_GO);
		item.setImage(MicroclimateUIPlugin.getImage(MicroclimateUIPlugin.GO_ICON));
		item = new TableItem(languageTable, SWT.NONE);
		item.setText(ProjectType.LANGUAGE_JAVA);
		item.setImage(MicroclimateUIPlugin.getImage(MicroclimateUIPlugin.JAVA_ICON));
		item = new TableItem(languageTable, SWT.NONE);
		item.setText(ProjectType.LANGUAGE_NODEJS);
		item.setImage(MicroclimateUIPlugin.getImage(MicroclimateUIPlugin.NODE_ICON));
		item = new TableItem(languageTable, SWT.NONE);
		item.setText(ProjectType.LANGUAGE_PYTHON);
		item.setImage(MicroclimateUIPlugin.getImage(MicroclimateUIPlugin.PYTHON_ICON));
		item = new TableItem(languageTable, SWT.NONE);
		item.setText(ProjectType.LANGUAGE_SWIFT);
		item.setImage(MicroclimateUIPlugin.getImage(MicroclimateUIPlugin.SWIFT_ICON));
	}
	
	private void fillTypeTable(Table typeTable) {
		TableItem item = new TableItem(typeTable, SWT.NONE);
		item.setText(ProjectType.TYPE_LIBERTY);
		item = new TableItem(typeTable, SWT.NONE);
		item.setText(ProjectType.TYPE_SPRING);
	}
	
	public MicroclimateConnection getConnection() {
		return connection;
	}
	
	public String getLanguage() {
		return language;
	}
	
	public String getType() {
		return type;
	}
	
	private void setupConnection() {
		List<MicroclimateConnection> connections = MicroclimateConnectionManager.activeConnections();
		if (connections != null && !connections.isEmpty()) {
			connection = connections.get(0);
		} else {
			try {
				// Will throw an Exception if fails
				connection = MicroclimateConnectionManager.createConnection(MicroclimateConnectionManager.DEFAULT_CONNECTION_URL);
			} catch(Exception e) {
				MCLogger.log("Attempting to connect to Codewind failed: " + e.getMessage());
			}
		}
		
		if (connection == null || !connection.isConnected()) {
			IRunnableWithProgress runnable = new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException {
					monitor.beginTask("Starting Codewind", IProgressMonitor.UNKNOWN);
					Process startProcess = null;
					try {
						startProcess = InstallUtil.startCodewind();
						ProcessResult result = ProcessHelper.waitForProcess(startProcess, 500, 300);
						if (result.getExitValue() != 0) {
							throw new InvocationTargetException(null, "There was a problem while trying to start Codewind: " + result.getError());
						}
					} catch (IOException e) {
						throw new InvocationTargetException(e, "An error occurred trying to start Codewind: " + e.getMessage());
					} catch (TimeoutException e) {
						throw new InvocationTargetException(e, "Codewind did not start in the expected time: " + e.getMessage());
					} finally {
						if (startProcess != null && startProcess.isAlive()) {
							startProcess.destroy();
						}
					}
					
				}
			};
			try {
				getWizard().getContainer().run(true, true, runnable);
			} catch (InvocationTargetException e) {
				MCLogger.logError("An error occurred trying to start Codewind", e);
				return;
			} catch (InterruptedException e) {
				MCLogger.logError("Codewind start was interrupted", e);
				return;
			}
		}
		
		// If there was a connection, check to see if it is connected to Codewind now
		if (connection != null) {
			for (int i = 0; i < 10; i++) {
				if (connection.isConnected()) {
					break;
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
					// Ignore
				}
			}
			if (!connection.isConnected()) {
				MCLogger.logError("The connection at " + connection.baseUrl + " is not active.");
			}
			return;
		}
		
		// If there was no connection, try to create one
		for (int i = 0; i < 10; i++) {
			try {
				connection = MicroclimateConnectionManager.createConnection(MicroclimateConnectionManager.DEFAULT_CONNECTION_URL);
				break;
			} catch (Exception e) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
					// Ignore
				}
			}
		}
		if (connection == null) {
			MCLogger.logError("Failed to connect to Codewind at: " + MicroclimateConnectionManager.DEFAULT_CONNECTION_URL);
		}
	}
		
}
