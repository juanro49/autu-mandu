/*
 * Copyright 2015 Jan Kühle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.kuehle.carreport.util.sync;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

import me.kuehle.carreport.Application;
import me.kuehle.carreport.gui.AuthenticatorAddAccountActivity;

public class Authenticator extends AbstractAccountAuthenticator {
    public static final String ACCOUNT_TYPE = "me.kuehle.carreport.sync";
    public static final String AUTH_TOKEN_TYPE = "Default";
    public static final String KEY_SYNC_PROVIDER = ACCOUNT_TYPE + ".provider";
    public static final String KEY_SYNC_PROVIDER_SETTINGS = ACCOUNT_TYPE + ".provider.settings";

    public static AbstractSyncProvider[] SYNC_PROVIDERS = {
            new DropboxSyncProvider(),
            new GoogleDriveSyncProvider(),
            new WebDavSyncProvider()
    };

    public static AbstractSyncProvider getSyncProviderByAccount(Account account) {
        AccountManager accountManager = AccountManager.get(Application.getContext());

        long providerId = Long.parseLong(accountManager.getUserData(account,
                Authenticator.KEY_SYNC_PROVIDER));
        return Authenticator.getSyncProviderById(providerId);
    }

    public static AbstractSyncProvider getSyncProviderById(long id) {
        for (AbstractSyncProvider provider : SYNC_PROVIDERS) {
            if (provider.getId() == id) {
                return provider;
            }
        }

        return null;
    }

    public static JSONObject getSyncProviderSettings(Account account) {
        AccountManager accountManager = AccountManager.get(Application.getContext());
        String settings = accountManager.getUserData(account, KEY_SYNC_PROVIDER_SETTINGS);
        try {
            return new JSONObject(settings);
        } catch (JSONException e) {
            return null;
        }
    }

    public static void setSyncProviderSettings(Account account, JSONObject settings) {
        AccountManager accountManager = AccountManager.get(Application.getContext());
        if (settings != null) {
            accountManager.setUserData(account, KEY_SYNC_PROVIDER_SETTINGS, settings.toString());
        } else {
            accountManager.setUserData(account, KEY_SYNC_PROVIDER_SETTINGS, null);
        }
    }

    private Context mContext;

    public Authenticator(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        AccountManager accountManager = AccountManager.get(mContext);
        if (accountManager.getAccountsByType(ACCOUNT_TYPE).length > 0) {
            throw new UnsupportedOperationException();
        }

        return createAuthenticatorAddAccountActivityIntentBundle(response);
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        AccountManager accountManager = AccountManager.get(mContext);
        String authToken = accountManager.peekAuthToken(account, authTokenType);

        Bundle result = new Bundle();
        result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
        result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
        return result;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        throw new UnsupportedOperationException();
    }

    private Bundle createAuthenticatorAddAccountActivityIntentBundle(AccountAuthenticatorResponse response) {
        Intent intent = new Intent(mContext, AuthenticatorAddAccountActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }
}
