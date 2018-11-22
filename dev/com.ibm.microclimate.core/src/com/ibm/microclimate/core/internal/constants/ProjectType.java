/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core.internal.constants;

public class ProjectType {
	
	public static final String UNKNOWN = "unknown";
	
	public static final String TYPE_LIBERTY = "liberty";
	public static final String TYPE_SPRING = "spring";
	public static final String TYPE_NODE = "nodejs";
	public static final String TYPE_DOCKER = "docker";
	
	public static final String LANGUAGE_JAVA = "java";
	public static final String LANGUAGE_PYTHON = "python";
	
	public static final ProjectType UNKNOWN_TYPE = new ProjectType(UNKNOWN, UNKNOWN);
	
	public final String type;
	public final String language;
	
	public ProjectType(String type, String language) {
		this.type = type;
		this.language = language;
	}
	
	public boolean isType(String type) {
		return type != null && type.equals(this.type);
	}
	
	public boolean isLanguage(String language) {
		return language != null && language.equals(this.language);
	}

}
