package com.ibm.microclimate.core.internal;

import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;

public interface IUpdateHandler {
	
	public void updateAll();
	
	public void updateConnection(MicroclimateConnection connection, boolean contentChanged);
	
	public void updateApplication(MicroclimateApplication application, boolean contentChanged);

}
