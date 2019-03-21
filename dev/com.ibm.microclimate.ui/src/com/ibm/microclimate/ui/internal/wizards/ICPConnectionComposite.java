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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.connection.auth.Authenticator;

public class ICPConnectionComposite extends ConnectionComposite {
	
	public ICPConnectionComposite(Composite parent, WizardPage wizardPage) {
		super(parent, wizardPage);
		createControl();
	}
	
	public void createControl() {
		this.setLayout(new GridLayout());
		createContent(this);
	}
	
	private void createContent(Composite parent) {
		///// Temp ICP connection test stuff
		final Text masterIPText = new Text(parent, SWT.BORDER);
		masterIPText.setText("9.42.28.18");
	
		final Button authBtn = new Button(parent, SWT.PUSH);
		authBtn.setText("Authorize");
		authBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					final String masterIP = masterIPText.getText();
					Authenticator.instance().authenticate(masterIP);
				} catch (Exception e) {
					MCLogger.logError("Auth error", e);
					MessageDialog.openError(parent.getShell(), "Auth error", e.getMessage());
				}
			}
		});
	}


	@Override
	protected void validate() {
		wizardPage.setMessage(null);
		wizardPage.getWizard().getContainer().updateButtons();
	}

}
