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
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Activity;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import me.kuehle.carreport.Application;
import me.kuehle.carreport.R;
import me.kuehle.carreport.gui.AuthenticatorAddAccountActivity;
import me.kuehle.carreport.gui.dialog.SetupWebDavSyncDialogActivity;
import me.kuehle.carreport.util.FileCopyUtil;
import me.kuehle.carreport.util.WebDavClient;
import me.kuehle.carreport.util.sync.AbstractSyncProvider;

public class WebDavSyncProvider extends AbstractSyncProvider {
    public static final String KEY_WEB_DAV_URL = "webDavUrl";

    private static final String TAG = "WebDavSyncProvider";
    private static final int REQUEST_SETUP = 1000;

    private WebDavClient mWebDavClient;

    @Override
    public long getId() {
        return 3;
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_c_sync_webdav_64dp;
    }

    @Override
    public String getName() {
        return "WebDAV";
    }

    @Override
    public void setup(@Nullable Account account, @Nullable String password, @Nullable String authToken, @Nullable JSONObject settings) {
        if (account != null && password != null && settings != null) {
            String url = settings.optString(KEY_WEB_DAV_URL);
            mWebDavClient = new WebDavClient(url, account.name, password);
        } else {
            mWebDavClient = null;
        }
    }

    @Override
    public void startAuthentication(AuthenticatorAddAccountActivity activity) {
        Intent setupIntent = new Intent(activity, SetupWebDavSyncDialogActivity.class);
        activity.startActivityForResult(setupIntent, REQUEST_SETUP);
    }

    @Override
    public void continueAuthentication(AuthenticatorAddAccountActivity activity, int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_SETUP)
            if (resultCode == Activity.RESULT_OK && data != null) {
                String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                String password = data.getStringExtra(AccountManager.KEY_PASSWORD);
                String url = data.getStringExtra(KEY_WEB_DAV_URL);

                JSONObject settings = new JSONObject();
                try {
                    settings.put(KEY_WEB_DAV_URL, url);
                } catch (JSONException e) {
                    Log.e(TAG, "Could not store url in settings JSONObject", e);
                }

                activity.onAuthenticationResult(accountName, password, null, settings);
            } else {
                activity.onAuthenticationResult(null, null, null, null);
            }
    }

    @Override
    public String getRemoteFileRev() throws Exception {
        File localFile = getLocalFile();
        Date lastModified = mWebDavClient.getLastModified(localFile.getName());
        return lastModified == null ? null : String.valueOf(lastModified.getTime());
    }

    @Override
    public String uploadFile() throws Exception {
        File localFile = getLocalFile();
        File tempFile = new File(Application.getContext().getCacheDir(), getClass().getSimpleName());
        if (!FileCopyUtil.copyFile(localFile, tempFile)) {
            throw new Exception();
        }

        try {
            if (!mWebDavClient.upload(tempFile, localFile.getName(), "application/x-sqlite")) {
                throw new NetworkErrorException("File upload failed.");
            }

            return getRemoteFileRev();
        } catch (IOException e) {
            throw new NetworkErrorException(e);
        } finally {
            if (!tempFile.delete()) {
                Log.w(TAG, "Could not delete temp file after uploading.");
            }
        }
    }

    @Override
    public void downloadFile() throws Exception {
        File localFile = getLocalFile();
        File tempFile = new File(Application.getContext().getCacheDir(), getClass().getSimpleName());

        try {
            mWebDavClient.download(localFile.getName(), tempFile);
            if (!FileCopyUtil.copyFile(tempFile, localFile)) {
                throw new Exception();
            }
        } catch (IOException e) {
            throw new NetworkErrorException(e);
        } finally {
            if (!tempFile.delete()) {
                Log.w(TAG, "Could not delete temp file after downloading.");
            }
        }
    }
}
