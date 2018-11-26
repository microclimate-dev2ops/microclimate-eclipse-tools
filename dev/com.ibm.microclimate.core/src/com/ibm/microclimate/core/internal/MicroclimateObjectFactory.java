/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core.internal;

import java.net.URI;

import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.constants.ProjectType;

/**
 * Factory for creating the correct Microclimate objects.  This is used to keep the Eclipse
 * code and the Microclimate code separate.
 * 
 * Currently only MicroclimateApplication has an Eclipse version.  Rather than let Eclipse
 * code leak into MicroclimateConnection an Eclipse version of it should be created if necessary.
 */
public class MicroclimateObjectFactory {
	
	public static MicroclimateConnection createMicroclimateConnection(URI uri) throws Exception {
		return new MicroclimateConnection(uri);
	}
	
	public static MicroclimateApplication createMicroclimateApplication(MicroclimateConnection mcConnection,
			String id, String name, ProjectType projectType, String pathInWorkspace, String contextRoot) throws Exception {
		return new MCEclipseApplication(mcConnection, id, name, projectType, pathInWorkspace, contextRoot);
	}

}
