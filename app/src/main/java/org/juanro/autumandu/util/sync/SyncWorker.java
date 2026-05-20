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
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONObject;
import org.juanro.autumandu.AutuManduApplication;
import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;

import java.util.Objects;

/**
 * Worker that performs the synchronization process.
 * Optimized for Room database compatibility and robust error handling.
 */
public class SyncWorker extends Worker {
    private static final String TAG = "SyncWorker";

    public static final String KEY_FORCE_UPLOAD = "force_upload";
    public static final String KEY_FORCE_DOWNLOAD = "force_download";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType(Authenticator.ACCOUNT_TYPE);
        Log.i(TAG, "SyncWorker started. Found " + accounts.length + " accounts of type " + Authenticator.ACCOUNT_TYPE);

        if (accounts.length == 0) {
            return Result.success();
        }

        Account account = accounts[0];
        Log.i(TAG, "Using account: " + account.name);
        String password = accountManager.getPassword(account);
        JSONObject settings = SyncProviders.getSyncProviderSettings(context, account);

        try {
            String authToken = accountManager.blockingGetAuthToken(account, Authenticator.AUTH_TOKEN_TYPE, true);
            AbstractSyncProvider syncProvider = SyncProviders.getSyncProviderByAccount(context, account);

            if (syncProvider == null) {
                Log.e(TAG, "Sync provider not found for account " + account.name + ". Check SyncProviders logs.");
                return Result.failure();
            }

            syncProvider.setup(account, password, authToken, settings);

            if (getInputData().getBoolean(KEY_FORCE_UPLOAD, false)) {
                Log.i(TAG, "Force upload requested.");
                upload(syncProvider);
                return Result.success();
            }

            if (getInputData().getBoolean(KEY_FORCE_DOWNLOAD, false)) {
                Log.i(TAG, "Force download requested.");
                String remoteRev = syncProvider.getRemoteFileRev();
                download(syncProvider, remoteRev);
                return Result.success();
            }

            String localRev = syncProvider.getLocalFileRev();
            String remoteRev = syncProvider.getRemoteFileRev();
            long localLastModifiedAtLastSync = syncProvider.getLocalFileLastModified();
            long currentLocalLastModified = syncProvider.getLocalFile().lastModified();
            boolean localChanged = currentLocalLastModified > localLastModifiedAtLastSync;

            Log.i(TAG, String.format("Sync status: localRev=%s, remoteRev=%s, localChanged=%b",
                    localRev, remoteRev, localChanged));

            if (Objects.equals(localRev, remoteRev)) {
                if (localChanged || localRev == null) {
                    Log.i(TAG, "Local changes detected or initial sync. Uploading...");
                    showToast(context, R.string.toast_sync_uploading);
                    upload(syncProvider);
                } else {
                    Log.i(TAG, "No changes detected. Sync complete.");
                    showToast(context, R.string.toast_sync_complete_no_changes);
                }
            } else {
                if (localRev == null || !localChanged) {
                    Log.i(TAG, "Remote changes detected and no local changes (or new account). Downloading...");
                    showToast(context, R.string.toast_sync_downloading);
                    download(syncProvider, remoteRev);
                } else {
                    // Conflict: localRev != remoteRev AND localChanged
                    Log.i(TAG, "Conflict detected (both local and remote changed).");
                    showToast(context, R.string.toast_sync_conflict_detected);
                    new Preferences(context).setSyncConflict(true);
                    SyncManager.showConflictNotification(context);
                    androidx.work.Data outputData = new androidx.work.Data.Builder()
                            .putBoolean("conflict", true)
                            .build();
                    return Result.success(outputData);
                }
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

    private void showToast(Context context, @StringRes int resId) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(context, resId, Toast.LENGTH_SHORT).show());
    }

    private void upload(AbstractSyncProvider syncProvider) throws SyncAuthException, SyncIoException, SyncParseException {
        String newRemoteRev = syncProvider.uploadFile();
        syncProvider.setLocalFileRev(newRemoteRev);
        syncProvider.setLocalFileLastModified(syncProvider.getLocalFile().lastModified());
    }

    private void download(AbstractSyncProvider syncProvider, String remoteRev) throws SyncAuthException, SyncIoException, SyncParseException {
        AutuManduApplication.closeDatabases();
        syncProvider.downloadFile();
        syncProvider.setLocalFileRev(remoteRev);
        syncProvider.setLocalFileLastModified(syncProvider.getLocalFile().lastModified());
    }
}
