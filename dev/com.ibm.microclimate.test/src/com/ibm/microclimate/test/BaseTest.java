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

import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.ide.IDE;

import com.ibm.microclimate.core.internal.HttpUtil;
import com.ibm.microclimate.core.internal.MCEclipseApplication;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.MicroclimateObjectFactory;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnectionManager;
import com.ibm.microclimate.core.internal.console.MicroclimateConsoleFactory;
import com.ibm.microclimate.core.internal.constants.AppState;
import com.ibm.microclimate.core.internal.constants.MCConstants;
import com.ibm.microclimate.core.internal.constants.ProjectType;
import com.ibm.microclimate.core.internal.constants.StartMode;
import com.ibm.microclimate.test.util.Condition;
import com.ibm.microclimate.test.util.ImportUtil;
import com.ibm.microclimate.test.util.MicroclimateUtil;
import com.ibm.microclimate.test.util.TestUtil;
import com.ibm.microclimate.ui.internal.actions.ImportProjectAction;

import junit.framework.TestCase;

public abstract class BaseTest extends TestCase {

	protected static final String MICROCLIMATE_URI = "http://localhost:9090";
	
	protected static final String MARKER_TYPE = "com.ibm.microclimate.core.validationMarker";
	
	protected static MicroclimateConnection connection;
	protected static IProject project;
	
	protected static String projectName;
	protected static ProjectType projectType;
	protected static String relativeURL;
	protected static String srcPath;
	
    public void doSetup() throws Exception {
        // Create a microclimate connection
        connection = MicroclimateObjectFactory.createMicroclimateConnection(new URI(MICROCLIMATE_URI));
        MicroclimateConnectionManager.add(connection);
        
        // Create a new microprofile project
        connection.requestProjectCreate(projectType, projectName);
        
        // Wait for the project to be created
        assertTrue("The application " + projectName + " should be created", MicroclimateUtil.waitForProject(connection, projectName, 300, 5));
        
        // Wait for the project to be started
        assertTrue("The application " + projectName + " should be running", MicroclimateUtil.waitForProjectStart(connection, projectName, 600, 5));
        
        // Import the application into eclipse
        MicroclimateApplication app = connection.getAppByName(projectName);
        ImportProjectAction.importProject(app);
        project = ImportUtil.waitForProject(projectName);
        assertNotNull("The " + projectName + " project should be imported in eclipse", project);
    }
    
    public void checkApp(String text) throws Exception {
    	MicroclimateApplication app = connection.getAppByName(projectName);
    	assertTrue("App should be in started state.  Current state is: " + app.getAppState(), MicroclimateUtil.waitForAppState(app, AppState.STARTED, 120, 2));
    	pingApp(text);
    	checkMode(StartMode.RUN);
    	showConsoles();
    	checkConsoles();
    }
    
    protected void pingApp(String expectedText) throws Exception {
    	MicroclimateApplication app = connection.getAppByName(projectName);
    	URL url = app.getBaseUrl();
    	url = new URL(url.toExternalForm() + relativeURL);
    	HttpUtil.HttpResult result = HttpUtil.get(url.toURI());
    	assertTrue("The response code should be 200: " + result.responseCode, result.responseCode == 200);
    	assertTrue("The response should contain the expected text: " + expectedText, result.response != null && result.response.contains(expectedText));   	
    }
    
    protected void checkMode(StartMode mode) throws Exception {
    	MicroclimateApplication app = connection.getAppByName(projectName);
    	assertTrue("App should be in mode: " + mode, app.getStartMode() == mode);
    	ILaunch launch = ((MCEclipseApplication)app).getLaunch();
    	if (StartMode.DEBUG_MODES.contains(mode)) {
    		assertNotNull("There should be a launch for the app", launch);
        	IDebugTarget debugTarget = launch.getDebugTarget();
	    	assertNotNull("The launch should have a debug target", debugTarget);
	    	assertTrue("The debug target should have threads", debugTarget.hasThreads());
    	} else {
    		assertNull("There should be no launch when in run mode", launch);
    	}
    }
    
