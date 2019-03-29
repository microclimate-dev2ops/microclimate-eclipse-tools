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

package com.ibm.microclimate.kubeclient.util;

import java.net.URI;
import java.net.URISyntaxException;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;

public class ICPUtil extends KubeclientUtil {
	
	private static final String APP_LABEL_KEY = "app";
	private static final String APP_LABEL_VALUE = "microclimate-ibm-microclimate-admin-editor";
	private static final String EDITOR_POD_PREFIX = "microclimate-ibm-microclimate-admin-editor";
	private static final String INGRESS_NAME = "microclimate-ibm-microclimate";
	
	public static ICPInfo getICPInfo() throws URISyntaxException {
		KubernetesClient client = getDefaultClient();
		
		Config config = client.getConfiguration();
		String masterURL = config.getMasterUrl();
		URI uri = new URI(masterURL);
		String masterIP = getHostname(uri);
		String username = config.getUsername();
		
		IngressList ingressList = client.extensions().ingresses().inAnyNamespace().list();
		for (Ingress ingress : ingressList.getItems()) {
			if (INGRESS_NAME.equals(ingress.getMetadata().getName())) {
				URI ingressURL = new URI("https://" + ingress.getSpec().getRules().get(0).getHost());
				String namespace = ingress.getMetadata().getNamespace();
				return new ICPInfo(masterIP, ingressURL, namespace, username);
			}
		}
			
		return null;
	}
	
	public static Pod getEditorPod(KubernetesClient client) {
		PodList pods = client.pods().inAnyNamespace().withLabel(APP_LABEL_KEY, APP_LABEL_VALUE).list();
		for (Pod pod : pods.getItems()) {
			if (pod.getMetadata().getName().startsWith(EDITOR_POD_PREFIX)) {
				return pod;
			}
		}
		return null;
	}
	
	private static String getHostname(URI uri) {
		String authority = uri.getAuthority();
		int index = authority.indexOf(':');
		if (index > 0) {
			return authority.substring(0, index);
		}
		return authority;
	}

	public static class ICPInfo {
		public final String masterIP;
		public final URI ingressURL;
		public final String namespace;
		public final String username;
		
		public ICPInfo(String masterIP, URI ingressURL, String namespace, String username) {
			this.masterIP = masterIP;
			this.ingressURL = ingressURL;
			this.namespace = namespace;
			this.username = username;
		}
	}
}
