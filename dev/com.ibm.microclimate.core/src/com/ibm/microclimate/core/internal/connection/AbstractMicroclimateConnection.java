package com.ibm.microclimate.core.internal.connection;

/*
public class AbstractMicroclimateConnection {

	public final String host;
	public final int port;

	public final String baseUrl;

	protected List<MicroclimateApplication> apps = Collections.emptyList();

	public AbstractMicroclimateConnection(String host, int port) {
		this.host = host;
		this.port = port;
		this.baseUrl = String.format("http://%s:%d/", host, port);
	}

	public List<MicroclimateApplication> getLinkedApps() {
		if (apps == null) {
			return Collections.emptyList();
		}

		return apps.stream()
				.filter(app -> app.isLinked())
				.collect(Collectors.toList());
	}

	// Note that toPrefsString and fromPrefsString are used to save and load connections from the preferences store
	// in MicroclimateConnectionManager, so be careful modifying these.

	private static final String HOST_KEY = "$host", PORT_KEY = "$port";

	public String toPrefsString() {
		// No newlines allowed!

		return String.format("%s %s=%s %s=%s", MicroclimateConnection.class.getSimpleName(),
				HOST_KEY, host, PORT_KEY, port);
	}

	public static AbstractMicroclimateConnection fromPrefsString(String str) throws Exception {

		int hostIndex = str.indexOf(HOST_KEY);
		String afterHostKey = str.substring(hostIndex + HOST_KEY.length() + 1);

		// The hostname is between HOST_KEY and the PORT_KEY, also trim the space between them
		String host = afterHostKey.substring(0, afterHostKey.indexOf(PORT_KEY)).trim();

		int portIndex = str.indexOf(PORT_KEY);
		String portStr = str.substring(portIndex + PORT_KEY.length() + 1);

		int port = Integer.parseInt(portStr);

		try {
			 new MicroclimateConnection(host, port);
		}
		catch (MicroclimateConnectionException mce) {
			return new BrokenMicroclimateConnection(host, port);
		}

	}
}
*/