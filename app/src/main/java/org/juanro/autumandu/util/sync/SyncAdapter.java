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

package org.juanro.autumandu.util.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;

import org.juanro.autumandu.Application;
import org.juanro.autumandu.BuildConfig;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = "SyncAdapter";

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        if (BuildConfig.DEBUG) Log.d(TAG, "onPerformSync");

        AccountManager accountManager = AccountManager.get(getContext());

        String password = accountManager.getPassword(account);
        JSONObject settings = SyncProviders.getSyncProviderSettings(account);

        String authToken;
        try {
            authToken = accountManager.blockingGetAuthToken(account, Authenticator.AUTH_TOKEN_TYPE,
                    true);
        } catch (OperationCanceledException | IOException | AuthenticatorException e) {
            Log.e(TAG, "Error getting auth token.", e);
            syncResult.stats.numAuthExceptions++;
            return;
        }

        AbstractSyncProvider syncProvider = SyncProviders.getSyncProviderByAccount(getContext(),
                account);

        try {
            syncProvider.setup(account, password, authToken, settings);

            String localRev = syncProvider.getLocalFileRev();
            String remoteRev = syncProvider.getRemoteFileRev();
            if (localRev == null || localRev.equals(remoteRev)) {
                String newLocalRev = syncProvider.uploadFile();
                syncProvider.setLocalFileRev(newLocalRev);
            } else {
                Application.closeDatabases();
                syncProvider.downloadFile();
                syncProvider.setLocalFileRev(remoteRev);
            }
        } catch (SyncAuthException e) {
            Log.e(TAG, "Auth error while syncing.", e);
            syncResult.stats.numAuthExceptions++;
        } catch (SyncIoException e) {
            Log.e(TAG, "IO error while syncing.", e);
            syncResult.stats.numIoExceptions++;
        } catch (SyncParseException e) {
            Log.e(TAG, "Parse error while syncing.", e);
            syncResult.stats.numParseExceptions++;
        }
    }
}
