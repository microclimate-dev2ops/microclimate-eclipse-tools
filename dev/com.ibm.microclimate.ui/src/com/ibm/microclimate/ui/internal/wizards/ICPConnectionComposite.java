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

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateObjectFactory;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnectionManager;
import com.ibm.microclimate.core.internal.connection.auth.Authenticator;
import com.ibm.microclimate.ui.MicroclimateUIPlugin;
import com.ibm.microclimate.ui.internal.views.ViewHelper;

public class ICPConnectionComposite extends ConnectionComposite {
	
	private Text masterIPText, ingressURLText, namespaceText;
	
	public ICPConnectionComposite(Composite parent, WizardPage wizardPage) {
		super(parent, wizardPage);
		createControl();
	}
	
	public void createControl() {
		this.setLayout(new GridLayout());
		createContent(this);
	}
	
	private void createContent(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
		
		Label masterIPLabel = new Label(composite, SWT.NONE);
		masterIPLabel.setText("Master IP:");
		masterIPLabel.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));
		
		masterIPText = new Text(composite, SWT.BORDER);
		masterIPText.setText("9.42.80.228");
		masterIPText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

		Label ingressURLLabel = new Label(composite, SWT.NONE);
		ingressURLLabel.setText("Ingress URL:");
		ingressURLLabel.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));
		
		ingressURLText = new Text(composite, SWT.BORDER);
		ingressURLText.setText("https://microclimate.9.42.41.81.nip.io");
		ingressURLText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
		
		Label namespaceLabel = new Label(composite, SWT.NONE);
		namespaceLabel.setText("Namespace:");
		namespaceLabel.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));
		
		namespaceText = new Text(composite, SWT.BORDER);
		namespaceText.setText("mcg");
		namespaceText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
	}

	@Override
	protected void validate() {
		wizardPage.setMessage(null);
		wizardPage.getWizard().getContainer().updateButtons();
	}

	@Override
	protected boolean canFinish() {
		return true;
	}

	@Override
	protected void performFinish() {
		final String masterIP = masterIPText.getText();
		final String ingressURL = ingressURLText.getText();
		final String namespace = namespaceText.getText();
		Job job = new Job("Connect to host: " + masterIP) {
			@Override
			protected IStatus run(IProgressMonitor arg0) {
				// Initiate the authentication
				try {
					Authenticator.instance().authenticate(masterIP);
				} catch (Exception e) {
					MCLogger.logError("An error occurred trying to authorize with ICP host: " + masterIP, e);
					return new Status(IStatus.ERROR, MicroclimateUIPlugin.PLUGIN_ID, "An error occurred trying to authorize with ICP host: " + masterIP, e);
				}
				
				// Wait for the authentication to complete
				int timeout = 10000;
				String token = null;
				for (int i = 0; i < timeout/500; i++) {
					try {
						token = Authenticator.instance().getToken(masterIP);
						if (token != null) {
							break;
						}
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							// Ignore
						}
					} catch (StorageException e) {
						MCLogger.logError("An error occurred trying to authorize with ICP host: " + masterIP, e);
						return new Status(IStatus.ERROR, MicroclimateUIPlugin.PLUGIN_ID, "An error occurred trying to authorize with ICP host: " + masterIP, e);
					}
				}
				
				if (token == null) {
					MCLogger.logError("The token was not set within the timeout period for host: " + masterIP);
					return new Status(IStatus.ERROR, MicroclimateUIPlugin.PLUGIN_ID, "Failed to authorize with ICP host: " + masterIP);
				}
				
				URI uri;
				try {
					uri = new URI(ingressURL);
				} catch (URISyntaxException e) {
					MCLogger.logError("Invalid ingress uri: " + ingressURL, e);
					return new Status(IStatus.ERROR, MicroclimateUIPlugin.PLUGIN_ID, "The ingress URL is not valid: " + ingressURL, e);
				}

				// Will throw an Exception if fails
				try {
					final MicroclimateConnection connection = MicroclimateObjectFactory.createICPConnection(uri, masterIP, namespace);
					MicroclimateConnectionManager.add(connection);
					Display.getDefault().syncExec(new Runnable() {
		                @Override
		                public void run() {
		                	ViewHelper.openMicroclimateExplorerView();
		            		ViewHelper.refreshMicroclimateExplorerView(null);
		            		ViewHelper.expandConnection(connection);
		                }
		            });
					return Status.OK_STATUS;
				} catch (Exception e) {
					MCLogger.logError("An error occurred trying to connect to Microclimate URL: " + ingressURL);
					return new Status(IStatus.ERROR, MicroclimateUIPlugin.PLUGIN_ID, "An error occurred trying to connect to Microclimate URL: " + ingressURL, e);
				}
			}
		};
		job.schedule();
		
	}

}
