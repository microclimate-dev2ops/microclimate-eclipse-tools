/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.views;

import com.ibm.microclimate.core.internal.IUpdateHandler;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;

/**
 * Update handler registered on the Microclimate core plug-in in order to keep
 * the Microclimate view up to date.
 */
public class UpdateHandler implements IUpdateHandler {
	
	@Override
	public void updateAll() {
		ViewHelper.refreshMicroclimateExplorerView(null);
	}

	@Override
	public void updateConnection(MicroclimateConnection connection) {
		ViewHelper.refreshMicroclimateExplorerView(connection);
	}

	@Override
	public void updateApplication(MicroclimateApplication application) {
		ViewHelper.refreshMicroclimateExplorerView(application);
	}

}
