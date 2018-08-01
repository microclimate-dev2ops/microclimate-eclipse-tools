package com.ibm.microclimate.ui.wizards;

import java.net.ConnectException;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.ibm.microclimate.core.internal.MicroclimateConnection;
import com.ibm.microclimate.core.internal.MicroclimateConnectionManager;

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
		Composite userAndPassGroup = new Composite(shell, SWT.BORDER);
		//gridData.verticalSpan = 2;
		userAndPassGroup.setLayout(new GridLayout(5, true));
		userAndPassGroup.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
		
		GridData hostnamePortLabelData = new GridData(GridData.END, GridData.BEGINNING, true, false);
		hostnamePortLabelData.minimumWidth = 100;
		
		Label hostnameLabel = new Label(userAndPassGroup, SWT.BORDER);
		hostnameLabel.setText("Hostname:");
		hostnameLabel.setLayoutData(hostnamePortLabelData);
		
		hostnameText = new Text(userAndPassGroup, SWT.BORDER);
		hostnameText.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 3, 1));
		hostnameText.setText("localhost");
		
		Label emptyLabel = new Label(userAndPassGroup, SWT.NONE);
			
		Label portLabel = new Label(userAndPassGroup, SWT.BORDER);
		portLabel.setText("Port:");
		portLabel.setLayoutData(hostnamePortLabelData);
		
		portText = new Text(userAndPassGroup, SWT.BORDER);
		portText.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 3, 1));
		portText.setText("9090");
		
		// Add a validator so that only integers can be entered into the port field
		portText.addVerifyListener(new VerifyListener() {

			@Override
			public void verifyText(VerifyEvent e) {
				Text text = (Text) e.getSource();
				String value = text.getText();
				boolean accept = true;
				
				// Accept any integer input
				try {
					Integer.parseInt(value);
				}
				catch (NumberFormatException nfe) {
					// not a valid port
					accept = false;
				}
				
				e.doit = accept;
			}
		});
		
		Label emptyLabel2 = new Label(userAndPassGroup, SWT.NONE);
		
		Button testConnectionBtn = new Button(userAndPassGroup, SWT.NONE);
		testConnectionBtn.setText("Test Connection");
		testConnectionBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				mcConnection = null;
				
				// Try to connect to Microclimate at the given hostname:port
				String hostname = hostnameText.getText().trim();
				String portStr = portText.getText().trim();

				String hostPortAddr = String.format("%s:%s", hostname, portStr);
				
				try {
					int port = Integer.parseInt(portStr);
					
					System.out.println("Validating connection: " + hostPortAddr);
					
					// Will throw a ConnectException if fails
					mcConnection = MicroclimateConnectionManager.create(hostname, port);
					
					if(mcConnection != null) {
						setErrorMessage(null);
						setMessage("Connecting to " + hostPortAddr + " succeeded");
					}
				}
				catch(ConnectException e) {
					setErrorMessage("Connecting to " + hostPortAddr + " failed");
				}
				catch(NumberFormatException e) {
					// Shouldn't happen because we already validated this field
					setErrorMessage(String.format("\"%s\" is not a valid port number", portStr));
				}

				getWizard().getContainer().updateButtons();
			}
		});
		testConnectionBtn.setLayoutData(new GridData(GridData.CENTER, GridData.CENTER, false, false));
	}

	boolean canFinish() {
		// System.out.println("NewConnectionPage canFinish= " + isFinished);
		return mcConnection != null;
	}
	
	@Override
	public boolean canFlipToNextPage() {
		return canFinish();
	}
}
