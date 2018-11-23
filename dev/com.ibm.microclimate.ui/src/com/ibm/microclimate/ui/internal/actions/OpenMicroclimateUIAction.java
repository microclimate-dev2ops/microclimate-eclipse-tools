/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.actions;

import java.net.URI;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.ui.internal.messages.Messages;

public class OpenMicroclimateUIAction implements IObjectActionDelegate {

    protected MicroclimateConnection connection;

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        if (!(selection instanceof IStructuredSelection)) {
            action.setEnabled(false);
            return;
        }

        IStructuredSelection sel = (IStructuredSelection) selection;
        if (sel.size() == 1) {
            Object obj = sel.getFirstElement();
            if (obj instanceof MicroclimateConnection) {
            	connection = (MicroclimateConnection)obj;
            	action.setEnabled(connection.isConnected());
            	return;
            }
        }
        action.setEnabled(false);
    }

    @Override
    public void run(IAction action) {
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

        URI uri = connection.baseUrl;

		try {
			IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
			IWebBrowser browser = browserSupport
					.createBrowser(IWorkbenchBrowserSupport.NAVIGATION_BAR | IWorkbenchBrowserSupport.LOCATION_BAR,
							uri.toString(), uri.toString(), "Microclimate UI - " + uri);

	        browser.openURL(uri.toURL());
		} catch (Exception e) {
			MCLogger.logError("Error opening Microclimate UI in browser", e); //$NON-NLS-1$
		}
    }

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		// nothing
	}
}
