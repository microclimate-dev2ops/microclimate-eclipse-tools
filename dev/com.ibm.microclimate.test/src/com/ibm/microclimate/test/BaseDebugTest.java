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

package com.ibm.microclimate.test;

import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.TextConsole;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.ibm.microclimate.core.internal.HttpUtil;
import com.ibm.microclimate.core.internal.MCEclipseApplication;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.MicroclimateObjectFactory;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnectionManager;
import com.ibm.microclimate.core.internal.console.MicroclimateConsoleFactory;
import com.ibm.microclimate.core.internal.constants.AppState;
import com.ibm.microclimate.core.internal.constants.ProjectType;
import com.ibm.microclimate.core.internal.constants.StartMode;
import com.ibm.microclimate.test.util.ImportUtil;
import com.ibm.microclimate.test.util.MicroclimateUtil;
import com.ibm.microclimate.test.util.TestUtil;
import com.ibm.microclimate.ui.internal.actions.ImportProjectAction;

import junit.framework.TestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class BaseDebugTest extends TestCase {

	private static final String MICROCLIMATE_URI = "http://localhost:9090";
	
	private static MicroclimateConnection connection;
	private static IProject project;
	
	protected static String projectName;
	protected static ProjectType projectType;
	protected static String relativeURL;
	protected static String srcPath;
	protected static String currentText;
	protected static String newText;
	protected static String dockerfile;
	
    @Test
    public void test01_doSetup() throws Exception {
        TestUtil.print("Starting test: " + getName());
        
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
    
    @Test
    public void test02_checkApp() throws Exception {
    	MicroclimateApplication app = connection.getAppByName(projectName);
    	assertTrue("App should be in started state.  Current state is: " + app.getAppState(), MicroclimateUtil.waitForAppState(app, AppState.STARTED, 120, 2));
    	pingApp(currentText);
    	checkMode(StartMode.RUN);
    	showConsoles();
    	// Application log is only updated every 20 seconds
    	try {
			Thread.sleep(20000);
		} catch (InterruptedException e) {
			// Ignore
		}
    	checkConsoles();
    }
    
    @Test
    public void test03_switchToDebugMode() throws Exception {
    	switchMode(StartMode.DEBUG);
    	pingApp(currentText);
    	checkConsoles();
    }
    
    @Test
    public void test04_modifyJavaFile() throws Exception {
    	IPath path = connection.getWorkspacePath().append(projectName);
    	path = path.append(srcPath);
    	TestUtil.updateFile(path.toOSString(), currentText, newText);
    	currentText = newText;
    	MicroclimateApplication app = connection.getAppByName(projectName);
    	// For Java builds the states can go by quickly so don't do an assert on this
    	MicroclimateUtil.waitForAppState(app, AppState.STOPPED, 120, 1);
    	assertTrue("App should be in started state", MicroclimateUtil.waitForAppState(app, AppState.STARTED, 120, 1));
    	pingApp(currentText);
    	checkMode(StartMode.DEBUG);
    	checkConsoles();
    }
    
    @Test
    public void test05_modifyDockerfile() throws Exception {
    	IPath path = connection.getWorkspacePath().append(projectName);
    	path = path.append(dockerfile);
    	TestUtil.prependToFile(path.toOSString(), "# no comment\n");
    	MicroclimateApplication app = connection.getAppByName(projectName);
    	assertTrue("App should be in stopped state", MicroclimateUtil.waitForAppState(app, AppState.STOPPED, 120, 1));
    	assertTrue("App should be in starting state", MicroclimateUtil.waitForAppState(app, AppState.STARTING, 600, 1));
    	assertTrue("App should be in started state", MicroclimateUtil.waitForAppState(app, AppState.STARTED, 300, 1));
    	pingApp(currentText);
    	checkMode(StartMode.DEBUG);
    	checkConsoles();
    }
    
    
    @Test
    public void test06_switchToRunMode() throws Exception {
    	switchMode(StartMode.RUN);
    	pingApp(currentText);
    	checkConsoles();
    }
    
    
    @Test
    public void test99_tearDown() {
    	try {
			MicroclimateUtil.cleanup(connection);
		} catch (Exception e) {
			TestUtil.print("Test case cleanup failed", e);
		}
    	TestUtil.print("Ending test: " + getName());
    }
    
    private void pingApp(String expectedText) throws Exception {
    	MicroclimateApplication app = connection.getAppByName(projectName);
    	URL url = app.getBaseUrl();
    	url = new URL(url.toExternalForm() + relativeURL);
    	HttpUtil.HttpResult result = HttpUtil.get(url.toURI());
    	assertTrue("The response code should be 200: " + result.responseCode, result.responseCode == 200);
    	assertTrue("The response should contain the expected text: " + expectedText, result.response != null && result.response.contains(expectedText));   	
    }
    
    private void showConsoles() throws Exception {
    	MicroclimateApplication app = connection.getAppByName(projectName);
    	IConsole appConsole = MicroclimateConsoleFactory.createApplicationConsole(app);
    	((MCEclipseApplication)app).setAppConsole(appConsole);
    	IConsole buildConsole = MicroclimateConsoleFactory.createBuildConsole(app);
    	((MCEclipseApplication)app).setAppConsole(buildConsole);
    }
    
    private void switchMode(StartMode mode) throws Exception {
    	MicroclimateApplication app = connection.getAppByName(projectName);
    	connection.requestProjectRestart(app, mode.startMode);
    	// For Java builds the states can go by quickly so don't do an assert on this
    	MicroclimateUtil.waitForAppState(app, AppState.STOPPED, 30, 1);
    	assertTrue("App should be in started state", MicroclimateUtil.waitForAppState(app, AppState.STARTED, 120, 1));
    	checkMode(mode);
    }
    
    private void checkMode(StartMode mode) throws Exception {
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
    
    private void checkConsoles() throws Exception {
    	Set<String> expectedConsoles = new HashSet<String>();
    	Set<String> foundConsoles = new HashSet<String>();
    	expectedConsoles.add("Application Log");
    	expectedConsoles.add("Build Log");
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
}
