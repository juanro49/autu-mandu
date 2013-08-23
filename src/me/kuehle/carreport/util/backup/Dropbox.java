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
import java.nio.channels.FileChannel;

import me.kuehle.carreport.Application;
import me.kuehle.carreport.Preferences;
import android.content.Context;
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

public class Dropbox {
	public interface OnAuthenticationFinishedListener {
		public void authenticationFinished(boolean success, String account,
				boolean remoteDataAvailable);
	}

	public interface OnSynchronizeListener {
		public void synchronizationFinished(boolean result);

		public void synchronizationStarted();
	}

	public static final int SYNC_NORMAL = 1;
	public static final int SYNC_DOWNLOAD = 2;
	public static final int SYNC_UPLOAD = 3;

	private final static String APP_KEY = "a6edub2n9b029if";
	private final static String APP_SECRET = "1cw56rcn1bbnb7f";
	private final static AccessType ACCESS_TYPE = AccessType.APP_FOLDER;

	private static final String TEMP_FILE_NAME = "dropbox";

	private static Dropbox instance;

	public static Dropbox getInstance() {
		return instance;
	}

	public static void init(Context context) {
		instance = new Dropbox(context);
	}

	private Context context;
	private DropboxAPI<AndroidAuthSession> mDBApi;

	private boolean synchronisationInProgress = false;
	private OnSynchronizeListener synchronisationCallback;

	private File tempFile;

	private Dropbox(Context context) {
		this.context = context;

		AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
		AndroidAuthSession session = new AndroidAuthSession(appKeys,
				ACCESS_TYPE);
		AccessTokenPair tokens = loadAccessTokens();
		if (tokens != null) {
			session.setAccessTokenPair(tokens);
		}

		this.mDBApi = new DropboxAPI<AndroidAuthSession>(session);

		this.tempFile = new File(context.getCacheDir(), TEMP_FILE_NAME);
	}

	public void finishAuthentication(
			final OnAuthenticationFinishedListener callback) {
		if (mDBApi.getSession().authenticationSuccessful()) {
			try {
				mDBApi.getSession().finishAuthentication();
				AccessTokenPair tokens = mDBApi.getSession()
						.getAccessTokenPair();
				saveAccessTokens(tokens);

				new AsyncTask<Void, Void, Boolean>() {
					private String accountName;
					private boolean remoteDataAvailable;

					@Override
					protected Boolean doInBackground(Void... params) {
						accountName = loadAccountName();
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
							if (e.error != 404) {
								remoteDataAvailable = false;
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
						callback.authenticationFinished(result, accountName,
								remoteDataAvailable);
					}
				}.execute();
			} catch (IllegalStateException e) {
				callback.authenticationFinished(false, null, false);
			}
		}
	}

	public String getAccountName() {
		Preferences prefs = new Preferences(context);
		return prefs.getDropboxAccount();
	}

	public boolean isLinked() {
		return mDBApi.getSession().isLinked();
	}

	public boolean isSynchronisationInProgress() {
		return synchronisationInProgress;
	}

	public void setSynchronisationCallback(OnSynchronizeListener callback) {
		synchronisationCallback = callback;
		if (synchronisationCallback != null && synchronisationInProgress) {
			synchronisationCallback.synchronizationStarted();
		}
	}

	public void startAuthentication(Context context) {
		mDBApi.getSession().startAuthentication(context);
	}

	public void synchronize() {
		synchronize(SYNC_NORMAL);
	}

	public void synchronize(int option) {
		if (synchronisationInProgress || !isLinked()) {
			return;
		}

		synchronisationInProgress = true;
		new AsyncTask<Integer, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(Integer... params) {
				Preferences prefs = new Preferences(context);
				File localFile = new File(Cache.openDatabase().getPath());
				String localRev = prefs.getDropboxLocalRev();

				String remoteRev = null;
				try {
					Entry remoteEntry = mDBApi.metadata(
							"/" + localFile.getName(), 1, null, false, null);
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

				if (params[0] == SYNC_UPLOAD
						|| remoteRev == null
						|| (params[0] != SYNC_DOWNLOAD && remoteRev
								.equals(localRev))) {
					// Upload
					if (!copyFile(localFile, tempFile)) {
						return false;
					}

					FileInputStream inputStream = null;
					try {
						inputStream = new FileInputStream(tempFile);
						Entry remoteEntry = mDBApi.putFile(
								"/" + localFile.getName(), inputStream,
								tempFile.length(), remoteRev, null);
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

						tempFile.delete();
					}
				} else {
					// Download
					FileOutputStream outputStream = null;
					try {
						outputStream = new FileOutputStream(tempFile);
						DropboxFileInfo info = mDBApi.getFile(
								"/" + localFile.getName(), null, outputStream,
								null);
						prefs.setDropboxLocalRev(info.getMetadata().rev);
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

						tempFile.delete();
					}

					if (copyFile(tempFile, localFile)) {
						Application.reinitializeDatabase();
					}

					tempFile.delete();
				}

				return true;
			}

			@Override
			protected void onPostExecute(Boolean result) {
				synchronisationInProgress = false;
				if (synchronisationCallback != null) {
					synchronisationCallback.synchronizationFinished(result);
				}
			}

			@Override
			protected void onPreExecute() {
				if (synchronisationCallback != null) {
					synchronisationCallback.synchronizationStarted();
				}
			}
		}.execute(option);
	}

	public void unlink() {
		mDBApi.getSession().unlink();
		Preferences prefs = new Preferences(context);
		prefs.setDropboxAccount(null);
		prefs.setDropboxKey(null);
		prefs.setDropboxSecret(null);
	}

	private boolean copyFile(File from, File to) {
		try {
			FileInputStream inStream = new FileInputStream(from);
			FileOutputStream outStream = new FileOutputStream(to);
			FileChannel src = inStream.getChannel();
			FileChannel dst = outStream.getChannel();
			dst.transferFrom(src, 0, src.size());
			src.close();
			dst.close();
			inStream.close();
			outStream.close();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private AccessTokenPair loadAccessTokens() {
		Preferences prefs = new Preferences(context);
		String key = prefs.getDropboxKey();
		String secret = prefs.getDropboxSecret();
		if (key != null && secret != null) {
			return new AccessTokenPair(key, secret);
		} else {
			return null;
		}
	}

	private String loadAccountName() {
		String accountName = null;
		try {
			accountName = mDBApi.accountInfo().displayName;
		} catch (DropboxUnlinkedException e) {
			unlink();
		} catch (DropboxException e) {
			accountName = "- could not load account name -";
		}

		Preferences prefs = new Preferences(context);
		prefs.setDropboxAccount(accountName);
		return accountName;
	}

	private void saveAccessTokens(AccessTokenPair tokens) {
		Preferences prefs = new Preferences(context);
		prefs.setDropboxKey(tokens.key);
		prefs.setDropboxSecret(tokens.secret);
	}
}
