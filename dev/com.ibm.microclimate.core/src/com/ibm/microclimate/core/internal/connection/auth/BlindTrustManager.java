package com.ibm.microclimate.core.internal.connection.auth;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class BlindTrustManager {
	
	/**
	 * Warning - This is terribly insecure!
	 * Here we disable certificate validation, thus opening up users to man-in-the-middle attacks.
	 * Good for development. Not suitable for production!
	 */
	public static void trustAll() throws KeyManagementException, NoSuchAlgorithmException {
		System.out.println("----- Trusting all TLS certs");
		
	    TrustManager blindTrustManager = new X509TrustManager() {
	        @Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
	            return null;
	        }
	        @Override
			public void checkClientTrusted(X509Certificate[] certs, String authType) {
	        	// Nothing!
	        }
	        
	        @Override
			public void checkServerTrusted(X509Certificate[] certs, String authType) {
	        	// Nothing!
	        }
	    };
	    
        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            @Override
			public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
        
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, new TrustManager[] { blindTrustManager }, new java.security.SecureRandom());
//        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
//        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
//        sun.net.www.protocol.https.HttpsURLConnectionImpl.setDefaultSSLSocketFactory(sc.getSocketFactory());
//        sun.net.www.protocol.https.HttpsURLConnectionImpl.setDefaultHostnameVerifier(allHostsValid);
        com.nimbusds.oauth2.sdk.http.HTTPRequest.setDefaultSSLSocketFactory(sc.getSocketFactory());
        com.nimbusds.oauth2.sdk.http.HTTPRequest.setDefaultHostnameVerifier(allHostsValid);
        
		System.out.println("----- Trusted all TLS certs");
	}
}
