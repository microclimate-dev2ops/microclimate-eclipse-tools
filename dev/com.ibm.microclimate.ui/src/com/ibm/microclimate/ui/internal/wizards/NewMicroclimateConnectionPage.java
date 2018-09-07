package com.ibm.microclimate.ui.internal.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnectionManager;

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
		setTitle("Create a Microclimate Connection");
		setDescription("Create a connection to a Microclimate instance.");
	}

	@Override
	public void createControl(Composite parent) {
		Composite shell = new Composite(parent, SWT.NULL);
		shell.setLayout(new GridLayout());

		createHostnameAndPortFields(shell);

		setControl(shell);
	}

	private void createHostnameAndPortFields(Composite shell) {
		Composite hostPortGroup = new Composite(shell, SWT.NONE);
		//gridData.verticalSpan = 2;
		hostPortGroup.setLayout(new GridLayout(3, false));
		hostPortGroup.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

		GridData hostnamePortLabelData = new GridData(GridData.FILL, GridData.FILL, false, false);

		Label hostnameLabel = new Label(hostPortGroup, SWT.NONE);
		hostnameLabel.setText("Hostname:");
		hostnameLabel.setLayoutData(hostnamePortLabelData);

		hostnameText = new Text(hostPortGroup, SWT.BORDER);
		GridData hostnamePortTextData = new GridData(GridData.FILL, GridData.BEGINNING, true, false, 2, 1);
		hostnameText.setLayoutData(hostnamePortTextData);
		hostnameText.setText("localhost");

		// grey this out (for now) because it's only ever localhost anyway
		final String localhostOnly = "Only localhost is supported at this time";
		hostnameText.setEnabled(false);
		hostnameLabel.setToolTipText(localhostOnly);
		// Doesn't work if hostnameText is disabled
		hostnameText.setToolTipText(localhostOnly);

		// Invalidate the wizard when the host or port are changed so that the user has to test the connection again.
		ModifyListener modifyListener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				removePreviousMCConnection();

				setErrorMessage(null);
				setMessage("Test your new connection to proceed");
				getWizard().getContainer().updateButtons();
			}
		};

		hostnameText.addModifyListener(modifyListener);

		Label portLabel = new Label(hostPortGroup, SWT.NONE);
		portLabel.setText("Port:");
		portLabel.setLayoutData(hostnamePortLabelData);

		portText = new Text(hostPortGroup, SWT.BORDER);
		portText.setLayoutData(hostnamePortTextData);
		portText.setText("9090");

		portText.addModifyListener(modifyListener);

		final Button testConnectionBtn = new Button(hostPortGroup, SWT.PUSH);
		testConnectionBtn.setText("Test Connection");
		testConnectionBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// Block the Test Connection button while we test it
				testConnectionBtn.setEnabled(false);
				testConnection();
				testConnectionBtn.setEnabled(true);
			}
		});
		testConnectionBtn.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));

		// In the Local case, the user can only create one connection,
		// so if they have one already, block the Add button.
		if (MicroclimateConnectionManager.connectionsCount() > 0) {
			testConnectionBtn.setEnabled(false);
			String existingConnectionUrl = MicroclimateConnectionManager.connections().get(0).baseUrl.toString();
			setErrorMessage("You already have an existing Microclimate connection at " + existingConnectionUrl +
					"\nAt this time, only one Microclimate connection is permitted.");
		}
	}

	void removePreviousMCConnection() {
		if (mcConnection != null) {
			mcConnection.close();
		}
		mcConnection = null;
	}

	void testConnection() {
		removePreviousMCConnection();

		// Try to connect to Microclimate at the given hostname:port
		String hostname = hostnameText.getText().trim();
		String portStr = portText.getText().trim();

		String hostPortAddr = String.format("%s:%s", hostname, portStr);

		try {
			int port = Integer.parseInt(portStr);

			MCLogger.log("Validating connection: " + hostPortAddr);

			// Will throw an Exception if fails
			mcConnection = new MicroclimateConnection(hostname, port);

			if(mcConnection != null) {
				setErrorMessage(null);
				setMessage("Connecting to " + mcConnection.baseUrl + " succeeded");
			}
		}
		catch(NumberFormatException e) {
			setErrorMessage(String.format("\"%s\" is not a valid port number", portStr));
		}
		catch(Exception e) {
			String msg = e.getMessage();
			if (msg == null) {
				// The exceptions we expect to get here should have good messages for the user.
				MCLogger.logError("Unexpected exception", e);
				msg = e.getClass().getSimpleName() + ": Could not connect to Microclimate at " + hostPortAddr;
			}
			setErrorMessage(msg);
		}

		getWizard().getContainer().updateButtons();
	}

	/**
	 * Test canFinish before calling this to make sure it will never return null.
	 */
	MicroclimateConnection getMCConnection() {
		return mcConnection;
	}

	@Override
	public boolean canFlipToNextPage() {
		return mcConnection != null;
	}

	void performFinish() {
		if (mcConnection != null) {
			MicroclimateConnectionManager.add(mcConnection);
		}
	}
}
