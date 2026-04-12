package org.juanro.autumandu.util.sync.provider;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.juanro.autumandu.util.sync.AuthenticationFinishedListener;
import org.juanro.autumandu.util.sync.Authenticator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.json.JSONObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class WebDavSyncProviderTest {

    @Test
    public void testProviderProperties() {
        WebDavSyncProvider provider = new WebDavSyncProvider();
        assertEquals("WebDAV", provider.getName());
        assertNotNull(provider.getId());
    }

    @Test
    public void testHandleAuthenticationResultAccountType() {
        final Account[] capturedAccount = new Account[1];
        AuthenticationFinishedListener listener = new AuthenticationFinishedListener() {
            @Override
            public void onAuthenticationFinished(Account account, String password, String authToken, JSONObject settings) {
                capturedAccount[0] = account;
            }
        };

        Intent data = new Intent();
        data.putExtra(AccountManager.KEY_ACCOUNT_NAME, "test@example.com");
        data.putExtra(AccountManager.KEY_PASSWORD, "password123");
        data.putExtra(WebDavSyncProvider.KEY_WEB_DAV_URL, "https://webdav.example.com");

        // RESULT_OK is -1 in Android
        WebDavSyncProvider.handleAuthenticationResult(listener, -1, data);

        assertNotNull("Account should have been finished", capturedAccount[0]);
        assertEquals("Account type must match Authenticator.ACCOUNT_TYPE",
                Authenticator.ACCOUNT_TYPE, capturedAccount[0].type);
        assertEquals("org.juanro.autumandu.sync", capturedAccount[0].type);
    }
}
