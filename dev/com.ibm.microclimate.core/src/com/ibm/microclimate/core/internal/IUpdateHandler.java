/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core.internal;

import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;

public interface IUpdateHandler {
	
	public void updateAll();
	
	public void updateConnection(MicroclimateConnection connection, boolean contentChanged);
	
	public void updateApplication(MicroclimateApplication application, boolean contentChanged);

}
