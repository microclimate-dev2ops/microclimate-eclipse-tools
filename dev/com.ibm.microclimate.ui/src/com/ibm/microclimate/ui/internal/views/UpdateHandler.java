package com.ibm.microclimate.ui.internal.views;

import com.ibm.microclimate.core.internal.IUpdateHandler;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;

public class UpdateHandler implements IUpdateHandler {
	
	@Override
	public void updateAll() {
		ViewHelper.refreshMicroclimateExplorerView(null);
	}

	@Override
	public void updateConnection(MicroclimateConnection connection, boolean contentChanged) {
		// Simple for now
		ViewHelper.refreshMicroclimateExplorerView(connection);
	}

	@Override
	public void updateApplication(MicroclimateApplication application, boolean contentChanged) {
		// Simple for now
		ViewHelper.refreshMicroclimateExplorerView(application.mcConnection);
	}

}
