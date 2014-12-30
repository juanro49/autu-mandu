/*
 * Copyright 2012 Jan Kühle
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

package me.kuehle.carreport.util.backup;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.activeandroid.Cache;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DropboxFileInfo;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.session.AppKeyPair;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import me.kuehle.carreport.Application;
import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.gui.dialog.ProgressDialogFragment;

public class DropboxSynchronizationProvider extends AbstractSynchronizationProvider {
    private static final String TAG = "DropboxSynchronizationProvider";

    private static final String APP_KEY = "a6edub2n9b029if";
    private static final String APP_SECRET = "1cw56rcn1bbnb7f";

    private DropboxAPI<AndroidAuthSession> mDBApi;

    public DropboxSynchronizationProvider(Context context) {
        super(context);

        Preferences prefs = new Preferences(mContext);

        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
        String token = prefs.getDropboxAccessToken();

        AndroidAuthSession session = new AndroidAuthSession(appKeys, token);
        mDBApi = new DropboxAPI<>(session);
    }

    @Override
    public String getAccountName() {
        Preferences prefs = new Preferences(mContext);
        return prefs.getDropboxAccount();
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_sync_dropbox;
    }

    @Override
    public String getName() {
        return "Dropbox";
    }

    @Override
    public boolean isAuthenticated() {
        return mDBApi.getSession().isLinked();
    }

    @Override
    protected void onStartAuthentication() {
        mDBApi.getSession().startOAuth2Authentication(mContext);
    }

    @Override
    protected void onContinueAuthentication(int requestCode, int resultCode,
                                            Intent data) {
        if (!mAuthenticationInProgess) {
            return;
        }

        if (mDBApi.getSession().authenticationSuccessful()) {
            ProgressDialogFragment
                    .newInstance(
                            mContext.getString(R.string.alert_sync_finishing_authentication))
                    .show(mAuthenticationFragmentManager, "progress");

            try {
                mDBApi.getSession().finishAuthentication();

                String token = mDBApi.getSession().getOAuth2AccessToken();
                Preferences prefs = new Preferences(mContext);
                prefs.setDropboxAccessToken(token);

                new AsyncTask<Void, Void, Boolean>() {
                    private String accountName;
                    private boolean remoteDataAvailable = false;

                    @Override
                    protected Boolean doInBackground(Void... params) {
                        // Load account name.
                        try {
                            accountName = mDBApi.accountInfo().displayName;
                        } catch (DropboxUnlinkedException e) {
                            unlink(null);
                            return false;
                        } catch (DropboxException e) {
                            accountName = null;
                        }

                        Preferences prefs = new Preferences(mContext);
                        prefs.setDropboxAccount(accountName);
                        if (accountName == null) {
                            return false;
                        }

                        // Check if remote data is available.
                        File localFile = new File(Cache.openDatabase().getPath());
                        try {
                            Entry remoteEntry = mDBApi.metadata("/" + localFile.getName(), 1, null,
                                    false, null);
                            if (!remoteEntry.isDeleted) {
                                remoteDataAvailable = true;
                            }
                        } catch (DropboxServerException e) {
                            if (e.error == DropboxServerException._401_UNAUTHORIZED
                                    || e.error == DropboxServerException._403_FORBIDDEN) {
                                return false;
                            }
                        } catch (DropboxUnlinkedException e) {
                            unlink(null);
                            return false;
                        } catch (DropboxException e) {
                            return false;
                        }

                        return true;
                    }

                    @Override
                    protected void onPostExecute(Boolean result) {
                        mAuthenticationFragmentManager
                                .executePendingTransactions();
                        ((ProgressDialogFragment) mAuthenticationFragmentManager
                                .findFragmentByTag("progress")).dismiss();

                        authenticationFinished(result, remoteDataAvailable);
                    }
                }.execute();
            } catch (IllegalStateException e) {
                authenticationFinished(false, false);
            }
        } else {
            authenticationFinished(false, false);
        }
    }

    @Override
    protected boolean onSynchronize(int option) {
        Preferences prefs = new Preferences(mContext);
        File localFile = new File(Cache.openDatabase().getPath());
        String localRev = prefs.getDropboxLocalRev();

        String remoteRev = null;
        try {
            Entry remoteEntry = mDBApi.metadata("/" + localFile.getName(), 1,
                    null, false, null);
            if (!remoteEntry.isDeleted) {
                remoteRev = remoteEntry.rev;
            }
        } catch (DropboxServerException e) {
            if (e.error != 404) {
                return false;
            }
        } catch (DropboxUnlinkedException e) {
            unlink(null);
            return false;
        } catch (DropboxException e) {
            return false;
        }

        if (option == SYNC_UPLOAD
                || remoteRev == null
                || (option == SYNC_NORMAL && remoteRev.equals(localRev))) {
            // --------------------------------------------------
            // Upload
            // --------------------------------------------------

            if (!copyFile(localFile, mTempFile)) {
                return false;
            }

            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(mTempFile);
                Entry remoteEntry = mDBApi.putFile("/" + localFile.getName(),
                        inputStream, mTempFile.length(), remoteRev, null);
                prefs.setDropboxLocalRev(remoteEntry.rev);
            } catch (DropboxException e) {
                return false;
            } catch (FileNotFoundException e) {
                return false;
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Could not close input stream after uploading file.", e);
                    }
                }

                if (!mTempFile.delete()) {
                    Log.w(TAG, "Could not delete temp file after uploading.");
                }
            }
        } else {
            // --------------------------------------------------
            // Download
            // --------------------------------------------------

            FileOutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(mTempFile);
                DropboxFileInfo info = mDBApi.getFile(
                        "/" + localFile.getName(), null, outputStream, null);
                if (copyFile(mTempFile, localFile)) {
                    prefs.setDropboxLocalRev(info.getMetadata().rev);
                    Application.reinitializeDatabase();
                }
            } catch (DropboxException e) {
                return false;
            } catch (FileNotFoundException e) {
                return false;
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Could not close output stream after downloading file.", e);
                    }
                }

                if (!mTempFile.delete()) {
                    Log.w(TAG, "Could not delete temp file after downloading.");
                }
            }
        }

        return true;
    }

    @Override
    protected void onUnlink() {
        mDBApi.getSession().unlink();

        Preferences prefs = new Preferences(mContext);
        prefs.setDropboxAccount(null);
        prefs.setDropboxAccessToken(null);
        prefs.setDropboxLocalRev(null);

        unlinkingFinished();
    }
}
