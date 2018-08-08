package com.ibm.microclimate.core.internal;

import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.eclipse.core.runtime.IPath;

public class MicroclimateApplication {

	private final MicroclimateConnection mcConnection;
	public final String id, name, language, host, contextRoot;
	public final IPath fullLocalPath;
	public final int httpPort;
	public final URL rootUrl;

	public static List<MicroclimateApplication> buildFromProjectsJson(MicroclimateConnection conn,
			String projectsJson) {

		List<MicroclimateApplication> result = new ArrayList<>();

		// TODO improve error handling - failures here need to be passed up to the calling UI so we can display a msg
		try {
			JsonReader jsReader = Json.createReader(new StringReader(projectsJson));
			JsonArray appArray = jsReader.readArray();

			for(int i = 0; i < appArray.size(); i++) {
				JsonObject app = appArray.getJsonObject(i);
				System.out.println("app: " + app.toString());
				String id 	= app.getString("projectID");
				String name = app.getString("name");
				String lang = app.getString("language");
				String loc 	= app.getString("locOnDisk");
				String exposedPort = app.getJsonObject("ports").getString("exposedPort");

				try {
					int port = Integer.parseInt(exposedPort);

					final String contextRootKey = "contextroot";
					String contextRoot = null;
					if(app.containsKey(contextRootKey)) {
						contextRoot = app.getString(contextRootKey);
					}

					result.add(new MicroclimateApplication(conn, id, name, lang, loc, port, contextRoot));
				}
				catch(NumberFormatException | MalformedURLException e) {
					e.printStackTrace();
				}
			}
		}
		catch(JsonException e) {
			e.printStackTrace();
		}

		return result;
	}

	MicroclimateApplication(MicroclimateConnection mcConnection,
			String id, String name, String language, String pathWithinWorkspace, int port, String contextRoot)
					throws MalformedURLException {

		this.mcConnection = mcConnection;
		this.id = id;
		this.name = name;
		this.language = language;
		this.fullLocalPath = mcConnection.localWorkspacePath().append(pathWithinWorkspace);
		this.httpPort = port;
		this.contextRoot = contextRoot;
		this.host = mcConnection.host();

		URL rootUrl = new URL("http", host, port, "");

		if (contextRoot != null) {
			rootUrl = new URL(rootUrl, contextRoot);
		}

		this.rootUrl = rootUrl;

		//System.out.println("Created mcApp:");
		//System.out.println(toString());
	}

	/*
	public String name() {
		return name;
	}

	public String language() {
		return language;
	}

	public IPath fullLocalPath() {
		return fullLocalPath;
	}

	public URL rootUrl() {
		return rootUrl;
	}*/

	@Override
	public String toString() {
		return String.format("%s@%s id=%s name=%s language=%s loc=%s",
				MicroclimateApplication.class.getSimpleName(), rootUrl.toString(),
				id, name, language, fullLocalPath.toOSString());
	}
}
