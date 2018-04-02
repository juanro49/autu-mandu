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

package me.kuehle.carreport.util.sync.provider;

import android.accounts.Account;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.NetworkIOException;
import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.GetMetadataErrorException;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.WriteMode;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import me.kuehle.carreport.Application;
import me.kuehle.carreport.BuildConfig;
import me.kuehle.carreport.R;
import me.kuehle.carreport.gui.AuthenticatorAddAccountActivity;
import me.kuehle.carreport.util.FileCopyUtil;
import me.kuehle.carreport.util.sync.AbstractSyncProvider;
import me.kuehle.carreport.util.sync.SyncIoException;
import me.kuehle.carreport.util.sync.SyncParseException;

public class DropboxSyncProvider extends AbstractSyncProvider {
    private static final String TAG = "DropboxSyncProvider";

    private static final String APP_KEY = "a6edub2n9b029if";

    private DbxClientV2 mDbxClient;

    @Override
    public long getId() {
        return 1;
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_c_sync_dropbox_64dp;
    }

    @Override
    public String getName() {
        return "Dropbox";
    }

    @Override
    public void setup(@Nullable Account account, @Nullable String password, @Nullable String authToken, @Nullable JSONObject settings) {
        if (authToken == null) {
            mDbxClient = null;
        } else {
            String userLocale = Locale.getDefault().toString();
            DbxRequestConfig requestConfig = new DbxRequestConfig(BuildConfig.APPLICATION_ID, userLocale);
            mDbxClient = new DbxClientV2(requestConfig, authToken);
        }
    }

    @Override
    public void startAuthentication(AuthenticatorAddAccountActivity activity) {
        Auth.startOAuth2Authentication(activity, APP_KEY);
    }

    @Override
    public void continueAuthentication(final AuthenticatorAddAccountActivity activity,
                                       final int requestCode, final int resultCode,
                                       final @Nullable Intent data) {
        final String accessToken = Auth.getOAuth2Token();
        if (accessToken != null) {
            try {
                setup(null, null, accessToken, null);

                new AsyncTask<Void, Void, String>() {
                    @Override
                    protected String doInBackground(Void... params) {
                        try {
                            return mDbxClient.users().getCurrentAccount().getName().getDisplayName();
                        } catch (DbxException e) {
                            return null;
                        }
                    }

                    @Override
                    protected void onPostExecute(String result) {
                        activity.onAuthenticationResult(result, null, accessToken, null);
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
    public String getRemoteFileRev() throws SyncIoException, SyncParseException {
        try {
            File localFile = getLocalFile();
            Metadata remoteMetadata = mDbxClient.files().getMetadata("/" + localFile.getName());

            if (remoteMetadata instanceof FileMetadata) {
                return ((FileMetadata) remoteMetadata).getRev();
            } else {
                return null;
            }
        } catch (NetworkIOException e) {
            throw new SyncIoException(e);
        } catch (GetMetadataErrorException e) {
            if (e.errorValue != null &&
                    e.errorValue.getPathValue() != null &&
                    e.errorValue.getPathValue().isNotFound()) {
                return null;
            } else {
                throw new SyncParseException(e);
            }
        } catch (DbxException e) {
            throw new SyncParseException(e);
        }
    }

    @Override
    public String uploadFile() throws SyncIoException, SyncParseException {
        File localFile = getLocalFile();
        File tempFile = new File(Application.getContext().getCacheDir(), getClass().getSimpleName());

        try {
            if (!FileCopyUtil.copyFile(localFile, tempFile)) {
                throw new SyncParseException("Copying database to temp file failed.");
            }

            try (FileInputStream inputStream = new FileInputStream(tempFile)) {
                FileMetadata remoteMetadata = mDbxClient.files()
                        .uploadBuilder("/" + localFile.getName())
                        .withMode(WriteMode.OVERWRITE)
                        .start()
                        .uploadAndFinish(inputStream);
                return remoteMetadata.getRev();
            }
        } catch (NetworkIOException e) {
            throw new SyncIoException(e);
        } catch (DbxException | IOException e) {
            throw new SyncParseException(e);
        } finally {
            if (!tempFile.delete()) {
                Log.w(TAG, "Could not delete temp file after uploading.");
            }
        }
    }

    @Override
    public void downloadFile() throws SyncIoException, SyncParseException {
        File localFile = getLocalFile();
        File tempFile = new File(Application.getContext().getCacheDir(), getClass().getSimpleName());

        try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            mDbxClient.files()
                    .download("/" + localFile.getName())
                    .download(outputStream);
            if (!FileCopyUtil.copyFile(tempFile, localFile)) {
                throw new IOException();
            }
        } catch (NetworkIOException e) {
            throw new SyncIoException(e);
        } catch (DbxException | IOException e) {
            throw new SyncParseException(e);
        } finally {
            if (!tempFile.delete()) {
                Log.w(TAG, "Could not delete temp file after downloading.");
            }
        }
    }
}
