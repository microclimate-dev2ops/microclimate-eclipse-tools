package com.ibm.microclimate.core.internal.connection.auth;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import com.ibm.microclimate.core.internal.MCLogger;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResourceOwnerPasswordCredentialsGrant;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
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
	
	private static final Scope OIDC_SCOPE = new Scope(OIDCScopeValue.OPENID);

	private static final String TOKEN_PREFIX = "token-";
	private static final String EXPIRY_PREFIX = "expires-";

	// Only one 'pending auth' at a time -> starting a new one cancels the previous
	// only used by the auth flows that open the browser (implicit / code)
	private PendingAuth pendingAuth;

	private String getOIDCServerURL(String masterNodeIP) {
		// add "/.well-known/openid-configuration" to get the OIDC config object.
		// add "/registration/$CLIENT_ID to get the OIDC config object 
		// for our microclimate-tools client (requires oauthadmin credentials)
		return String.format("https://%s:8443/oidc/endpoint/OP", masterNodeIP);
	}
	
	/**
	 * Directly exchange the user's credentials for an access_token. No browser required.
	 * 
	 * https://connect2id.com/products/nimbus-oauth-openid-connect-sdk/examples/oauth/token-request#password
	 * https://static.javadoc.io/com.nimbusds/oauth2-oidc-sdk/5.11/com/nimbusds/openid/connect/sdk/package-summary.html
	 * 
	 * @throws AuthException in the case of bad credentials, or any unexpected auth failure.
	 */
	public void authorizePassword(String masterNodeIP, String username, String password) 
			throws Exception {
		
		MCLogger.log("Password grant authorization against master IP " + masterNodeIP);
		
		URI tokenEndpoint = new URI(getOIDCServerURL(masterNodeIP) + "/token");
		MCLogger.log("Token endpoint is " + tokenEndpoint);
		AuthorizationGrant pwGrant = new ResourceOwnerPasswordCredentialsGrant(username, new Secret(password));
		
		ClientAuthentication clientAuth = new ClientAuthentication(ClientAuthenticationMethod.NONE, new ClientID(CLIENT_ID)) {
			@Override
			public void applyTo(HTTPRequest req) {
//				String qs = req.getQuery();
//				if (qs == null) {
//					qs = "client_id=" + CLIENT_ID;
//				}
//				else {
//					qs += "&client_id=" + CLIENT_ID;
//				}
//				req.setQuery(qs);
//				MCLogger.log("The new QS is " + req.getQuery());
			}
		};
		
		TokenRequest tokenReq = new TokenRequest(tokenEndpoint, clientAuth, pwGrant, OIDC_SCOPE);
		
		HTTPRequest req = tokenReq.toHTTPRequest();
		// Timeout must be set or a bad request will hang
		req.setConnectTimeout(5000);
		MCLogger.log("REQUEST URL IS: " + req.getURL() + "?" + req.getQuery());

		TokenResponse res = TokenResponse.parse(req.send());

		if (!res.indicatesSuccess()) {
			MCLogger.log("Password auth failure");
			throw new AuthException(res.toErrorResponse());
		}

		MCLogger.log("Password auth success");
		AccessTokenResponse successResp = res.toSuccessResponse();
		saveToken(masterNodeIP, successResp.getTokens().getAccessToken());
	}
	
	/**
	 * Assemble the AuthorizationRequest and open the user's browser to the authorization login page.
	 * After the user logs in, Eclipse receives the callback 
	 * and passes the callback URI to handleAuthorizationCallback.
	 * <br>
	 * <br>
	 * https://connect2id.com/products/nimbus-oauth-openid-connect-sdk/examples/oauth/authorization-request#implicit-flow
	 * <br>
	 * https://www.javadoc.io/doc/com.nimbusds/oauth2-oidc-sdk/6.5
	 */
	public void authorizeImplicit(String masterNodeIP) 
			throws URISyntaxException, PartInitException, MalformedURLException {
		
		MCLogger.log("Implicit grant authorization against master IP " + masterNodeIP);
		
		// This is obtained from oidcServerUrl + /.well-known/openid-configuration
		// We should do a proper "discover" to that endpoint 
		// but since we only need /token and /authorize it's not really necessary.
		final URI authorizeEndpoint = new URI(getOIDCServerURL(masterNodeIP) + "/authorize");
		MCLogger.log("Authorize endpoint is " + authorizeEndpoint);

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
			.scope(OIDC_SCOPE)
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
	void handleAuthorizationCallback(String uri) 
			throws AuthException, ParseException, URISyntaxException, StorageException {
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
			throw new AuthException(response.toErrorResponse().getErrorObject().toString());
		}
		MCLogger.log("Implicit auth success");

		final AuthorizationSuccessResponse successResp = (AuthorizationSuccessResponse) response;
		if (!state.equals(successResp.getState())) {
			throw new AuthException("State mismatch! Please restart the authorization process.");
		}
		MCLogger.log("State verified");
		saveToken(masterNodeIP, successResp.getAccessToken());
	}

	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	
	private void saveToken(String masterNodeIP, AccessToken token) throws StorageException {
		// Expected: lifetime 12hr, scope 'openid', type 'bearer'
		MCLogger.log(String.format("Got access token with lifetime %d, scope %s, type %s",
			token.getLifetime(),
			token.getScope(),
			token.getType()
		));
		
		// https://help.eclipse.org/photon/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2Fsecure_storage_dev.htm

		Date now = new Date(Calendar.getInstance().getTimeInMillis());
		Date expiry = new Date(now.getTime() + token.getLifetime() * 1000);
		MessageDialog.openInformation(Display.getDefault().getActiveShell(), 
				"Authorization Success", 
				"Access token will expire at " + sdf.format(expiry));

		ISecurePreferences secureStore = SecurePreferencesFactory.getDefault();
		secureStore.put(TOKEN_PREFIX + masterNodeIP, token.getValue(), true);
		secureStore.putLong(EXPIRY_PREFIX + masterNodeIP, expiry.getTime(), true);
	}
}
