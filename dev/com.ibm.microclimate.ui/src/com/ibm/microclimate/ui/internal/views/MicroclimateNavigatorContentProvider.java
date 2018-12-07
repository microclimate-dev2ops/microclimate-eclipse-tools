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

import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ITreeContentProvider;

import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnectionManager;

/**
 * Content provider for the Microclimate view.
 */
public class MicroclimateNavigatorContentProvider implements ITreeContentProvider {

	@Override
	public Object[] getChildren(Object obj) {
		if (obj instanceof MicroclimateConnection) {
			MicroclimateConnection connection = (MicroclimateConnection)obj;
			List<MicroclimateApplication> apps = connection.getApps();
			return apps.toArray(new MicroclimateApplication[apps.size()]);
		}
		return null;
	}

	@Override
	public Object[] getElements(Object obj) {
		List<MicroclimateConnection> connections = MicroclimateConnectionManager.activeConnections();
		return connections.toArray(new MicroclimateConnection[connections.size()]);
	}

	@Override
	public Object getParent(Object obj) {
		if (obj instanceof MicroclimateConnection) {
			return ResourcesPlugin.getWorkspace().getRoot();
		} else if (obj instanceof MicroclimateApplication) {
			MicroclimateApplication app = (MicroclimateApplication)obj;
			return app.mcConnection;
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object obj) {
		if (obj instanceof MicroclimateConnection) {
			MicroclimateConnection connection = (MicroclimateConnection)obj;
			return !connection.getApps().isEmpty();
		}
		return false;
	}

}
