/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.core.internal.constants;

/**
 * Type and language of a project.
 */
public class ProjectType {
	
	public static final String UNKNOWN = "unknown";
	
	public static final String TYPE_LIBERTY = "liberty";
	public static final String TYPE_SPRING = "spring";
	public static final String TYPE_NODEJS = "nodejs";
	public static final String TYPE_DOCKER = "docker";
	
	public static final String LANGUAGE_JAVA = "java";
	public static final String LANGUAGE_NODEJS = "nodejs";
	public static final String LANGUAGE_SWIFT = "swift";
	public static final String LANGUAGE_PYTHON = "python";
	public static final String LANGUAGE_GO = "go";
	
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

	@Override
	public String toString() {
		return ("Project type: " + type + ", project language: " + language);
	}

}
