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

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateObjectFactory;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnectionManager;
import com.ibm.microclimate.ui.internal.messages.Messages;

public class LocalConnectionComposite extends ConnectionComposite {
	
	private Text hostnameText, portText;
	private Button testConnectionBtn;
	
	public LocalConnectionComposite(Composite parent, WizardPage wizardPage) {
		super(parent, wizardPage);
		createControl();
	}
	
	public void createControl() {
		this.setLayout(new GridLayout());
		createContent(this);
	}
	
	private void createContent(Composite parent) {
		Composite hostPortGroup = new Composite(parent, SWT.NONE);
		//gridData.verticalSpan = 2;
		hostPortGroup.setLayout(new GridLayout(3, false));
		hostPortGroup.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

		GridData hostnamePortLabelData = new GridData(GridData.FILL, GridData.FILL, false, false);

		Label hostnameLabel = new Label(hostPortGroup, SWT.NONE);
		hostnameLabel.setText(Messages.NewConnectionPage_HostnameLabel);
		hostnameLabel.setLayoutData(hostnamePortLabelData);

		hostnameText = new Text(hostPortGroup, SWT.BORDER | SWT.READ_ONLY);
		GridData hostnamePortTextData = new GridData(GridData.FILL, GridData.BEGINNING, true, false, 2, 1);
		hostnameText.setLayoutData(hostnamePortTextData);
		Color bg = hostnameText.getBackground();
		Color fg = hostnameText.getForeground();
        final Color gray = new Color(bg.getDevice(), (bg.getRed() + fg.getRed()) / 2, (bg.getGreen() + fg.getGreen()) / 2, (bg.getBlue() + fg.getBlue()) / 2);
        hostnameText.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent event) {
                gray.dispose();
            }
        });
        hostnameText.setForeground(gray);
		hostnameText.setText("localhost"); //$NON-NLS-1$
		final String localhostOnly = Messages.NewConnectionPage_OnlyLocalhostSupported;
		hostnameLabel.setToolTipText(localhostOnly);
		hostnameText.setToolTipText(localhostOnly);

		// Invalidate the wizard when the host or port are changed so that the user has to test the connection again.
		ModifyListener modifyListener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				removePreviousMCConnection();
				validate();
			}
		};

		hostnameText.addModifyListener(modifyListener);

		Label portLabel = new Label(hostPortGroup, SWT.NONE);
		portLabel.setText(Messages.NewConnectionPage_PortLabel);
		portLabel.setLayoutData(hostnamePortLabelData);

		portText = new Text(hostPortGroup, SWT.BORDER);
		portText.setLayoutData(hostnamePortTextData);
		portText.setText("9090"); //$NON-NLS-1$

		portText.addModifyListener(modifyListener);

		testConnectionBtn = new Button(hostPortGroup, SWT.PUSH);
		testConnectionBtn.setText(Messages.NewConnectionPage_TestConnectionBtn);
		testConnectionBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// Block the Test Connection button while we test it
				testConnectionBtn.setEnabled(false);
				testConnection();
				testConnectionBtn.setEnabled(true);
				testConnectionBtn.setFocus();
			}
		});
		testConnectionBtn.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));

		validate();
	}

	@Override
	protected void validate() {
		// In the Local case, the user can only create one connection
		MicroclimateConnection localConnection = MicroclimateConnectionManager.getLocalConnection();
		if (localConnection != null) {
			testConnectionBtn.setEnabled(false);
			String existingConnectionUrl = localConnection.baseUrl.toString();
			wizardPage.setMessage(
					NLS.bind(Messages.NewConnectionPage_ErrAConnectionAlreadyExists,
					existingConnectionUrl), WizardPage.ERROR);
		} else if (portText.getText() == null || portText.getText().isEmpty()) {
			wizardPage.setMessage(Messages.NewConnectionPage_NoPort, WizardPage.ERROR);
		} else {
			wizardPage.setMessage(Messages.NewConnectionPage_TestToProceed);
			wizardPage.getWizard().getContainer().updateButtons();
		}
		wizardPage.getWizard().getContainer().updateButtons();
	}

	void testConnection() {
		removePreviousMCConnection();

		// Try to connect to Microclimate at the given hostname:port
		String hostname = hostnameText.getText().trim();
		String portStr = portText.getText().trim();

		URI uri = null;
		try {
			int port = Integer.parseInt(portStr);

			uri = MicroclimateConnection.buildUrl(hostname, port);
		}
		catch(NumberFormatException e) {
			MCLogger.logError(e);
			wizardPage.setErrorMessage(NLS.bind(Messages.NewConnectionPage_NotValidPortNum, portStr));
		}
		catch(URISyntaxException e) {
			MCLogger.logError(e);
			wizardPage.setErrorMessage(e.getMessage());
		}

		if (uri == null) {
			return;
		}

		try {
			MCLogger.log("Validating connection: " + uri); //$NON-NLS-1$

			// Will throw an Exception if fails
			mcConnection = MicroclimateObjectFactory.createMicroclimateConnection(uri);

			if(mcConnection != null) {
				wizardPage.setErrorMessage(null);
				wizardPage.setMessage(NLS.bind(Messages.NewConnectionPage_ConnectSucceeded, mcConnection.baseUrl));
			}
		}
		catch(Exception e) {
			String msg = e.getMessage();
			if (msg == null) {
				// The exceptions we expect to get here should have good messages for the user.
				// Show a generic message if none is provided.
				MCLogger.logError("Unexpected exception", e); //$NON-NLS-1$

				msg = NLS.bind(Messages.NewConnectionPage_ErrCouldNotConnectToMC, uri);
			}
			wizardPage.setErrorMessage(msg);
		}

		wizardPage.getWizard().getContainer().updateButtons();
	}
}
