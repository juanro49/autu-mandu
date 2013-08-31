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

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import me.kuehle.carreport.Application;
import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.gui.dialog.ProgressDialogFragment;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.activeandroid.Cache;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;

public class GoogleDrive extends AbstractSynchronizationProvider {
	private class AuthenticateTask extends AsyncTask<Void, Void, Void> {
		private UserRecoverableAuthIOException userRecovEx;
		private Exception ex;
		private boolean remoteDataAvailable = false;

		@Override
		protected void onPreExecute() {
			ProgressDialogFragment
					.newInstance(
							mContext.getString(R.string.alert_sync_finishing_authentication))
					.show(mAuthenticationFragmentManager, "progress");
		}

		@Override
		protected Void doInBackground(Void... params) {
			Preferences prefs = new Preferences(mContext);
			Drive service = getService();

			try {
				// Load appdata folder id.
				File appdata = service.files().get("appdata").execute();
				prefs.setGoogleDriveAppDataID(appdata.getId());

				// Check if remote data is available.
				java.io.File localFile = new java.io.File(Cache.openDatabase()
						.getPath());
				FileList files = service
						.files()
						.list()
						.setQ(String.format("'%s' in parents and title = '%s'",
								appdata.getId(), localFile.getName()))
						.execute();
				if (files.getItems().size() > 0) {
					remoteDataAvailable = true;
				}
			} catch (UserRecoverableAuthIOException e) {
				userRecovEx = e;
			} catch (Exception e) {
				unlink();
				ex = e;
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			mAuthenticationFragmentManager.executePendingTransactions();
			((ProgressDialogFragment) mAuthenticationFragmentManager
					.findFragmentByTag("progress")).dismiss();

			if (userRecovEx != null) {
				mAuthenticationFragment.startActivityForResult(
						userRecovEx.getIntent(),
						REQUEST_RECOVER_FROM_AUTH_ERROR);
			} else if (ex != null) {
				authenticationFinished(false, false);
			} else {
				authenticationFinished(true, remoteDataAvailable);
			}
		}
	}

	private static final int REQUEST_CHOOSE_ACCOUNT = 1000;
	private static final int REQUEST_RECOVER_FROM_AUTH_ERROR = 1001;
	private static final String SCOPE = DriveScopes.DRIVE_APPDATA;

	private GoogleAccountCredential credential;

	public GoogleDrive(Context context) {
		super(context);
		credential = GoogleAccountCredential.usingOAuth2(mContext,
				Arrays.asList(SCOPE));
		Preferences prefs = new Preferences(mContext);
		credential.setSelectedAccountName(prefs.getGoogleDriveAccount());
	}

	@Override
	public String getAccountName() {
		return credential.getSelectedAccountName();
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
		return credential.getSelectedAccountName() != null;
	}

	private Drive getService() {
		return new Drive.Builder(AndroidHttp.newCompatibleTransport(),
				new GsonFactory(), credential).setApplicationName(
				mContext.getString(R.string.app_name)).build();
	}

	@Override
	protected void onContinueAuthentication(int requestCode, int resultCode,
			Intent data) {
		switch (requestCode) {
		case REQUEST_CHOOSE_ACCOUNT:
			if (resultCode == Activity.RESULT_OK) {
				String accountName = data
						.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
				if (accountName != null) {
					Preferences prefs = new Preferences(mContext);
					prefs.setGoogleDriveAccount(accountName);

					credential.setSelectedAccountName(accountName);
					new AuthenticateTask().execute();
				} else {
					authenticationFinished(false, false);
				}
			} else {
				authenticationFinished(false, false);
			}

			break;
		case REQUEST_RECOVER_FROM_AUTH_ERROR:
			if (resultCode == Activity.RESULT_OK) {
				new AuthenticateTask().execute();
			} else {
				authenticationFinished(false, false);
			}
			break;
		}
	}

	@Override
	protected void onStartAuthentication() {
		mAuthenticationFragment.startActivityForResult(
				credential.newChooseAccountIntent(), REQUEST_CHOOSE_ACCOUNT);
	}

	@Override
	protected boolean onSynchronize(int option) {
		Preferences prefs = new Preferences(mContext);
		Drive service = getService();

		java.io.File localFile = new java.io.File(Cache.openDatabase()
				.getPath());
		String appdata = prefs.getGoogleDriveAppDataID();
		DateTime localModified = prefs.getGoogleDriveLocalModifiedDate();

		// Get id and modification date of the remote file.
		File remoteFile = null;
		try {
			FileList files = service
					.files()
					.list()
					.setQ(String.format("'%s' in parents and title = '%s'",
							appdata, localFile.getName())).execute();
			if (files.getItems().size() > 0) {
				remoteFile = files.getItems().get(0);
			}
		} catch (IOException e) {
			return false;
		}

		if (option == SYNC_UPLOAD
				|| remoteFile == null
				|| (option != SYNC_DOWNLOAD && remoteFile.getModifiedDate()
						.equals(localModified))) {
			// Upload
			if (!copyFile(localFile, mTempFile)) {
				return false;
			}

			File body = new File();
			body.setTitle(localFile.getName());
			body.setMimeType("application/x-sqlite");
			body.setParents(Arrays.asList(new ParentReference().setId(appdata)));
			FileContent mediaContent = new FileContent("application/x-sqlite",
					localFile);
			try {
				File file;
				if (remoteFile == null) {
					file = service.files().insert(body, mediaContent).execute();
				} else {
					file = service.files()
							.update(remoteFile.getId(), body, mediaContent)
							.execute();
				}

				prefs.setGoogleDriveLocalModifiedDate(file.getModifiedDate());
			} catch (IOException e) {
				return false;
			} finally {
				mTempFile.delete();
			}
		} else {
			// Download
			FileOutputStream outputStream = null;
			try {
				outputStream = new FileOutputStream(mTempFile);
				HttpResponse resp = service
						.getRequestFactory()
						.buildGetRequest(
								new GenericUrl(remoteFile.getDownloadUrl()))
						.execute();
				resp.download(outputStream);
				if (copyFile(mTempFile, localFile)) {
					prefs.setGoogleDriveLocalModifiedDate(remoteFile
							.getModifiedDate());
					Application.reinitializeDatabase();
				}
			} catch (IOException e) {
				return false;
			} finally {
				if (outputStream != null) {
					try {
						outputStream.close();
					} catch (IOException e) {
					}
				}

				mTempFile.delete();
			}
		}

		return true;
	}

	@Override
	protected void onUnlink() {
		Preferences prefs = new Preferences(mContext);
		prefs.setGoogleDriveAccount(null);
		prefs.setGoogleDriveAppDataID(null);
		prefs.setGoogleDriveLocalModifiedDate(null);
	}
}
