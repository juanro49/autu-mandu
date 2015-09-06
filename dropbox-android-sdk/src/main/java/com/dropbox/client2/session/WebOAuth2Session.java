/*
 * Copyright (c) 2013 Dropbox, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package com.dropbox.client2.session;

import java.util.Map;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.RESTUtility;
import com.dropbox.client2.RESTUtility.RequestMethod;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxServerException;

/**
 * Keeps track of a logged in user contains configuration options for the
 * {@link DropboxAPI}. This type of {@link Session} uses the OAuth2
 * "auth code flow" to authenticate users:
 * <ol>
 *   <li>A redirect URL are retrieved using
 *   {@link WebOAuth2Session#getAuthorizeURL()} or
 *   {@link WebOAuth2Session#getAuthorizeURL(String, String)}.</li>
 *   <li>You redirect the
 *   user to the redirect URL where they will authenticate with Dropbox and
 *   grant your app permission to access their account.</li>
 *   <li>Dropbox will redirect back to your site with a auth code in the query
 *   string if it was provided a URL to do so (otherwise, you have to ask the
 *   user for the auth code when he/she is done).</li>
 *   <li>The user's access token is set on this session when you call
 *   {@link WebOAuth2Session#retrieveWebAccessToken(String, String)} with the auth code
 *   from Dropbox and the same redirect URL you passed to
 *   {@link WebOAuth2Session#getAuthorizeURL(String, String)}. You have a limited amount
 *   of time to make this call or the request token will expire.</li> </ol>
 */
public class WebOAuth2Session extends AbstractSession {
    /**
     * Creates a new web auth session with the given app key pair.
     * The session will not be linked because it has no access token.
     */
    public WebOAuth2Session(AppKeyPair appKeyPair) {
        super(appKeyPair);
    }

    /**
     * Creates a new web auth session with the given app key pair.
     * The session will also be linked to the account corresponding to the
     * given access token.
     */
    public WebOAuth2Session(AppKeyPair appKeyPair, String oauth2AccessToken) {
        super(appKeyPair, oauth2AccessToken);
    }

    public String getAuthorizeURL() {
        return getAuthorizeURL(null, null);
    }

    /**
     * Starts an authentication request with Dropbox servers and computes the
     * URL the user needs to visit to authorize your app.
     *
     * @param redirectUrl the URL to which Dropbox will redirect the user after
     *         he/she has authenticated on the Dropbox site. If the user
     *         authorizes your app, the server will pass a <code>code</code>
     *         query parameter that you should pass to
     *         {@link WebOAuth2Session#retrieveWebAccessToken(String, String)} to
     *         finish authentication. If the user denies access to your app, the
     *         query will contain <code>error</code> and
     *         <code>error_description</code> parameters. Make sure this URL
     *         appears in the "Redirect URIs" section on the App Console page
     *         for your app. If you pass <code>null</code> for this parameter,
     *         the user will be presented with the Auth code.
     * @param csrfToken If not <code>null</code>, this will appear as the
     *         <code>state</code> query parameter when the server redirects the
     *         user to <code>redirectURL</code>. This should be a short string
     *         tied to the authentication state of the user in your system. When
     *         a user fetches <code>redirectURL</code>, you should verify the
     *         state parameter is the same what you pass to this function. This
     *         protects your application against cross-site request
     *         forgery. While technically optional, this parameter should always
     *         be passed when using redirects.
     *
     * @return the URL to redirect the user to to authorize your app
     */
    public String getAuthorizeURL(String redirectUrl, String csrfToken) {
        String[] args = new String[] {
            "response_type", "code",
            "client_id", getAppKeyPair().key,
            "redirect_uri", redirectUrl,
            "state", csrfToken,
        };
        String path = "/oauth2/authorize";
        return RESTUtility.buildURL(getWebServer(), DropboxAPI.VERSION, path, args);
    }

    /**
     * Obtain an access token for a user. This is the second step of OAuth2
     * "Auth code flow".  After you send the user to the authorize URL, you can
     * use the authorized request code with this method to get the access token
     * to use for future operations. The access token is stored on the session
     * object.
     *
     * @param code the auth code sent to you by the Dropbox server
     * @param redirectUrl This should be exactly the same as was passed to
     *         getAuthorizeURL. (Possibly null).
     *
     * @return the access token for the user
     *
     * @throws DropboxServerException if the server responds with an error
     *         code. See the constants in {@link DropboxServerException} for
     *         the meaning of each error code. The most common error codes you
     *         can expect from this call are 401 (bad request token), 403 (bad
     *         app key pair), 500, 502, and 503 (all for internal Dropbox
     *         server issues).
     * @throws DropboxIOException if any network-related error occurs.
     * @throws DropboxParseException if a malformed or unknown response was
     *         received from the server.
     * @throws DropboxException for any other unknown errors. This is also a
     *         superclass of all other Dropbox exceptions, so you may want to
     *         only catch this exception which signals that some kind of error
     *         occurred.
     */
    public String retrieveWebAccessToken(String code, String redirectUrl) throws DropboxException {
        if (code == null)
            throw new IllegalArgumentException("'code' must not be null");
        String[] args = new String[] {
            "grant_type", "authorization_code",
            "code", code,
            "client_id", getAppKeyPair().key,
            "client_secret", getAppKeyPair().secret,
            "redirect_uri", redirectUrl,
        };
        String path = "/oauth2/token";
        String url = RESTUtility.buildURL(getAPIServer(), DropboxAPI.VERSION, path, args);
        HttpUriRequest req = new HttpPost(url);
        HttpResponse resp = RESTUtility.execute(this, req);
        @SuppressWarnings("unchecked")
        Map<String, Object> respData = (Map<String, Object>)RESTUtility.parseAsJSON(resp);
        String accessToken = (String)respData.get("access_token");
        setOAuth2AccessToken(accessToken);
        return accessToken;
    }
}
