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

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;

import me.kuehle.carreport.Application;
import me.kuehle.carreport.BuildConfig;
import me.kuehle.carreport.R;
import me.kuehle.carreport.gui.AuthenticatorAddAccountActivity;
import me.kuehle.carreport.util.FileCopyUtil;
import me.kuehle.carreport.util.sync.AbstractSyncProvider;
import me.kuehle.carreport.util.sync.Authenticator;
import me.kuehle.carreport.util.sync.SyncAuthException;
import me.kuehle.carreport.util.sync.SyncIoException;
import me.kuehle.carreport.util.sync.SyncParseException;

public class GoogleDriveSyncProvider extends AbstractSyncProvider implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "GoogleDriveSyncProvider";

    private static final int REQUEST_PICK_ACCOUNT = 1000;
    private static final int REQUEST_RESOLVE_CONNECTION = 1001;
    private static final int REQUEST_PERMISSIONS = 1002;

    // Google Drive Android API (GDAA)
    private GoogleApiClient mGoogleApiClient;
    // Google APIs Java Client (REST)
    private GoogleAccountCredential mGoogleAccountCredential;
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
        mGoogleAccountCredential = GoogleAccountCredential.usingOAuth2(Application.getContext(),
                Collections.singletonList(DriveScopes.DRIVE_APPDATA));

        if (account != null) {
            builder.setAccountName(account.name);
            mGoogleAccountCredential.setSelectedAccountName(account.name);
        }

        mGoogleApiClient = builder.build();
        mGoogleApiServiceDrive = new com.google.api.services.drive.Drive.Builder(
                new NetHttpTransport(),
                new AndroidJsonFactory(),
                mGoogleAccountCredential)
                .setApplicationName(Application.getContext().getString(R.string.app_name) + "/" + BuildConfig.VERSION_NAME)
                .build();
    }

    @Override
    public void startAuthentication(AuthenticatorAddAccountActivity activity) {
        String permission = Manifest.permission.GET_ACCOUNTS;
        String[] permissions = new String[]{permission};
        if (ContextCompat.checkSelfPermission(activity, permission) ==
                PackageManager.PERMISSION_GRANTED) {
            continueAuthentication(activity, REQUEST_PERMISSIONS, Activity.RESULT_OK, null);
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                Toast.makeText(activity, R.string.toast_need_accounts_permission,
                        Toast.LENGTH_LONG).show();
            }

            ActivityCompat.requestPermissions(activity, permissions, REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void continueAuthentication(AuthenticatorAddAccountActivity activity, int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS:
                if (resultCode == Activity.RESULT_OK) {
                    Intent pickerIntent = mGoogleAccountCredential.newChooseAccountIntent();
                    activity.startActivityForResult(pickerIntent, REQUEST_PICK_ACCOUNT);
                } else {
                    activity.onAuthenticationResult(null, null, null, null);
                }

                break;
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
    public String getRemoteFileRev() throws SyncAuthException, SyncIoException, SyncParseException {
        ensureSuccessfulConnect();

        try {
            com.google.api.services.drive.model.File remoteFile = getRemoteFile();
            if (remoteFile != null) {
                return String.valueOf(remoteFile.getModifiedTime().getValue());
            } else {
                return null;
            }
        } finally {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public String uploadFile() throws SyncAuthException, SyncIoException, SyncParseException {
        ensureSuccessfulConnect();

        File localFile = getLocalFile();
        File tempFile = new File(Application.getContext().getCacheDir(), getClass().getSimpleName());
        try {
            if (!FileCopyUtil.copyFile(localFile, tempFile)) {
                throw new SyncParseException("Copying database to temp file failed.");
            }

            com.google.api.services.drive.model.File remoteFile = getRemoteFile();
            com.google.api.services.drive.model.File body = new com.google.api.services.drive.model.File();
            FileContent mediaContent = new FileContent("application/x-sqlite", tempFile);
            com.google.api.services.drive.model.File newFile;
            if (remoteFile == null) {
                body.setName(localFile.getName());
                body.setMimeType("application/x-sqlite");
                body.setParents(Collections.singletonList("appDataFolder"));

                newFile = mGoogleApiServiceDrive.files()
                        .create(body, mediaContent)
                        .setFields("modifiedTime")
                        .execute();
            } else {
                newFile = mGoogleApiServiceDrive.files()
                        .update(remoteFile.getId(), body, mediaContent)
                        .setFields("modifiedTime")
                        .execute();
            }

            return String.valueOf(newFile.getModifiedTime().getValue());
        } catch (IOException e) {
            throw new SyncIoException(e);
        } finally {
            if (!tempFile.delete()) {
                Log.w(TAG, "Could not delete temp file after uploading.");
            }

            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void downloadFile() throws SyncAuthException, SyncIoException, SyncParseException {
        ensureSuccessfulConnect();

        File localFile = getLocalFile();
        File tempFile = new File(Application.getContext().getCacheDir(), getClass().getSimpleName());
        try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            com.google.api.services.drive.model.File remoteFile = getRemoteFile();
            if (remoteFile == null) {
                throw new SyncParseException();
            }

            mGoogleApiServiceDrive
                    .files()
                    .get(remoteFile.getId())
                    .executeMediaAndDownloadTo(outputStream);
            if (!FileCopyUtil.copyFile(tempFile, localFile)) {
                throw new SyncParseException();
            }
        } catch (IOException e) {
            throw new SyncIoException(e);
        } finally {
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
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (mIsAuthenticationInProgress) {
            mIsAuthenticationInProgress = false;
            if (connectionResult.hasResolution()) {
                try {
                    connectionResult.startResolutionForResult(
                            mAuthenticatorAddAccountActivity,
                            REQUEST_RESOLVE_CONNECTION);
                } catch (IntentSender.SendIntentException e) {
                    mAuthenticatorAddAccountActivity.onAuthenticationResult(null, null, null, null);
                }
            } else {
                GoogleApiAvailability.getInstance().showErrorDialogFragment(
                        mAuthenticatorAddAccountActivity,
                        connectionResult.getErrorCode(),
                        REQUEST_RESOLVE_CONNECTION,
                        dialogInterface -> mAuthenticatorAddAccountActivity.onAuthenticationResult(null, null, null, null));
            }
        }
    }

    private com.google.api.services.drive.model.File getRemoteFile() throws SyncIoException, SyncParseException {
        File localFile = getLocalFile();
        com.google.api.services.drive.model.File remoteFile = null;
        try {
            FileList files = mGoogleApiServiceDrive.files().list()
                    .setQ(String.format("name = '%s'", localFile.getName()))
                    .setSpaces("appDataFolder")
                    .setFields("files(id,modifiedTime,size)").execute();
            if (files.getFiles().size() > 1) {
                // Due to a bug in the GDAA it is possible, that there is more than one
                // database file. In this case we use the largest one (the one with
                // the most data) and remove the others.
                for (com.google.api.services.drive.model.File file : files.getFiles()) {
                    if (remoteFile == null) {
                        remoteFile = file;
                    } else if (remoteFile.getSize() < file.getSize()) {
                        mGoogleApiServiceDrive.files().delete(remoteFile.getId()).execute();
                        remoteFile = file;
                    } else {
                        mGoogleApiServiceDrive.files().delete(file.getId()).execute();
                    }
                }

                return remoteFile;
            } else if (files.getFiles().size() == 1) {
                return files.getFiles().get(0);
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new SyncIoException(e);
        } catch (Exception e) {
            // There are reported app crashes with an IllegalArgumentException. It causes the app
            // to crash while it's not even visibly running. I can't find any documentation about
            // this exception though. To avoid crashes for users, this catches all exceptions and
            // reports them as good as possible.
            throw new SyncParseException(e);
        }
    }

    private void ensureSuccessfulConnect() throws SyncAuthException, SyncIoException, SyncParseException {
        ConnectionResult connectionResult = mGoogleApiClient.blockingConnect();
        if (!connectionResult.isSuccess()) {
            switch (connectionResult.getErrorCode()) {
                case ConnectionResult.SIGN_IN_REQUIRED:
                case ConnectionResult.INVALID_ACCOUNT:
                case ConnectionResult.RESOLUTION_REQUIRED:
                case ConnectionResult.SIGN_IN_FAILED:
                case ConnectionResult.SERVICE_MISSING_PERMISSION:
                    throw new SyncAuthException();
                case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                case ConnectionResult.NETWORK_ERROR:
                case ConnectionResult.INTERNAL_ERROR:
                case ConnectionResult.CANCELED:
                case ConnectionResult.TIMEOUT:
                case ConnectionResult.INTERRUPTED:
                case ConnectionResult.API_UNAVAILABLE:
                case ConnectionResult.SERVICE_UPDATING:
                    throw new SyncIoException(connectionResult.getErrorMessage());
                default:
                    throw new SyncParseException(connectionResult.getErrorMessage());
            }
        }
    }
}
