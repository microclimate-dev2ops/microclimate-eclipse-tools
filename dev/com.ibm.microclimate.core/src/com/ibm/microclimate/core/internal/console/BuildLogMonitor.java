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
