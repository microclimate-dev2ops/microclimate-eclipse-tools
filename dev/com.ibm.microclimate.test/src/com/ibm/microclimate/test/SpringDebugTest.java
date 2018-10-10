package com.ibm.microclimate.test;

import com.ibm.microclimate.core.internal.constants.ProjectType;

public class SpringDebugTest extends BaseDebugTest {

	static {
		projectName = "springdebugtest";
		projectType = ProjectType.SPRING;
		relativeURL = "/v1";
		srcPath = "src/main/java/application/rest/v1/Example.java";
		currentText = "Congratulations";
		newText = "Hello";
		dockerfile = "Dockerfile";
	}
}
