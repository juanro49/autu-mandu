package org.juanro.autumandu.util.sync;

import android.accounts.Account;
import org.json.JSONObject;

/**
 * Interface for receiving authentication results.
 */
public interface AuthenticationFinishedListener {
    void onAuthenticationFinished(Account account, String password, String authToken, JSONObject settings);
}
