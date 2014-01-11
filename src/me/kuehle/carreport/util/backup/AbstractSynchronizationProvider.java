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
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

import me.kuehle.carreport.Preferences;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

public abstract class AbstractSynchronizationProvider {
	public interface OnAuthenticationListener {
		public void onAuthenticationFinished(boolean success,
				boolean remoteDataAvailable);
	}

	public interface OnSynchronizeListener {
		public void onSynchronizationFinished(boolean result);

		public void onSynchronizationStarted();
	}

	private static class SynchronizationStatusReceiver extends
			BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			int status = intent.getIntExtra(
					SynchronizationService.EXTRA_STATUS, -1);
			boolean result = intent.getBooleanExtra(
					SynchronizationService.EXTRA_RESULT, false);

			if (status == SynchronizationService.STATUS_STARTED) {
				if (mSynchronisationListener != null) {
					mSynchronisationListener.onSynchronizationStarted();
				}
			} else if (status == SynchronizationService.STATUS_FINISHED) {
				mSynchronisationInProgress = false;
				if (mSynchronisationListener != null) {
					mSynchronisationListener.onSynchronizationFinished(result);
				}
			}
		}
	}

	public static final int SYNC_NORMAL = 1;
	public static final int SYNC_DOWNLOAD = 2;
	public static final int SYNC_UPLOAD = 3;

	private static AbstractSynchronizationProvider current;
	private static AbstractSynchronizationProvider[] avaialble;

	private static boolean mSynchronisationInProgress = false;
	private static OnSynchronizeListener mSynchronisationListener;

	public static synchronized AbstractSynchronizationProvider[] getAvailable(
			Context context) {
		if (avaialble != null) {
			return avaialble;
		}

		avaialble = new AbstractSynchronizationProvider[] {
				new Dropbox(context), new GoogleDrive(context) };

		return avaialble;
	}

	public static synchronized AbstractSynchronizationProvider getCurrent(
			Context context) {
		if (current != null) {
			return current;
		}

		Preferences prefs = new Preferences(context);
		String providerName = prefs.getSynchronizationProvider();
		if (providerName == null) {
			return null;
		}

		AbstractSynchronizationProvider[] avaialble = getAvailable(context);
		for (AbstractSynchronizationProvider provider : avaialble) {
			if (provider.getClass().getName().equals(providerName)) {
				current = provider;
				return current;
			}
		}

		return null;
	}

	public static void initialize(Context context) {
		IntentFilter intentFilter = new IntentFilter(
				SynchronizationService.BROADCAST_ACTION);
		SynchronizationStatusReceiver receiver = new SynchronizationStatusReceiver();

		LocalBroadcastManager.getInstance(context).registerReceiver(receiver,
				intentFilter);
	}

	public static boolean isSynchronisationInProgress() {
		return mSynchronisationInProgress;
	}

	public static void setSynchronisationCallback(OnSynchronizeListener callback) {
		mSynchronisationListener = callback;
		if (mSynchronisationListener != null && mSynchronisationInProgress) {
			mSynchronisationListener.onSynchronizationStarted();
		}
	}

	protected File mTempFile;
	protected Context mContext;

	private OnAuthenticationListener mAuthenticationListener;
	private boolean mAuthenticationInProgess = false;
	protected Fragment mAuthenticationFragment;
	protected FragmentManager mAuthenticationFragmentManager;

	public AbstractSynchronizationProvider(Context context) {
		this.mContext = context;

		mTempFile = new File(context.getCacheDir(), getClass().getSimpleName());
	}

	public void continueAuthentication(int requestCode, int resultCode,
			Intent data) {
		if (mAuthenticationInProgess) {
			onContinueAuthentication(requestCode, resultCode, data);
		}
	}

	public abstract String getAccountName();

	/**
	 * @return An icon for the synchronization provider as a drawable resource.
	 */
	public abstract int getIcon();

	/**
	 * @return The name of the synchronization provider.
	 */
	public abstract String getName();

	public abstract boolean isAuthenticated();

	public void startAuthentication(Fragment fragment,
			OnAuthenticationListener listener) {
		mAuthenticationFragment = fragment;
		mAuthenticationFragmentManager = fragment.getFragmentManager();
		mAuthenticationListener = listener;
		mAuthenticationInProgess = true;
		onStartAuthentication();
	}

	public void synchronize() {
		synchronize(SYNC_NORMAL);
	}

	public void synchronize(int option) {
		if (mSynchronisationInProgress || !isAuthenticated()) {
			return;
		}

		mSynchronisationInProgress = true;

		Intent serviceIntent = new Intent(mContext,
				SynchronizationService.class);
		serviceIntent.putExtra(SynchronizationService.EXTRA_OPTION, option);
		mContext.startService(serviceIntent);
	}

	public void unlink() {
		Preferences prefs = new Preferences(mContext);
		prefs.setSynchronizationProvider(null);
		current = null;
		onUnlink();
	}

	protected void authenticationFinished(boolean success,
			boolean remoteDataAvailable) {
		if (success) {
			Preferences prefs = new Preferences(mContext);
			prefs.setSynchronizationProvider(getClass().getName());
		}

		mAuthenticationInProgess = false;
		mAuthenticationListener.onAuthenticationFinished(success,
				remoteDataAvailable);
	}

	protected boolean copyFile(File from, File to) {
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

	protected abstract void onContinueAuthentication(int requestCode,
			int resultCode, Intent data);

	protected abstract void onStartAuthentication();

	protected abstract boolean onSynchronize(int option);

	protected abstract void onUnlink();
}
