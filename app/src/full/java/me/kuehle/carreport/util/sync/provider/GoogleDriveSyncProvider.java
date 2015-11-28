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
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;

import me.kuehle.carreport.Application;
import me.kuehle.carreport.R;
import me.kuehle.carreport.gui.AuthenticatorAddAccountActivity;
import me.kuehle.carreport.util.FileCopyUtil;
import me.kuehle.carreport.util.sync.AbstractSyncProvider;
import me.kuehle.carreport.util.sync.Authenticator;

public class GoogleDriveSyncProvider extends AbstractSyncProvider implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "GoogleDriveSyncProvider";

    private static final int REQUEST_PICK_ACCOUNT = 1000;
    private static final int REQUEST_RESOLVE_CONNECTION = 1001;

    // Google Drive Android API (GDAA)
    private GoogleApiClient mGoogleApiClient;
    // Google APIs Java Client (REST)
    private com.google.api.services.drive.Drive mGoogleApiServiceDrive;

    private boolean mIsAuthenticationInProgress = false;
    private AuthenticatorAddAccountActivity mAuthenticatorAddAccountActivity;
    private String mCurrentAuthenticationAccountName;

    @Override
    public long getId() {
        return 2;
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_c_sync_drive_64dp;
    }

    @Override
    public String getName() {
        return "Google Drive";
    }

    @Override
    public void setup(@Nullable Account account, @Nullable String password, @Nullable String authToken, @Nullable JSONObject settings) {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(Application.getContext())
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_APPFOLDER)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this);
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(Application.getContext(),
                Collections.singletonList(DriveScopes.DRIVE_APPDATA));

        if (account != null) {
            builder.setAccountName(account.name);
            credential.setSelectedAccountName(account.name);
        }

        mGoogleApiClient = builder.build();
        mGoogleApiServiceDrive = new com.google.api.services.drive.Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                new GsonFactory(),
                credential)
                .build();
    }

    @Override
    public void startAuthentication(AuthenticatorAddAccountActivity activity) {
        Intent pickerIntent = AccountPicker.newChooseAccountIntent(null, null,
                new String[]{"com.google"}, false, null, null, null, null);
        activity.startActivityForResult(pickerIntent, REQUEST_PICK_ACCOUNT);
    }

    @Override
    public void continueAuthentication(AuthenticatorAddAccountActivity activity, int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case REQUEST_PICK_ACCOUNT:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    setup(new Account(accountName, Authenticator.ACCOUNT_TYPE), null, null, null);

                    mIsAuthenticationInProgress = true;
                    mCurrentAuthenticationAccountName = accountName;
                    mAuthenticatorAddAccountActivity = activity;

                    mGoogleApiClient.connect();
                } else {
                    activity.onAuthenticationResult(null, null, null, null);
                }

                break;
            case REQUEST_RESOLVE_CONNECTION:
                if (resultCode == Activity.RESULT_OK) {
                    mIsAuthenticationInProgress = true;
                    mAuthenticatorAddAccountActivity = activity;

                    mGoogleApiClient.connect();
                } else {
                    activity.onAuthenticationResult(null, null, null, null);
                }

                break;
        }
    }

    @Override
    public String getRemoteFileRev() throws Exception {
        ConnectionResult connectionResult = mGoogleApiClient.blockingConnect();
        if (!connectionResult.isSuccess()) {
            throw new Exception();
        }

        try {
            com.google.api.services.drive.model.File remoteFile = getRemoteFile();
            if (remoteFile != null) {
                return String.valueOf(remoteFile.getModifiedDate().getValue());
            } else {
                return null;
            }
        } finally {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public String uploadFile() throws Exception {
        ConnectionResult connectionResult = mGoogleApiClient.blockingConnect();
        if (!connectionResult.isSuccess()) {
            throw new Exception();
        }

        File localFile = getLocalFile();
        File tempFile = new File(Application.getContext().getCacheDir(), getClass().getSimpleName());
        try {
            if (!FileCopyUtil.copyFile(localFile, tempFile)) {
                throw new Exception();
            }

            com.google.api.services.drive.model.File remoteFile = getRemoteFile();

            com.google.api.services.drive.model.File body = new com.google.api.services.drive.model.File();
            body.setTitle(localFile.getName());
            body.setMimeType("application/x-sqlite");
            body.setParents(Collections.singletonList(new ParentReference().setId("appdata")));

            FileContent mediaContent = new FileContent("application/x-sqlite", localFile);

            com.google.api.services.drive.model.File newFile;
            if (remoteFile == null) {
                newFile = mGoogleApiServiceDrive.files().insert(body, mediaContent).execute();
            } else {
                newFile = mGoogleApiServiceDrive.files().update(remoteFile.getId(), body,
                        mediaContent).execute();
            }

            return String.valueOf(newFile.getModifiedDate().getValue());
        } catch (IOException e) {
            throw new NetworkErrorException(e);
        } finally {
            if (!tempFile.delete()) {
                Log.w(TAG, "Could not delete temp file after uploading.");
            }

            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void downloadFile() throws Exception {
        ConnectionResult connectionResult = mGoogleApiClient.blockingConnect();
        if (!connectionResult.isSuccess()) {
            throw new Exception();
        }

        File localFile = getLocalFile();
        File tempFile = new File(Application.getContext().getCacheDir(), getClass().getSimpleName());
        FileOutputStream outputStream = null;
        try {
            if (!FileCopyUtil.copyFile(localFile, tempFile)) {
                throw new Exception();
            }

            com.google.api.services.drive.model.File remoteFile = getRemoteFile();
            if (remoteFile == null) {
                throw new Exception();
            }

            outputStream = new FileOutputStream(tempFile);
            HttpResponse resp = mGoogleApiServiceDrive
                    .getRequestFactory()
                    .buildGetRequest(new GenericUrl(remoteFile.getDownloadUrl()))
                    .execute();
            resp.download(outputStream);
            if (!FileCopyUtil.copyFile(tempFile, localFile)) {
                throw new Exception();
            }
        } catch (IOException e) {
            throw new NetworkErrorException(e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.w(TAG, "Could not close output stream after downloading.");
                }
            }

            if (!tempFile.delete()) {
                Log.w(TAG, "Could not delete temp file after downloading.");
            }

            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (mIsAuthenticationInProgress) {
            mIsAuthenticationInProgress = false;
            mAuthenticatorAddAccountActivity.onAuthenticationResult(mCurrentAuthenticationAccountName, null, null, null);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (mIsAuthenticationInProgress) {
            mIsAuthenticationInProgress = false;
            if (connectionResult.hasResolution()) {
                try {
                    connectionResult.startResolutionForResult(mAuthenticatorAddAccountActivity,
                            REQUEST_RESOLVE_CONNECTION);
                } catch (IntentSender.SendIntentException e) {
                    mAuthenticatorAddAccountActivity.onAuthenticationResult(null, null, null, null);
                }
            } else {
                GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(),
                        mAuthenticatorAddAccountActivity, 0).show();
            }
        }
    }

    private com.google.api.services.drive.model.File getRemoteFile() throws Exception {
        File localFile = getLocalFile();
        com.google.api.services.drive.model.File remoteFile = null;
        try {
            FileList files = mGoogleApiServiceDrive.files().list()
                    .setQ(String.format("title = '%s'", localFile.getName()))
                    .setFields("items(downloadUrl,id,modifiedDate,fileSize)").execute();
            if (files.getItems().size() > 1) {
                // Due to a bug in the GDAA it is possible, that there is more than one
                // database file. In this case we use the largest one (the one with
                // the most data) and remove the others.
                for (com.google.api.services.drive.model.File file : files.getItems()) {
                    if (remoteFile == null) {
                        remoteFile = file;
                    } else if (remoteFile.getFileSize() < file.getFileSize()) {
                        mGoogleApiServiceDrive.files().trash(remoteFile.getId()).execute();
                        remoteFile = file;
                    } else {
                        mGoogleApiServiceDrive.files().trash(file.getId()).execute();
                    }
                }

                return remoteFile;
            } else if (files.getItems().size() == 1) {
                return files.getItems().get(0);
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new NetworkErrorException(e);
        }
    }
}
