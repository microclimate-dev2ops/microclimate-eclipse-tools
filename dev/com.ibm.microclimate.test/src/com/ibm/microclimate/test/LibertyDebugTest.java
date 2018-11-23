package com.ibm.microclimate.test;

import com.ibm.microclimate.core.internal.constants.ProjectType;

public class LibertyDebugTest extends BaseDebugTest {

	static {
		projectName = "libertydebugtest";
		projectType = new ProjectType(ProjectType.TYPE_LIBERTY, ProjectType.LANGUAGE_JAVA);
		relativeURL = "/v1/example";
		srcPath = "src/main/java/application/rest/v1/Example.java";
		currentText = "Congratulations";
		newText = "Hello";
		dockerfile = "Dockerfile-lang";
	}
}
