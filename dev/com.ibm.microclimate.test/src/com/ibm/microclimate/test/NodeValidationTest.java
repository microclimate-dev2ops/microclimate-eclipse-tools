/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.test;

import org.eclipse.core.runtime.IPath;

import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.constants.AppState;
import com.ibm.microclimate.core.internal.constants.ProjectType;
import com.ibm.microclimate.test.util.MicroclimateUtil;
import com.ibm.microclimate.test.util.TestUtil;

public class NodeValidationTest extends BaseValidationTest {

	static {
		projectName = "nodevalidationtest";
		projectType = new ProjectType(ProjectType.TYPE_NODEJS, ProjectType.LANGUAGE_NODEJS);
		relativeURL = "/hello";
		srcPath = "server/server.js";
		text = "Hello World!";
		dockerfile = "Dockerfile";
	}

	@Override
	public void doSetup() throws Exception {
		super.doSetup();
		IPath path = connection.getWorkspacePath().append(projectName);
    	path = path.append(srcPath);
		TestUtil.updateFile(path.toOSString(), "// Add your code here", "app.get('/hello', (req, res) => res.send('Hello World!'));");
		build();
		MicroclimateApplication app = connection.getAppByName(projectName);
		// For Java builds the states can go by quickly so don't do an assert on this
		MicroclimateUtil.waitForAppState(app, AppState.STOPPED, 120, 1);
		assertTrue("App should be in started state", MicroclimateUtil.waitForAppState(app, AppState.STARTED, 300, 1));
	}
}
