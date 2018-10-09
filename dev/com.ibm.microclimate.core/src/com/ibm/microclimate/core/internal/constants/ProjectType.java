package com.ibm.microclimate.core.internal.constants;

import com.ibm.microclimate.core.internal.MCLogger;

public enum ProjectType {

	LIBERTY("liberty", "Microprofile", true),
	SPRING("spring", "Spring", true),
	NODE("nodejs", "Node.js", false),
	SWIFT("swift", "Swift", false),
	DOCKER("docker", "Docker", false),
	UNKNOWN("unknown", "Unknown", false);

	public final String internalType, userFriendlyType;
	public final boolean isDebuggable;

	private ProjectType(String internalType, String userFriendlyType, boolean isDebuggable) {
		this.internalType = internalType;
		this.userFriendlyType = userFriendlyType;
		this.isDebuggable = isDebuggable;
	}

	public static ProjectType fromInternalType(String internalType) {
		for (ProjectType type : ProjectType.values()) {
			if (type.internalType.equals(internalType)) {
				return type;
			}
		}
		MCLogger.logError("Unknown internal project type " + internalType);
		return ProjectType.UNKNOWN;
	}
}
