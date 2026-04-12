/*
 * Copyright 2026 Juanro49
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONObject;
import org.juanro.autumandu.Application;

import java.util.Objects;

/**
 * Worker that performs the synchronization process.
 * Optimized for Room database compatibility and robust error handling.
 */
public class SyncWorker extends Worker {
    private static final String TAG = "SyncWorker";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType(Authenticator.ACCOUNT_TYPE);

        if (accounts.length == 0) {
            return Result.success();
        }

        Account account = accounts[0];
        String password = accountManager.getPassword(account);
        JSONObject settings = SyncProviders.getSyncProviderSettings(account);

        try {
            String authToken = accountManager.blockingGetAuthToken(account, Authenticator.AUTH_TOKEN_TYPE, true);
            AbstractSyncProvider syncProvider = SyncProviders.getSyncProviderByAccount(context, account);

            if (syncProvider == null) {
                Log.e(TAG, "Sync provider not found for account.");
                return Result.failure();
            }

            syncProvider.setup(account, password, authToken, settings);

            String localRev = syncProvider.getLocalFileRev();
            String remoteRev = syncProvider.getRemoteFileRev();

            if (Objects.equals(localRev, remoteRev)) {
                // Remote and local are the same, or both null (new account)
                if (localRev == null) {
                    // New account setup: upload local file as initial version
                    String newLocalRev = syncProvider.uploadFile();
                    syncProvider.setLocalFileRev(newLocalRev);
                }
            } else if (localRev == null) {
                // Remote exists but local has no record: download
                Application.closeDatabases();
                syncProvider.downloadFile();
                syncProvider.setLocalFileRev(remoteRev);
            } else {
                // Conflict or update: For simplicity, local wins (uploads and updates remote)
                // In a more complex scenario, we could implement a merge strategy here.
                String newLocalRev = syncProvider.uploadFile();
                syncProvider.setLocalFileRev(newLocalRev);
            }

            return Result.success();
        } catch (SyncAuthException e) {
            Log.e(TAG, "Authentication error during sync", e);
            return Result.failure(); // Failure for auth issues to avoid infinite retries without re-login
        } catch (Exception e) {
            Log.e(TAG, "Error during sync", e);
            return Result.retry();
        }
    }
}
