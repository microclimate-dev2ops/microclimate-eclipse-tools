/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.core.internal.remote;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.remote.ProcessHelper.ProcessResult;

public class ICPConnection {
	
	// TODO: set up kubectl using ICP address and authorization
	// use "kubectl --kubeconfig <path for config> ..."
	
	// Use a thread safe list
	private Vector<Process> portForwardProcesses = new Vector<Process>();
	
	private final String namespace;
	
	public ICPConnection(String namespace) {
		this.namespace = namespace;
	}
	
	public boolean isThisConnection(String namespace) {
		return this.namespace.equals(namespace);
	}
	
	/**
	 * Get the contents of a file in the pod
	 * @param podName The pod name
	 * @param namespace The namespace for the pod
	 * @param containerName The container name or null if there is only one container
	 * @param srcPath The location of the file in the pod
	 * @param destPath A destination directory to copy the file to
	 * @throws Exception
	 */
	public String getFileFromContainer(String podName, String containerName, String srcPath, String fileName, String tmpPath) throws Exception {
		// kubectl cp <namespace>/<pod name>:<src path> <relative dest path> -c <container name>
		// kubectl cp only works if the local path is a relative path
		List<String> command = new ArrayList<String>(Arrays.asList("kubectl", "cp", namespace + "/" + podName + ":" + srcPath + "/" + fileName, namespace));
		if (containerName != null) {
			command.add("-c");
			command.add(containerName);
		}
		File fullPath = new File(tmpPath + File.separator + namespace);
		File file = new File(fullPath.getPath() + File.separator + fileName);
		ProcessResult result = null;
		try {
			fullPath.mkdirs();
			ProcessBuilder builder = new ProcessBuilder(command);
			builder.directory(new File(tmpPath));
			result = ProcessHelper.waitForProcess(builder.start(), 500, 5);
			if (result.getExitValue() == 0 && file.exists()) {
				FileInputStream in = null;
				try {
					in = new FileInputStream(file);
					String content = MCUtil.readAllFromStream(in);
					return content;
				} finally {
					if (in != null) {
						try {
							in.close();
						} catch (IOException e) {
							MCLogger.logError("Could not close the file: " + file);
						}
					}
				}
			}
			MCLogger.logError("Failed to copy " + srcPath + " from pod: " + podName);
			throw new IOException(result.getError());
		} finally {
			if (file.exists()) {
				file.delete();
			}
			if (fullPath.exists()) {
				fullPath.delete();
			}
		}
	}
	
	/**
	 * Given a selector, get the pod name
	 * @param selector The pod selector
	 * @param namespace The namespace to search in
	 * @return The pod name
	 * @throws Exception
	 */
	public String getPodName(String selector) throws Exception {
		// kubectl get pod -l <pod selector> -n <namespace> -o jsonpath={.items[0].metadata.name}
		String[] command = {"kubectl", "get", "pod", "-l", selector, "-n", namespace, "-o", "jsonpath={.items[0].metadata.name}"};
		ProcessBuilder builder = new ProcessBuilder(command);
		ProcessResult result = ProcessHelper.waitForProcess(builder.start(), 500, 5);
		if (result.getExitValue() == 0) {
			return result.getOutput();
		}
		MCLogger.logError("Failed to get the pod name: " + result.getError());
		throw new IOException(result.getError());
	}
	
	/**
	 * Start port forwarding
	 * @param podName The pod name
	 * @param namespace The namespace for the pod
	 * @param localPort The local port to use
	 * @param containerPort The container port to forward
	 * @return The process created to do the port forwarding. This process should be killed if Eclipse is closed.
	 * @throws Exception
	 */
	public void portForward(String podName, String localPort, String containerPort) throws Exception {
		// kubectl port-forward <pod name> -n <namespace> <local port>:<container port>
		String[] command = {"kubectl", "port-forward", podName, "-n", namespace, localPort + ":" + containerPort};
		ProcessBuilder builder = new ProcessBuilder(command);
		Process process = builder.start();
		if (process != null && process.isAlive()) {
			portForwardProcesses.add(process);
			return;
		}
		
		// Make sure the process is killed
		if (process != null) {
			process.destroy();
		}
		String msg = "Failed to initiate port forwarding:" + String.join(" ", command);
		MCLogger.logError(msg);
		throw new IOException(msg);
	}
	
	public void dispose() {
		for (Process process : portForwardProcesses) {
			process.destroy();
		}
	}
}
