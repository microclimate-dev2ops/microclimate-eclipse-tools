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

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;

public abstract class ConnectionComposite extends Composite {
	
	protected final WizardPage wizardPage;
	
	protected MicroclimateConnection mcConnection;
	
	public ConnectionComposite(Composite parent, WizardPage wizardPage) {
		super(parent, SWT.NONE);
		this.wizardPage = wizardPage;
	}
	
	public MicroclimateConnection getMCConnection() {
		return mcConnection;
	}
	
	protected void removePreviousMCConnection() {
		if (mcConnection != null) {
			mcConnection.close();
		}
		mcConnection = null;
	}
	
	protected abstract void validate();

}
