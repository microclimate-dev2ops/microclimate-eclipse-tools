/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *	 IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.actions;

import java.net.URI;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.ui.MicroclimateUIPlugin;
import com.ibm.microclimate.ui.internal.messages.Messages;

/**
 * Action to open the Microclimate UI in a browser.  This allows users to easily
 * access features that are not implemented in the Microclimate plug-ins.
 */
public class OpenMicroclimateUIAction extends SelectionProviderAction {
	
	public enum Page {
		HOME,
		NEW_PROJECT,
		IMPORT_PROJECT
	};
	
	protected final Page page;
	protected MicroclimateConnection connection;
	
	public OpenMicroclimateUIAction(String label, Page page, ISelectionProvider selectionProvider) {
		super(selectionProvider, label);
		this.page = page;
		selectionChanged(getStructuredSelection());
		setImageDescriptor(MicroclimateUIPlugin.getDefaultIcon());
	}


	@Override
	public void selectionChanged(IStructuredSelection sel) {
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof MicroclimateConnection) {
				connection = (MicroclimateConnection)obj;
				setEnabled(connection.isConnected());
				return;
			}
		}
		setEnabled(false);
	}

	@Override
	public void run() {
		if (connection == null) {
			// should not be possible
			MCLogger.logError("Open Microclimate UI action ran but no Microclimate connection was selected");
			return;
		}

		if (!connection.isConnected()) {
			MCUtil.openDialog(true, Messages.OpenMicroclimateUIError,
					NLS.bind(Messages.OpenMicroclimateUINotConnectedError, connection.baseUrl));
			return;
		}

		try {
			URI uri = null;
			switch (page) {
				case HOME:
					uri = connection.baseUrl;
					break;
				case NEW_PROJECT:
					uri = connection.getNewProjectURI();
					break;
				case IMPORT_PROJECT:
					uri = connection.getImportProjectURI();
					break;
			}
			if (uri == null) {
				uri = connection.baseUrl;
			}
			
			IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
			IWebBrowser browser = browserSupport
					.createBrowser(IWorkbenchBrowserSupport.NAVIGATION_BAR | IWorkbenchBrowserSupport.LOCATION_BAR,
							uri.toString(), uri.toString(), "Microclimate UI - " + uri);

			browser.openURL(uri.toURL());
		} catch (Exception e) {
			MCLogger.logError("Error opening Microclimate UI in browser", e); //$NON-NLS-1$
		}
	}
}
