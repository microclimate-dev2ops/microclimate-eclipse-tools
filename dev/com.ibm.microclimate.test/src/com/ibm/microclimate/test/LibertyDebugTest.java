package com.ibm.microclimate.test;

import java.net.URI;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.wst.server.core.IServer;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnectionManager;
import com.ibm.microclimate.core.internal.server.MicroclimateServerBehaviour;
import com.ibm.microclimate.core.internal.server.MicroclimateServerFactory;
import com.ibm.microclimate.test.util.ImportUtil;
import com.ibm.microclimate.test.util.MicroclimateUtil;
import com.ibm.microclimate.test.util.ServerUtil;
import com.ibm.microclimate.test.util.TestUtil;

import junit.framework.TestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LibertyDebugTest extends TestCase {

	private static final String MICROCLIMATE_URI = "http://localhost:9090";
	private static final String PROJECT_NAME = "libertydebugtest";
	private static MicroclimateConnection connection;
	private static IProject project;
	
    @Test
    public void test01_doSetup() throws Exception {
        TestUtil.print("Starting test: " + getName());
        
        // Create a microclimate connection
        connection = new MicroclimateConnection(new URI(MICROCLIMATE_URI));
        MicroclimateConnectionManager.add(connection);
        
        // Create a new microprofile project
        connection.requestMicroprofileProjectCreate(PROJECT_NAME);
        
        // Wait for the project to be created
        assertTrue("The application " + PROJECT_NAME + " should be created", MicroclimateUtil.waitForProject(connection, PROJECT_NAME, 300, 5));
        
        // Wait for the project to be started
        assertTrue("The application " + PROJECT_NAME + " should be running", MicroclimateUtil.waitForProjectStart(connection, PROJECT_NAME, 600, 5));
        
        // Import the application into eclipse
        IPath workspace = connection.getWorkspacePath();
        project = ImportUtil.importExistingMavenProjects(workspace, PROJECT_NAME);
        assertNotNull("The " + PROJECT_NAME + " project should be imported in eclipse", project);
    }
    
    @Test
    public void test02_linkProjectToMicroclimate() throws Exception {
    	IServer server = MicroclimateServerFactory.create(connection.getAppByName(PROJECT_NAME));
    	// Make sure the server behaviour gets created so that its initialize method gets called.
    	// The initialize method sets the initial server state.
    	MicroclimateServerBehaviour mcServerBehaviour = (MicroclimateServerBehaviour) server.loadAdapter(MicroclimateServerBehaviour.class, null);
    	assertNotNull("Server behaviour should be created.", mcServerBehaviour);
    	assertTrue("Server should be in started state.  Current state is: " + server.getServerState(), ServerUtil.waitForServerState(server, IServer.STATE_STARTED, 120, 2));
    	assertNotNull("Should be able to get the server from the app.", connection.getAppByName(PROJECT_NAME).getLinkedServer());
    }
    
    @Test
    public void test03_switchToDebugMode() throws Exception {
    	MicroclimateApplication app = connection.getAppByName(PROJECT_NAME);
    	MicroclimateServerBehaviour serverBehaviour = app.getLinkedServer();
    	serverBehaviour.restart(ILaunchManager.DEBUG_MODE);
    	assertTrue("Server should be in started state", ServerUtil.waitForServerState(serverBehaviour.getServer(), IServer.STATE_STARTED, 120, 1));
    	assertTrue("Server should be in debug mode", serverBehaviour.getServer().getMode() == ILaunchManager.DEBUG_MODE);
    	ILaunch launch = serverBehaviour.getServer().getLaunch();
    	assertNotNull("There should be a launch for the server", launch);
    	IDebugTarget debugTarget = launch.getDebugTarget();
    	assertNotNull("The launch should have a debug target", debugTarget);
    	assertTrue("The debug target should have threads", debugTarget.hasThreads());
    }
    
    @Test
    public void test08_switchToRunMode() throws Exception {
    	MicroclimateApplication app = connection.getAppByName(PROJECT_NAME);
    	MicroclimateServerBehaviour serverBehaviour = app.getLinkedServer();
    	serverBehaviour.restart(ILaunchManager.RUN_MODE);
    	assertTrue("Server should be in started state", ServerUtil.waitForServerState(serverBehaviour.getServer(), IServer.STATE_STARTED, 120, 1));
    	assertTrue("Server should be in run mode", serverBehaviour.getServer().getMode() == ILaunchManager.RUN_MODE);
    	ILaunch launch = serverBehaviour.getServer().getLaunch();
    	assertNotNull("There should be a launch for the server", launch);
    	IDebugTarget debugTarget = launch.getDebugTarget();
    	assertNull("The launch should not have a debug target", debugTarget);
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
}
