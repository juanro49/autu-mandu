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

import android.accounts.Account;
import android.accounts.NetworkErrorException;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.session.AppKeyPair;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import me.kuehle.carreport.Application;
import me.kuehle.carreport.R;
import me.kuehle.carreport.gui.AuthenticatorAddAccountActivity;
import me.kuehle.carreport.util.FileCopyUtil;

public class DropboxSyncProvider extends AbstractSyncProvider {
    private static final String TAG = "DropboxSyncProvider";

    private static final String APP_KEY = "a6edub2n9b029if";
    private static final String APP_SECRET = "1cw56rcn1bbnb7f";

    private DropboxAPI<AndroidAuthSession> mDBApi;

    @Override
    public long getId() {
        return 1;
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
    public void setup(@Nullable Account account, @Nullable String password, @Nullable String authToken, @Nullable JSONObject settings) {
        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys, authToken);
        mDBApi = new DropboxAPI<>(session);
    }

    @Override
    public void startAuthentication(AuthenticatorAddAccountActivity activity) {
        mDBApi.getSession().startOAuth2Authentication(activity);
    }

    @Override
    public void continueAuthentication(final AuthenticatorAddAccountActivity activity,
                                       final int requestCode, final int resultCode,
                                       final @Nullable Intent data) {
        if (mDBApi.getSession().authenticationSuccessful()) {
            try {
                mDBApi.getSession().finishAuthentication();

                new AsyncTask<Void, Void, String>() {
                    @Override
                    protected String doInBackground(Void... params) {
                        try {
                            return mDBApi.accountInfo().displayName;
                        } catch (DropboxException e) {
                            return null;
                        }
                    }

                    @Override
                    protected void onPostExecute(String result) {
                        activity.onAuthenticationResult(result,
                                mDBApi.getSession().getOAuth2AccessToken(), null, null);
                    }
                }.execute();
            } catch (IllegalStateException e) {
                activity.onAuthenticationResult(null, null, null, null);
            }
        } else {
            activity.onAuthenticationResult(null, null, null, null);
        }
    }

    @Override
    public String getRemoteFileRev() throws Exception {
        try {
            File localFile = getLocalFile();
            DropboxAPI.Entry remoteEntry = mDBApi.metadata("/" + localFile.getName(), 1, null, false, null);
            if (!remoteEntry.isDeleted) {
                return remoteEntry.rev;
            }
        } catch (DropboxServerException e) {
            if (e.error != 404) {
                throw new NetworkErrorException(e);
            }
        } catch (DropboxUnlinkedException e) {
            throw new AccountUnlinkedException(e);
        } catch (DropboxException e) {
            throw new Exception(e);
        }

        return null;
    }

    @Override
    public String uploadFile() throws Exception {
        File localFile = getLocalFile();
        File tempFile = new File(Application.getContext().getCacheDir(), getClass().getSimpleName());
        if (!FileCopyUtil.copyFile(localFile, tempFile)) {
            throw new Exception();
        }

        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(tempFile);
            DropboxAPI.Entry remoteEntry = mDBApi.putFile("/" + localFile.getName(), inputStream,
                    tempFile.length(), getRemoteFileRev(), null);
            return remoteEntry.rev;
        } catch (DropboxServerException e) {
            throw new NetworkErrorException(e);
        } catch (DropboxUnlinkedException e) {
            throw new AccountUnlinkedException(e);
        } catch (DropboxException | FileNotFoundException e) {
            throw new Exception(e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Could not close input stream after uploading file.", e);
                }
            }

            if (!tempFile.delete()) {
                Log.w(TAG, "Could not delete temp file after uploading.");
            }
        }
    }

    @Override
    public void downloadFile() throws Exception {
        File localFile = getLocalFile();
        File tempFile = new File(Application.getContext().getCacheDir(), getClass().getSimpleName());

        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(tempFile);
            mDBApi.getFile("/" + localFile.getName(), null, outputStream, null);
            if (!FileCopyUtil.copyFile(tempFile, localFile)) {
                throw new Exception();
            }
        } catch (DropboxServerException e) {
            throw new NetworkErrorException(e);
        } catch (DropboxUnlinkedException e) {
            throw new AccountUnlinkedException(e);
        } catch (DropboxException | FileNotFoundException e) {
            throw new Exception(e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Could not close output stream after downloading file.", e);
                }
            }

            if (!tempFile.delete()) {
                Log.w(TAG, "Could not delete temp file after downloading.");
            }
        }
    }
}
