/*
 * Copyright 2013 Jan Kühle
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

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;

import com.activeandroid.Cache;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.plus.Plus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import me.kuehle.carreport.Application;
import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.gui.dialog.ProgressDialogFragment;

public class GoogleDriveSynchronizationProvider extends AbstractSynchronizationProvider implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final int REQUEST_PICK_ACCOUNT = 1000;
    private static final int REQUEST_RESOLVE_CONNECTION = 1001;
    private static final Scope SCOPE = Drive.SCOPE_APPFOLDER;

    private GoogleApiClient mGoogleApiClient;

    public GoogleDriveSynchronizationProvider(Context context) {
        super(context);

        createApiClient();
    }

    @Override
    public String getAccountName() {
        Preferences prefs = new Preferences(mContext);
        return prefs.getGoogleDriveAccount();
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_sync_drive;
    }

    @Override
    public String getName() {
        return "Google Drive";
    }

    @Override
    public boolean isAuthenticated() {
        return getAccountName() != null;
    }

    @Override
    protected void onStartAuthentication() {
        Intent pickerIntent = AccountPicker.newChooseAccountIntent(null, null,
                new String[] { "com.google" }, false, null, null, null, null);
        mAuthenticationFragment.startActivityForResult(pickerIntent, REQUEST_PICK_ACCOUNT);
    }

    @Override
    protected void onContinueAuthentication(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_PICK_ACCOUNT:
                if(resultCode == Activity.RESULT_OK) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    Preferences prefs = new Preferences(mContext);
                    prefs.setGoogleDriveAccount(accountName);

                    createApiClient();
                    mGoogleApiClient.connect();
                } else {
                    authenticationFinished(false, false);
                }

                break;
            case REQUEST_RESOLVE_CONNECTION:
                if (resultCode == Activity.RESULT_OK) {
                    mGoogleApiClient.connect();
                } else {
                    authenticationFinished(false, false);
                }

                break;
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (mAuthenticationInProgess) {
            ProgressDialogFragment
                    .newInstance(mContext.getString(R.string.alert_sync_finishing_authentication))
                    .show(mAuthenticationFragmentManager, "progress");

            try {
                new AsyncTask<Void, Void, Boolean>() {
                    private boolean remoteDataAvailable = false;

                    @Override
                    protected Boolean doInBackground(Void... params) {
                        // Check if remote data is available.
                        File localFile = new File(Cache.openDatabase().getPath());
                        Query query = new Query.Builder()
                                .addFilter(Filters.eq(SearchableField.TITLE, localFile.getName()))
                                .build();

                        DriveApi.MetadataBufferResult metadataBufferResult =  Drive.DriveApi
                                .getAppFolder(mGoogleApiClient)
                                .queryChildren(mGoogleApiClient, query)
                                .await();
                        if(!metadataBufferResult.getStatus().isSuccess()) {
                            return false;
                        }

                        remoteDataAvailable = metadataBufferResult.getMetadataBuffer().getCount() > 0;

                        return true;
                    }

                    @Override
                    protected void onPostExecute(Boolean result) {
                        mGoogleApiClient.disconnect();

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
        } else if (mUnlinkingInProgress) {
            Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            unlinkingFinished();
                        }
                    });
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if(mAuthenticationInProgess) {
            if (connectionResult.hasResolution()) {
                try {
                    connectionResult.startResolutionForResult(mAuthenticationFragment.getActivity(),
                            REQUEST_RESOLVE_CONNECTION);
                } catch (IntentSender.SendIntentException e) {
                    authenticationFinished(false, false);
                }
            } else {
                GooglePlayServicesUtil
                        .getErrorDialog(connectionResult.getErrorCode(),
                                mAuthenticationFragment.getActivity(), 0)
                        .show();
            }
        } else if(mUnlinkingInProgress) {
            // Nothing to do here.
            unlinkingFinished();
        }
    }

    @Override
    protected boolean onSynchronize(int option) {
        ConnectionResult connectionResult = mGoogleApiClient.blockingConnect();
        if (!connectionResult.isSuccess()) {
            return false;
        }

        boolean result = doSynchronization(option);

        mGoogleApiClient.disconnect();

        return result;
    }

    /**
     * Actually performs the synchronization, assuming a connected GoogleApiClient.
     * @param option
     * @return
     */
    private boolean doSynchronization(int option) {
        Preferences prefs = new Preferences(mContext);
        File localFile = new File(Cache.openDatabase().getPath());
        Date localModifiedDate = prefs.getGoogleDriveLocalModifiedDate();
        DriveFolder remoteFolder = Drive.DriveApi.getAppFolder(mGoogleApiClient);

        // Get id and modification date of the remote file.
        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, localFile.getName()))
                .build();
        DriveApi.MetadataBufferResult remoteFileResult = remoteFolder
                .queryChildren(mGoogleApiClient, query)
                .await();
        if (!remoteFileResult.getStatus().isSuccess()) {
            return false;
        }

        DriveFile remoteFile = null;
        Date remoteFileModifiedDate = null;
        if (remoteFileResult.getMetadataBuffer().getCount() > 0) {
            Metadata metadata = remoteFileResult.getMetadataBuffer().get(0);
            remoteFile = Drive.DriveApi.getFile(mGoogleApiClient, metadata.getDriveId());
            remoteFileModifiedDate = metadata.getModifiedDate();
        }

        if (option == SYNC_UPLOAD
                || remoteFile == null
                || (option == SYNC_NORMAL && remoteFileModifiedDate.equals(localModifiedDate))) {
            // --------------------------------------------------
            // Upload
            // --------------------------------------------------

            if (!copyFile(localFile, mTempFile)) {
                return false;
            }

            try {
                if (remoteFile == null) {
                    DriveApi.DriveContentsResult newContentResult = Drive.DriveApi
                            .newDriveContents(mGoogleApiClient)
                            .await();
                    if (!newContentResult.getStatus().isSuccess()) {
                        return false;
                    }

                    DriveContents newContents = newContentResult.getDriveContents();

                    FileInputStream inStream = new FileInputStream(localFile);
                    copyFile(inStream, newContents.getOutputStream());
                    inStream.close();

                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle(localFile.getName())
                            .setMimeType("application/x-sqlite")
                            .build();
                    DriveFolder.DriveFileResult newFileResult = remoteFolder
                            .createFile(mGoogleApiClient, changeSet, newContents)
                            .await();
                    if (!newFileResult.getStatus().isSuccess()) {
                        return false;
                    }

                    remoteFile = newFileResult.getDriveFile();
                } else {
                    DriveApi.DriveContentsResult contentResult = remoteFile
                            .open(mGoogleApiClient, DriveFile.MODE_WRITE_ONLY, null)
                            .await();
                    if (!contentResult.getStatus().isSuccess()) {
                        return false;
                    }

                    DriveContents contents = contentResult.getDriveContents();

                    FileInputStream inStream = new FileInputStream(localFile);
                    copyFile(inStream, contents.getOutputStream());
                    inStream.close();

                    Status status = contents.commit(mGoogleApiClient, null).await();
                    if (!status.isSuccess()) {
                        return false;
                    }
                }

                DriveResource.MetadataResult metadataResult = remoteFile
                        .getMetadata(mGoogleApiClient)
                        .await();
                if (metadataResult.getStatus().isSuccess()) {
                    remoteFileModifiedDate = metadataResult.getMetadata().getModifiedDate();
                    prefs.setGoogleDriveLocalModifiedDate(remoteFileModifiedDate);
                }
            } catch (IOException e) {
                return false;
            } finally {
                mTempFile.delete();
            }
        } else {
            // --------------------------------------------------
            // Download
            // --------------------------------------------------

            DriveApi.DriveContentsResult contentResult = remoteFile
                    .open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null)
                    .await();
            if (!contentResult.getStatus().isSuccess()) {
                return false;
            }

            DriveContents contents = contentResult.getDriveContents();

            try {
                FileOutputStream outStream = new FileOutputStream(localFile);
                boolean copySuccessful = copyFile(contents.getInputStream(), outStream);
                outStream.close();

                if (copySuccessful) {
                    prefs.setGoogleDriveLocalModifiedDate(remoteFileModifiedDate);
                    Application.reinitializeDatabase();
                }
            } catch (IOException e) {
                return false;
            }
        }

        return true;
    }

    @Override
    protected void onUnlink() {
        Preferences prefs = new Preferences(mContext);
        prefs.setGoogleDriveAccount(null);
        prefs.setGoogleDriveLocalModifiedDate(null);

        mGoogleApiClient.connect();
    }

    private void createApiClient() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        Preferences prefs = new Preferences(mContext);
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(Drive.API)
                .addApi(Plus.API)
                .addScope(SCOPE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .setAccountName(prefs.getGoogleDriveAccount())
                .build();
    }
}
