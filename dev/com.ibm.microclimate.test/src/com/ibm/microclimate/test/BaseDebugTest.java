package com.ibm.microclimate.test;

import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.wst.server.core.IServer;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.ibm.microclimate.core.internal.HttpUtil;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.MicroclimateObjectFactory;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnectionManager;
import com.ibm.microclimate.core.internal.constants.ProjectType;
import com.ibm.microclimate.core.internal.server.MicroclimateServerBehaviour;
import com.ibm.microclimate.core.internal.server.MicroclimateServerFactory;
import com.ibm.microclimate.test.util.ImportUtil;
import com.ibm.microclimate.test.util.MicroclimateUtil;
import com.ibm.microclimate.test.util.ServerUtil;
import com.ibm.microclimate.test.util.TestUtil;

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
        IPath workspace = connection.getWorkspacePath();
        project = ImportUtil.importExistingMavenProjects(workspace, projectName);
        assertNotNull("The " + projectName + " project should be imported in eclipse", project);
    }
    
    @Test
    public void test02_linkProjectToMicroclimate() throws Exception {
    	IServer server = MicroclimateServerFactory.create(connection.getAppByName(projectName));
    	// Make sure the server behaviour gets created so that its initialize method gets called.
    	// The initialize method sets the initial server state.
    	MicroclimateServerBehaviour mcServerBehaviour = (MicroclimateServerBehaviour) server.loadAdapter(MicroclimateServerBehaviour.class, null);
    	assertNotNull("Server behaviour should be created.", mcServerBehaviour);
    	assertTrue("Server should be in started state.  Current state is: " + server.getServerState(), ServerUtil.waitForServerState(server, IServer.STATE_STARTED, 120, 2));
    	assertNotNull("Should be able to get the server from the app.", connection.getAppByName(projectName).getLinkedServer());
    	pingApp(currentText);
    	checkMode(ILaunchManager.RUN_MODE);
    	checkConsoles();
    }
    
    @Test
    public void test03_switchToDebugMode() throws Exception {
    	switchMode(ILaunchManager.DEBUG_MODE);
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
    	MicroclimateServerBehaviour serverBehaviour = app.getLinkedServer();
    	// For Java builds the states can go by quickly so don't do an assert on this
    	ServerUtil.waitForServerState(serverBehaviour.getServer(), IServer.STATE_STOPPED, 120, 1);
    	assertTrue("Server should be in started state", ServerUtil.waitForServerState(serverBehaviour.getServer(), IServer.STATE_STARTED, 120, 1));
    	pingApp(currentText);
    	checkMode(ILaunchManager.DEBUG_MODE);
    	checkConsoles();
    }
    
    @Test
    public void test05_modifyDockerfile() throws Exception {
    	IPath path = connection.getWorkspacePath().append(projectName);
    	path = path.append(dockerfile);
    	TestUtil.prependToFile(path.toOSString(), "# no comment\n");
    	MicroclimateApplication app = connection.getAppByName(projectName);
    	MicroclimateServerBehaviour serverBehaviour = app.getLinkedServer();
    	assertTrue("Server should be in stopped state", ServerUtil.waitForServerState(serverBehaviour.getServer(), IServer.STATE_STOPPED, 120, 1));
    	assertTrue("Server should be in starting state", ServerUtil.waitForServerState(serverBehaviour.getServer(), IServer.STATE_STARTING, 600, 1));
    	assertTrue("Server should be in started state", ServerUtil.waitForServerState(serverBehaviour.getServer(), IServer.STATE_STARTED, 300, 1));
    	pingApp(currentText);
    	checkMode(ILaunchManager.DEBUG_MODE);
    	checkConsoles();
    }
    
    
    @Test
    public void test06_switchToRunMode() throws Exception {
    	switchMode(ILaunchManager.RUN_MODE);
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
    
    private void switchMode(String mode) throws Exception {
    	MicroclimateApplication app = connection.getAppByName(projectName);
    	MicroclimateServerBehaviour serverBehaviour = app.getLinkedServer();
    	serverBehaviour.restart(mode);
    	// For Java builds the states can go by quickly so don't do an assert on this
    	ServerUtil.waitForServerState(serverBehaviour.getServer(), IServer.STATE_STOPPED, 30, 1);
    	assertTrue("Server should be in started state", ServerUtil.waitForServerState(serverBehaviour.getServer(), IServer.STATE_STARTED, 120, 1));
    	checkMode(mode);
    }
    
    private void checkMode(String mode) throws Exception {
    	MicroclimateApplication app = connection.getAppByName(projectName);
    	MicroclimateServerBehaviour serverBehaviour = app.getLinkedServer();
    	assertTrue("Server should be in mode: " + mode, serverBehaviour.getServer().getMode() == mode);
    	ILaunch launch = serverBehaviour.getServer().getLaunch();
    	assertNotNull("There should be a launch for the server", launch);
    	IDebugTarget debugTarget = launch.getDebugTarget();
    	if (ILaunchManager.DEBUG_MODE.equals(mode)) {
	    	assertNotNull("The launch should have a debug target", debugTarget);
	    	assertTrue("The debug target should have threads", debugTarget.hasThreads());
    	} else {
    		assertNull("The launch should not have a debug target", debugTarget);
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
