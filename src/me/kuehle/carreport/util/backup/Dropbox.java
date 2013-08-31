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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import me.kuehle.carreport.Application;
import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.gui.dialog.ProgressDialogFragment;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.activeandroid.Cache;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DropboxFileInfo;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;

public class Dropbox extends AbstractSynchronizationProvider {
	private static final String APP_KEY = "a6edub2n9b029if";
	private static final String APP_SECRET = "1cw56rcn1bbnb7f";
	private static final AccessType ACCESS_TYPE = AccessType.APP_FOLDER;

	private DropboxAPI<AndroidAuthSession> mDBApi;
	private boolean authenticationInProgress = false;

	public Dropbox(Context context) {
		super(context);

		AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
		AndroidAuthSession session = new AndroidAuthSession(appKeys,
				ACCESS_TYPE);
		AccessTokenPair tokens = loadAccessTokens();
		if (tokens != null) {
			session.setAccessTokenPair(tokens);
		}

		mDBApi = new DropboxAPI<AndroidAuthSession>(session);
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

	private AccessTokenPair loadAccessTokens() {
		Preferences prefs = new Preferences(mContext);
		String key = prefs.getDropboxKey();
		String secret = prefs.getDropboxSecret();
		if (key != null && secret != null) {
			return new AccessTokenPair(key, secret);
		} else {
			return null;
		}
	}

	private void saveAccessTokens(AccessTokenPair tokens) {
		Preferences prefs = new Preferences(mContext);
		prefs.setDropboxKey(tokens.key);
		prefs.setDropboxSecret(tokens.secret);
	}

	@Override
	protected void onContinueAuthentication(int requestCode, int resultCode,
			Intent data) {
		if (!authenticationInProgress) {
			return;
		}

		if (mDBApi.getSession().authenticationSuccessful()) {
			ProgressDialogFragment
					.newInstance(
							mContext.getString(R.string.alert_dropbox_finishing_authentication))
					.show(mAuthenticationFragmentManager, "progress");

			try {
				mDBApi.getSession().finishAuthentication();
				AccessTokenPair tokens = mDBApi.getSession()
						.getAccessTokenPair();
				saveAccessTokens(tokens);

				new AsyncTask<Void, Void, Boolean>() {
					private String accountName;
					private boolean remoteDataAvailable = false;

					@Override
					protected Boolean doInBackground(Void... params) {
						// Load account name.
						try {
							accountName = mDBApi.accountInfo().displayName;
						} catch (DropboxUnlinkedException e) {
							unlink();
							return false;
						} catch (DropboxException e) {
							accountName = "- could not load account name -";
						}

						Preferences prefs = new Preferences(mContext);
						prefs.setDropboxAccount(accountName);
						if (accountName == null) {
							return false;
						}

						// Check if remote data is available.
						File localFile = new File(Cache.openDatabase()
								.getPath());
						try {
							Entry remoteEntry = mDBApi.metadata(
									"/" + localFile.getName(), 1, null, false,
									null);
							if (!remoteEntry.isDeleted) {
								remoteDataAvailable = true;
							}
						} catch (DropboxServerException e) {
							if (e.error == DropboxServerException._401_UNAUTHORIZED
									|| e.error == DropboxServerException._403_FORBIDDEN) {
								return false;
							}
						} catch (DropboxUnlinkedException e) {
							unlink();
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
	protected void onStartAuthentication() {
		authenticationInProgress = true;
		mDBApi.getSession().startAuthentication(mContext);
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
			unlink();
			return false;
		} catch (DropboxException e) {
			return false;
		}

		if (option == SYNC_UPLOAD || remoteRev == null
				|| (option != SYNC_DOWNLOAD && remoteRev.equals(localRev))) {
			// Upload
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
					}
				}

				mTempFile.delete();
			}
		} else {
			// Download
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
					}
				}

				mTempFile.delete();
			}
		}

		return true;
	}

	@Override
	protected void onUnlink() {
		mDBApi.getSession().unlink();
		Preferences prefs = new Preferences(mContext);
		prefs.setDropboxAccount(null);
		prefs.setDropboxKey(null);
		prefs.setDropboxLocalRev(null);
		prefs.setDropboxSecret(null);
	}
}
