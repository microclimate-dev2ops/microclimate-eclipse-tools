/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.microclimate.ui.server.actions;

import java.util.Iterator;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;
import org.eclipse.wst.server.core.IServer;

import com.ibm.microclimate.core.internal.MCConstants;
import com.ibm.microclimate.core.server.MicroclimateServerBehaviour;

/**
 * From com.ibm.ws.st.ui.internal.actions.LogActionProvider
 *
 */
public class LogActionProvider extends CommonActionProvider implements ISelectionChangedListener {

    protected MicroclimateServerBehaviour mcServerBehaviour;
    protected Shell shell;

    @Override
    public void init(ICommonActionExtensionSite aSite) {
        super.init(aSite);
        shell = aSite.getViewSite().getShell();

        ISelectionProvider selectionProvider = aSite.getStructuredViewer();
        selectionProvider.addSelectionChangedListener(this);
        onSelectionChange(selectionProvider.getSelection());
    }

	@Override
	public void selectionChanged(SelectionChangedEvent selectionChangedEvent) {
		onSelectionChange(selectionChangedEvent.getSelection());
	}

	private void onSelectionChange(ISelection selection) {
		if (selection instanceof StructuredSelection) {
			IStructuredSelection sel = (IStructuredSelection) selection;

	        Iterator<?> iterator = sel.iterator();
	        while (iterator.hasNext()) {
	            Object obj = iterator.next();
	            if (obj instanceof IServer) {
	                IServer server = (IServer) obj;
	                mcServerBehaviour = (MicroclimateServerBehaviour)
	                		server.loadAdapter(MicroclimateServerBehaviour.class, null);
	            }
	        }
		}
	}

    @Override
    public void fillContextMenu(IMenuManager menu) {

    	if (mcServerBehaviour == null) {
    		return;
    	}

        MenuManager openLogsMenu = new MenuManager("Open Log File", "OpenLogFiles");

    	for (IPath logFilePath : mcServerBehaviour.getApp().getLogFilePaths()) {
    		String name = logFilePath.lastSegment();
			if (name.endsWith(MCConstants.BUILD_LOG_SHORTNAME)) {
				name = MCConstants.BUILD_LOG_SHORTNAME;
			}

			OpenLogAction openLogAction = new OpenLogAction("Open " + name, shell, logFilePath);
			openLogsMenu.add(openLogAction);
    	}

        menu.appendToGroup(ICommonMenuConstants.GROUP_ADDITIONS, openLogsMenu);
    }
}