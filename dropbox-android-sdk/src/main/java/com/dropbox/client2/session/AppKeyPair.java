package com.dropbox.client2.session;

/**
 * <p>
 * Holds your app's key and secret.
 * </p>
 */
public final class AppKeyPair extends TokenPair {

    private static final long serialVersionUID = -5526503075188547139L;

    public AppKeyPair(String key, String secret) {
        super(key, secret);
    }
}
