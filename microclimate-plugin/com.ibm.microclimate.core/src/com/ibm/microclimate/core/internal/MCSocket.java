package com.ibm.microclimate.core.internal;

import java.net.URISyntaxException;

import com.ibm.microclimate.core.MCLogger;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MCSocket {

	public final Socket socket;

	public MCSocket(String hostname, int port) throws URISyntaxException {
		// socket = IO.socket(hostname);
		String url = "http://" + hostname + ":" + port;
		socket = IO.socket(url);

		socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				MCLogger.log("SocketIO connect success");
			}
		})
		.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				MCLogger.log("SocketIO Connect Failure");
			}
		})
		.on(Socket.EVENT_ERROR, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				MCLogger.logError("SocketIO Error: " + arg0[0].toString());
			}
		})
		.on("projectStatusChanged", new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				MCLogger.log("projectStatusChanged: " + arg0[0].toString());
			}
		});
		socket.connect();

		MCLogger.log("Created SocketIO socket at " + url);
	}


}
