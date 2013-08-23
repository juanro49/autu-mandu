package me.kuehle.carreport.gui;

import me.kuehle.carreport.R;
import me.kuehle.carreport.gui.dialog.MessageDialogFragment;
import me.kuehle.carreport.gui.dialog.MessageDialogFragment.MessageDialogFragmentListener;
import me.kuehle.carreport.gui.dialog.ProgressDialogFragment;
import me.kuehle.carreport.util.backup.Backup;
import me.kuehle.carreport.util.backup.CSVExportImport;
import me.kuehle.carreport.util.backup.Dropbox;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.widget.Toast;

public class PreferencesBackupFragment extends PreferenceFragment implements
		MessageDialogFragmentListener {
	private static final int DROPBOX_FIRST_SYNC_REQUEST_CODE = 0;
	private static final int BACKUP_OVERWRITE_REQUEST_CODE = 1;
	private static final int RESTORE_REQUEST_CODE = 2;
	private static final int EXPORT_CSV_REQUEST_CODE = 3;
	private static final int IMPORT_CSV_REQUEST_CODE = 4;

	private Dropbox dropbox;
	private boolean dropboxAuthenticationInProgress = false;
	private Backup backup;
	private CSVExportImport csvExportImport;

	private OnPreferenceClickListener mSyncDropbox = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			if (dropbox.isLinked()) {
				dropbox.unlink();
				setupDropdoxPreference();
			} else {
				dropboxAuthenticationInProgress = true;
				dropbox.startAuthentication(getActivity());
			}

			return true;
		}
	};

	private OnPreferenceClickListener mBackup = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			if (backup.backupFileExists()) {
				MessageDialogFragment.newInstance(PreferencesBackupFragment.this,
						BACKUP_OVERWRITE_REQUEST_CODE,
						R.string.alert_backup_overwrite_title,
						getString(R.string.alert_backup_overwrite_message),
						R.string.overwrite, android.R.string.cancel).show(
						getFragmentManager(), null);
			} else {
				doBackup();
			}
			return true;
		}

	};

	private OnPreferenceClickListener mRestore = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			MessageDialogFragment.newInstance(PreferencesBackupFragment.this,
					RESTORE_REQUEST_CODE, R.string.alert_restore_title,
					getString(R.string.alert_restore_message),
					R.string.restore, android.R.string.cancel).show(
					getFragmentManager(), null);
			return true;
		}
	};

	private OnPreferenceClickListener mExportCSV = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			if (csvExportImport.anyExportFileExist()) {
				MessageDialogFragment.newInstance(PreferencesBackupFragment.this,
						EXPORT_CSV_REQUEST_CODE,
						R.string.alert_export_csv_overwrite_title,
						getString(R.string.alert_export_csv_overwrite_message),
						R.string.overwrite, android.R.string.cancel).show(
						getFragmentManager(), null);
			} else {
				doExportCSV();
			}

			return true;
		}
	};

	private OnPreferenceClickListener mImportCSV = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			MessageDialogFragment.newInstance(PreferencesBackupFragment.this,
					IMPORT_CSV_REQUEST_CODE, R.string.alert_import_csv_title,
					getString(R.string.alert_import_csv_message),
					R.string.import_, android.R.string.cancel).show(
					getFragmentManager(), null);
			return true;
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences_backup);
		dropbox = Dropbox.getInstance();
		backup = new Backup();
		csvExportImport = new CSVExportImport();

		// Sync Dropbox
		{
			setupDropdoxPreference();
		}

		// Backup
		{
			Preference backup = findPreference("backup");
			backup.setEnabled(this.backup.canBackup());
			backup.setOnPreferenceClickListener(mBackup);
		}

		// Restore
		{
			setupRestorePreference();
		}

		// Export CSV
		{
			Preference export = findPreference("exportcsv");
			export.setEnabled(csvExportImport.canExport());
			export.setOnPreferenceClickListener(mExportCSV);
		}

		// Import CSV
		{
			setupImportCSVPreference();
		}
	}

	@Override
	public void onDialogNegativeClick(int requestCode) {
		if (requestCode == DROPBOX_FIRST_SYNC_REQUEST_CODE) {
			dropbox.synchronize(Dropbox.SYNC_UPLOAD);
		}
	}

	@Override
	public void onDialogPositiveClick(int requestCode) {
		if (requestCode == DROPBOX_FIRST_SYNC_REQUEST_CODE) {
			dropbox.synchronize(Dropbox.SYNC_DOWNLOAD);
		} else if (requestCode == BACKUP_OVERWRITE_REQUEST_CODE) {
			doBackup();
		} else if (requestCode == RESTORE_REQUEST_CODE) {
			doRestore();
		} else if (requestCode == EXPORT_CSV_REQUEST_CODE) {
			doExportCSV();
		} else if (requestCode == IMPORT_CSV_REQUEST_CODE) {
			doImportCSV();
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		if (dropboxAuthenticationInProgress) {
			dropboxAuthenticationInProgress = false;

			ProgressDialogFragment.newInstance(
					getString(R.string.alert_dropbox_finishing_authentication))
					.show(getFragmentManager(), "progress");

			dropbox.finishAuthentication(new Dropbox.OnAuthenticationFinishedListener() {
				@Override
				public void authenticationFinished(boolean success,
						String accountName, boolean remoteDataAvailable) {
					((ProgressDialogFragment) getFragmentManager()
							.findFragmentByTag("progress")).dismiss();
					if (success) {
						setupDropdoxPreference();
						if (remoteDataAvailable) {
							dropboxFirstSynchronisation();
						} else {
							dropbox.synchronize(Dropbox.SYNC_UPLOAD);
						}
					} else {
						Toast.makeText(getActivity(),
								R.string.toast_dropbox_authentication_failed,
								Toast.LENGTH_SHORT).show();
					}
				}
			});
		}
	}

	private void doBackup() {
		if (backup.backup()) {
			Toast.makeText(getActivity(),
					getString(R.string.toast_backup_success, Backup.FILE_NAME),
					Toast.LENGTH_SHORT).show();
			setupRestorePreference();
		} else {
			Toast.makeText(getActivity(), R.string.toast_backup_failed,
					Toast.LENGTH_SHORT).show();
		}
	}

	private void doRestore() {
		if (backup.restore()) {
			Toast.makeText(getActivity(), R.string.toast_restore_success,
					Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(getActivity(), R.string.toast_restore_failed,
					Toast.LENGTH_SHORT).show();
		}
	}

	private void doExportCSV() {
		if (csvExportImport.export()) {
			Toast.makeText(getActivity(), R.string.toast_export_csv_succeeded,
					Toast.LENGTH_SHORT).show();
			setupImportCSVPreference();
		} else {
			Toast.makeText(getActivity(), R.string.toast_export_csv_failed,
					Toast.LENGTH_SHORT).show();
		}
	}

	private void doImportCSV() {
		if (csvExportImport.import_()) {
			Toast.makeText(getActivity(), R.string.toast_import_success,
					Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(getActivity(), R.string.toast_import_failed,
					Toast.LENGTH_SHORT).show();
		}
	}

	private void dropboxFirstSynchronisation() {
		MessageDialogFragment.newInstance(this,
				DROPBOX_FIRST_SYNC_REQUEST_CODE,
				R.string.alert_dropbox_first_sync_title,
				getString(R.string.alert_dropbox_first_sync_message),
				R.string.alert_dropbox_first_sync_download,
				R.string.alert_dropbox_first_sync_upload).show(
				getFragmentManager(), null);
	}

	private void setupDropdoxPreference() {
		Preference sync = findPreference("sync_dropbox");
		sync.setOnPreferenceClickListener(mSyncDropbox);

		if (dropbox.isLinked()) {
			sync.setSummary(getString(
					R.string.pref_summary_sync_dropbox_unlink,
					dropbox.getAccountName()));
		} else {
			sync.setSummary(R.string.pref_summary_sync_dropbox_link);
		}
	}

	private void setupRestorePreference() {
		Preference restore = findPreference("restore");
		if (backup.canRestore()) {
			restore.setSummary(getString(R.string.pref_summary_restore,
					Backup.FILE_NAME));
			restore.setEnabled(true);
		} else {
			restore.setSummary(getString(R.string.pref_summary_restore_no_data,
					Backup.FILE_NAME));
			restore.setEnabled(false);
		}
		restore.setOnPreferenceClickListener(mRestore);
	}

	private void setupImportCSVPreference() {
		Preference import_ = findPreference("importcsv");
		if (csvExportImport.canImport()) {
			import_.setSummary(getString(R.string.pref_summary_importcsv,
					Backup.FILE_NAME));
			import_.setEnabled(true);
		} else {
			import_.setSummary(getString(
					R.string.pref_summary_importcsv_no_data,
					CSVExportImport.DIRECTORY));
			import_.setEnabled(false);
		}

		import_.setOnPreferenceClickListener(mImportCSV);
	}
}