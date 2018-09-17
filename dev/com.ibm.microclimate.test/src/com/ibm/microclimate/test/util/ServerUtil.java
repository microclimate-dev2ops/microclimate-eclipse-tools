package com.ibm.microclimate.test.util;

import org.eclipse.wst.server.core.IServer;

public class ServerUtil {
	
	public static boolean waitForServerState(IServer server, int state, long timeout, long interval) {
		TestUtil.wait(new Condition() {
			@Override
			public boolean test() {
				return server.getServerState() == state;
			}
		}, timeout, interval);
		return server.getServerState() == state;
	}

}
