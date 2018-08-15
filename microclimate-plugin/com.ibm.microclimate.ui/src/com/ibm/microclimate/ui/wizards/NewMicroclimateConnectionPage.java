package com.ibm.microclimate.ui.wizards;

import java.net.ConnectException;
import java.net.URISyntaxException;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.ibm.microclimate.core.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateConnection;
import com.ibm.microclimate.core.internal.MicroclimateConnectionManager;

/**
 * This simple page allows the user to add new Microclimate connections, by entering a hostname and port and
 * validating that Microclimate is indeed reachable at the given address.
 *
 * @author timetchells@ibm.com
 *
 */
public class NewMicroclimateConnectionPage extends WizardPage {

	private Text hostnameText, portText;

	private MicroclimateConnection mcConnection;

	protected NewMicroclimateConnectionPage() {
		super("New Microclimate Connection");
		setTitle("New Microclimate Connection Title");
		setDescription("New Microclimate Connection Description");
	}

	@Override
	public void createControl(Composite parent) {
		Composite shell = new Composite(parent, SWT.NULL);
		shell.setLayout(new GridLayout());

		createHostnameAndPortFields(shell);

		setControl(shell);

	}

	private void createHostnameAndPortFields(Composite shell) {
		Composite hostPortGroup = new Composite(shell, SWT.BORDER);
		//gridData.verticalSpan = 2;
		hostPortGroup.setLayout(new GridLayout(3, false));
		hostPortGroup.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

		GridData hostnamePortLabelData = new GridData(GridData.FILL, GridData.BEGINNING, false, false);

		Label hostnameLabel = new Label(hostPortGroup, SWT.BORDER);
		hostnameLabel.setText("Hostname:");
		hostnameLabel.setLayoutData(hostnamePortLabelData);

		hostnameText = new Text(hostPortGroup, SWT.BORDER);
		GridData hostnamePortTextData = new GridData(GridData.FILL, GridData.BEGINNING, true, false, 2, 1);
		hostnameText.setLayoutData(hostnamePortTextData);
		hostnameText.setText("localhost");

		/*
		// Invalidate the wizard when the host or port are changed so that the user has to test the connection again.
		ModifyListener modifyListener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				mcConnection = null;
				getWizard().getContainer().updateButtons();
			}
		};*/

		// hostnameText.addModifyListener(modifyListener);

		Label portLabel = new Label(hostPortGroup, SWT.BORDER);
		portLabel.setText("Port:");
		portLabel.setLayoutData(hostnamePortLabelData);

		portText = new Text(hostPortGroup, SWT.BORDER);
		portText.setLayoutData(hostnamePortTextData);
		portText.setText("9090");

		// portText.addModifyListener(modifyListener);

		Button testConnectionBtn = new Button(hostPortGroup, SWT.NONE);
		testConnectionBtn.setText("Add Connection");
		testConnectionBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				testConnection();
			}
		});
		testConnectionBtn.setLayoutData(new GridData(GridData.CENTER, GridData.CENTER, false, false));
	}

	void testConnection() {
		mcConnection = null;

		// Try to connect to Microclimate at the given hostname:port
		String hostname = hostnameText.getText().trim();
		String portStr = portText.getText().trim();

		String hostPortAddr = String.format("%s:%s", hostname, portStr);

		try {
			int port = Integer.parseInt(portStr);

			MCLogger.log("Validating connection: " + hostPortAddr);

			// Will throw a ConnectException if fails
			mcConnection = MicroclimateConnectionManager.create(hostname, port);

			if(mcConnection != null) {
				setErrorMessage(null);
				setMessage("Connecting to " + hostPortAddr + " succeeded");
			}
		}
		catch(ConnectException | URISyntaxException e) {
			setErrorMessage(e.getMessage());
		}
		catch(NumberFormatException e) {
			setErrorMessage(String.format("\"%s\" is not a valid port number", portStr));
		}

		getWizard().getContainer().updateButtons();
	}

	boolean canFinish() {
		// MCLogger.log("NewConnectionPage canFinish= " + isFinished);
		return mcConnection != null;
	}

	@Override
	public boolean canFlipToNextPage() {
		return canFinish();
	}
}