    protected void switchMode(StartMode mode) throws Exception {
    	MicroclimateApplication app = connection.getAppByName(projectName);
    	connection.requestProjectRestart(app, mode.startMode);
    	// For Java builds the states can go by quickly so don't do an assert on this
    	MicroclimateUtil.waitForAppState(app, AppState.STOPPED, 30, 1);
    	assertTrue("App should be in started state", MicroclimateUtil.waitForAppState(app, AppState.STARTED, 120, 1));
    	checkMode(mode);
    }
    
    protected void showConsoles() throws Exception {
    	MicroclimateApplication app = connection.getAppByName(projectName);
    	if (!app.projectType.isType(ProjectType.TYPE_NODEJS)) {
    		IConsole buildConsole = MicroclimateConsoleFactory.createBuildConsole(app);
        	((MCEclipseApplication)app).setAppConsole(buildConsole);
    	}
    	IConsole appConsole = MicroclimateConsoleFactory.createApplicationConsole(app);
		((MCEclipseApplication)app).setAppConsole(appConsole);
		// Wait for application log to have content - it is only updated every 20s
		TestUtil.wait(new Condition() {
			@Override
			public boolean test() {
				return ((TextConsole)appConsole).getDocument().getLength() > 0;
			}
		}, 20, 1);
    }

    protected void checkConsoles() throws Exception {
    	MicroclimateApplication app = connection.getAppByName(projectName);
    	Set<String> expectedConsoles = new HashSet<String>();
    	Set<String> foundConsoles = new HashSet<String>();
    	if (!app.projectType.isType(ProjectType.TYPE_NODEJS)) {
    		expectedConsoles.add("Build Log");
    	}
    	expectedConsoles.add("Application Log");
    	IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
    	for (IConsole console : manager.getConsoles()) {
    		if (console.getName().contains(projectName)) {
    			TestUtil.print("Found console: " + console.getName());
    			assertTrue("The " + console.getName() + " console should be a TextConsole", console instanceof TextConsole);
    			assertTrue("The " + console.getName() + " console should not be empty", ((TextConsole)console).getDocument().getLength() > 0);
    			for (String name : expectedConsoles) {
    				if (console.getName().contains(name)) {
    					foundConsoles.add(name);
    					break;
    				}
    			}
    		}
    	}
    	assertTrue("Did not find all expected consoles", foundConsoles.size() == expectedConsoles.size());
    }
    
    protected void buildIfWindows() throws Exception {
    	if (TestUtil.isWindows()) {
    		build();
    	}
    }
    
    protected void build() throws Exception {
    	MicroclimateApplication app = connection.getAppByName(projectName);
		connection.requestProjectBuild(app, MCConstants.VALUE_ACTION_BUILD);
    }
    
    protected void setAutoBuild(boolean enabled) throws Exception {
    	String actionKey = enabled ? MCConstants.VALUE_ACTION_ENABLEAUTOBUILD : MCConstants.VALUE_ACTION_DISABLEAUTOBUILD;
    	MicroclimateApplication app = connection.getAppByName(projectName);
		connection.requestProjectBuild(app, actionKey);
    }
    
    protected IMarker[] getMarkers(IResource resource) throws Exception {
    	return resource.findMarkers(MARKER_TYPE, false, IResource.DEPTH_ONE);
    }
    
    protected void runValidation() throws Exception {
    	MicroclimateApplication app = connection.getAppByName(projectName);
		connection.requestValidate(app);
    }
    
    protected void runQuickFix(IResource resource) throws Exception {
    	IMarker[] markers = getMarkers(resource);
    	assertTrue("There should be at least one marker for " + resource.getName() + ": " + markers.length, markers.length > 0);

        IMarkerResolution[] resolutions = IDE.getMarkerHelpRegistry().getResolutions(markers[0]);
        assertTrue("Did not get any marker resolutions.", resolutions.length > 0);
        resolutions[0].run(markers[0]);
        TestUtil.waitForJobs(10, 1);
    }

}
