/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.marker;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;

import com.ibm.microclimate.core.internal.MCEclipseApplication;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnectionManager;

/**
 * Generates the quick fixes for the marker if there are any.
 */
public class MicroclimateMarkerResolutionGenerator implements IMarkerResolutionGenerator2 {

	@Override
	public IMarkerResolution[] getResolutions(IMarker marker) {
		MicroclimateApplication app = getApplication(marker);
		if (app == null) {
			return null;
		}
		
		String quickFixId = marker.getAttribute(MCEclipseApplication.QUICK_FIX_ID, (String)null);
		String quickFixDescription = marker.getAttribute(MCEclipseApplication.QUICK_FIX_DESCRIPTION, (String)null);
		IMarkerResolution resolution = new MicroclimateMarkerResolution(app, quickFixId, quickFixDescription);
		return new IMarkerResolution[] { resolution };
	}

	@Override
	public boolean hasResolutions(IMarker marker) {
		String quickFixId = marker.getAttribute(MCEclipseApplication.QUICK_FIX_ID, (String)null);
		String quickFixDescription = marker.getAttribute(MCEclipseApplication.QUICK_FIX_DESCRIPTION, (String)null);
		if (quickFixId == null || quickFixDescription == null) {
			return false;
		}
		
		// Check that the project still exists
		MicroclimateApplication app = getApplication(marker);
		if (app == null) {
			return false;
		}
		
		return true;
	}
	
	private MicroclimateApplication getApplication(IMarker marker) {
		String connectionUrl = marker.getAttribute(MCEclipseApplication.CONNECTION_URL, (String)null);
		String projectId = marker.getAttribute(MCEclipseApplication.PROJECT_ID, (String)null);
		MicroclimateConnection connection = MicroclimateConnectionManager.getActiveConnection(connectionUrl);
		if (connection == null) {
			return null;
		}
		MicroclimateApplication app = connection.getAppByID(projectId);
		return app;
	}

}
