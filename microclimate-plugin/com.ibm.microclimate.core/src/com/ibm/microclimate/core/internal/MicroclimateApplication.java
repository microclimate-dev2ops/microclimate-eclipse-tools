package com.ibm.microclimate.core.internal;

public class MicroclimateApplication {
	
	private MicroclimateConnection mcConnection;
	private String id, name, language;
	
	MicroclimateApplication(MicroclimateConnection mcConnection, String id, String name, String language) {
		this.mcConnection = mcConnection;
		this.id = id;
		this.name = name;
		this.language = language;
	}

	public String name() {
		return name;
	}
	
	public String language() {
		return language;
	}
	
	@Override
	public String toString() {
		return String.format("%s@%s id=%s name=%s language=%s", 
				MicroclimateApplication.class.getSimpleName(), mcConnection.baseUrl(), id, name, language);
	}
}
