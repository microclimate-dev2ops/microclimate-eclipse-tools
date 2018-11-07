/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core.internal.console;

import java.io.IOException;
import java.net.URI;

import com.ibm.microclimate.core.internal.HttpUtil;
import com.ibm.microclimate.core.internal.HttpUtil.HttpResult;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.constants.MCConstants;

public class BuildLogMonitor implements Runnable {
	
	private boolean disposed = false;
	
	private final BuildLogConsole console;
	
	public BuildLogMonitor(BuildLogConsole console) {
		this.console = console;
	}

	@Override
	public void run() {
		while (!disposed) {
			// First check if log has changed
			MicroclimateApplication app = console.getApp();
			String buildLogPath = MCConstants.APIPATH_PROJECT_LIST + "/" + app.projectID + "/" + MCConstants.KEY_BUILD_LOG;	//$NON-NLS-1$
			URI uri = app.mcConnection.baseUrl.resolve(buildLogPath);
			try {
				HttpResult result = HttpUtil.head(uri);
				if (result.isGoodResponse) {
					String timestampStr = result.getHeader(MCConstants.KEY_BUILD_LOG_LAST_MODIFIED);
					double timestamp = Double.parseDouble(timestampStr);
					if (console.hasChanged(timestamp)) {
						// Now get the contents
						result = HttpUtil.get(uri);
						if (result.isGoodResponse ) {
							timestampStr = result.getHeader(MCConstants.KEY_BUILD_LOG_LAST_MODIFIED);
							timestamp = Double.parseDouble(timestampStr);
							String contents = result.response;
							console.update(contents, timestamp, true);
						} else {
							MCLogger.logError("Get request failed for " + uri + ": " + result.error); //$NON-NLS-1$
						}
					}
				} else {
					MCLogger.logError("Head request failed for " + uri + ": " + result.error); //$NON-NLS-1$
				}
			} catch (IOException e) {
				MCLogger.logError("Head request failed for uri: " + uri, e); //$NON-NLS-1$
			} catch (NumberFormatException e) {
				MCLogger.logError("Invalid timestamp returned for uri: " + uri, e); //$NON-NLS-1$
			} catch (Exception e) {
				MCLogger.logError("Exception processing result for uri: " + uri, e); //$NON-NLS-1$
			}
			
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// Ignore
			}
		}
	}

	public synchronized void dispose() {
		if (!disposed) {
			disposed = true;
		}
	}

}
