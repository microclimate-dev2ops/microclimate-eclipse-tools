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

package com.ibm.microclimate.core.internal.connection.auth;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import com.ibm.microclimate.core.internal.MCLogger;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.openid.connect.sdk.OIDCScopeValue;

public class Authenticator {

	//// Singleton stuff
	private static Authenticator instance;

	public static Authenticator instance() {
		if (instance == null) {
			MCLogger.log("Initializing Authenticator");
			instance = new Authenticator();
		}
		return instance;
	}

	private Authenticator() {
		// no instantiation except instance()
	}

	////

	private static final String CLIENT_ID = "microclimate-tools";
	static final String CALLBACK_URI = "mdteclipse://authcb";

	private static final String TOKEN_PREFIX = "token-";
	private static final String EXPIRY_PREFIX = "expires-";

	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	// Only one 'pending auth' at a time -> starting a new one cancels the previous
	private PendingAuth pendingAuth;

	/**
	 * Assemble the AuthorizationRequest and open the user's browser to the authorization login page.
	 * <br>
	 * <br>
	 * https://connect2id.com/products/nimbus-oauth-openid-connect-sdk/examples/oauth/authorization-request#implicit-flow
	 * <br>
	 * https://www.javadoc.io/doc/com.nimbusds/oauth2-oidc-sdk/6.5
	 */
	public void authenticate(String masterNodeIP) throws Exception {
		MCLogger.log("Authenticating against " + masterNodeIP);

		final String oidcServerUrl = String.format("https://%s:8443/oidc/endpoint/OP", masterNodeIP);
		// This is obtained from oidcServerUrl + /.well-known/openid-configuration
		final URI authorizeEndpoint = new URI(oidcServerUrl + "/authorize");

		// https://connect2id.com/products/nimbus-oauth-openid-connect-sdk/examples/oauth/authorization-request#implicit-flow
		// https://www.javadoc.io/doc/com.nimbusds/oauth2-oidc-sdk/6.5
		final URI callback = new URI(CALLBACK_URI);
		final State state = new State();

		if (pendingAuth != null) {
			MCLogger.log("Cancelling existing pendingAuth " + pendingAuth);
		}
		pendingAuth = new PendingAuth(masterNodeIP, state);

		final URI authReqUri = new AuthorizationRequest
			.Builder(new ResponseType(ResponseType.Value.TOKEN), new ClientID(CLIENT_ID))
			.scope(new Scope(OIDCScopeValue.OPENID))
			.state(state)
			.redirectionURI(callback)
			.endpointURI(authorizeEndpoint)
			.build()
			.toURI();

		MCLogger.log("Authorization URL is: " + authReqUri.toString());

		IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();

		// The internal browser was acting up for me. You can try it again if you like, since it would be nicer.
//		IWebBrowser browser = browserSupport.createBrowser(
//				IWorkbenchBrowserSupport.LOCATION_BAR | IWorkbenchBrowserSupport.NAVIGATION_BAR,
//				"mdt-auth", "Log in to ICP", "Log in to ICP");
//        browser.openURL(authReqUri.toURL());

		browserSupport.getExternalBrowser().openURL(authReqUri.toURL());
		// after user logs in, the browser calls-back to Eclipse, which will be handled by handleAuthorizationCallback below
	}

	/**
	 * Handle authorization callback by verifying it succeeded, and getting tokens if so - or reporting error.
	 *
	 * See references above
	 */
	void handleAuthorizationCallback(String uri) throws Exception {
		if (pendingAuth == null) {
			MCLogger.logError("Received auth callback but no pending auth is in progress");
			return;
		}

		State state = pendingAuth.state;
		String masterNodeIP = pendingAuth.masterNodeIP;
		pendingAuth = null;

		final URI cbUri = new URI(uri);
		final AuthorizationResponse response = AuthorizationResponse.parse(cbUri);

		if (!response.indicatesSuccess()) {
			MCLogger.log("Oh no, it failed");
			throw new Exception(response.toErrorResponse().getErrorObject().toString());
		}
		MCLogger.log("Auth response is a success");

		final AuthorizationSuccessResponse successResp = (AuthorizationSuccessResponse) response;
		if (!state.equals(successResp.getState())) {
			throw new Exception("State mismatch! Please restart the authorization process.");
		}
		MCLogger.log("State verified");


		AccessToken token = successResp.getAccessToken();

		// Expected: lifetime 12hr, scope 'openid', type 'bearer'
		String msg = String.format("Got access token with lifetime %d, scope %s, type %s",
			token.getLifetime(),
			token.getScope(),
			token.getType()
		);
		MCLogger.log(msg);

		saveToken(masterNodeIP, token);
	}
	
	public String getToken(String masterNodeIP) throws StorageException {
		ISecurePreferences secureStore = SecurePreferencesFactory.getDefault();
		String token = secureStore.get(TOKEN_PREFIX + masterNodeIP, "");
		if (token == null || token.isEmpty()) {
			return null;
		}
		return token;
	}

	private void saveToken(String masterNodeIP, AccessToken token) throws StorageException {
		// https://help.eclipse.org/photon/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2Fsecure_storage_dev.htm

		Date now = new Date(Calendar.getInstance().getTimeInMillis());
		Date expiry = new Date(now.getTime() + token.getLifetime() * 1000);
//		MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Authorization Success", "Access token will expire at " + sdf.format(expiry));

		ISecurePreferences secureStore = SecurePreferencesFactory.getDefault();
		secureStore.put(TOKEN_PREFIX + masterNodeIP, token.getValue(), true);
		secureStore.putLong(EXPIRY_PREFIX + masterNodeIP, expiry.getTime(), true);
	}
}
