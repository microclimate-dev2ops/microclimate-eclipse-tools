/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.views;

import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ITreeContentProvider;

import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnectionManager;

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
