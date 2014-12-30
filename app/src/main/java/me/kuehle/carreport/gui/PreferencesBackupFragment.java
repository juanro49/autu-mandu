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

package me.kuehle.carreport.gui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import me.kuehle.carreport.R;
import me.kuehle.carreport.gui.dialog.ListDialogFragment;
import me.kuehle.carreport.gui.dialog.ListDialogFragment.ListDialogFragmentListener;
import me.kuehle.carreport.gui.dialog.MessageDialogFragment;
import me.kuehle.carreport.gui.dialog.MessageDialogFragment.MessageDialogFragmentListener;
import me.kuehle.carreport.util.backup.AbstractSynchronizationProvider;
import me.kuehle.carreport.util.backup.AbstractSynchronizationProvider.OnAuthenticationListener;
import me.kuehle.carreport.util.backup.AbstractSynchronizationProvider.OnUnlinkListener;
import me.kuehle.carreport.util.backup.Backup;
import me.kuehle.carreport.util.backup.CSVExportImport;
import me.kuehle.carreport.util.backup.DropboxSynchronizationProvider;

public class PreferencesBackupFragment extends PreferenceFragment implements
        MessageDialogFragmentListener, ListDialogFragmentListener,
        OnAuthenticationListener, OnUnlinkListener {
    private static final int REQUEST_CHOOSE_SYNC_PROVIDER = 10;
    private static final int REQUEST_FIRST_SYNC = 11;
    private static final int REQUEST_BACKUP_OVERWRITE = 12;
    private static final int REQUEST_RESTORE = 13;
    private static final int REQUEST_EXPORT_CSV_OVERWRITE = 14;
    private static final int REQUEST_IMPORT_CSV = 15;

    private AbstractSynchronizationProvider mCurrentSyncProvider;
    private Backup mBackup;
    private CSVExportImport mCSVExportImport;

    private OnPreferenceClickListener mSetupSync = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            AbstractSynchronizationProvider[] providers = AbstractSynchronizationProvider
                    .getAvailable(getActivity());
            String[] items = new String[providers.length];
            int[] icons = new int[providers.length];
            for (int i = 0; i < providers.length; i++) {
                items[i] = providers[i].getName();
                icons[i] = providers[i].getIcon();
            }

            ListDialogFragment.newInstance(PreferencesBackupFragment.this,
                    REQUEST_CHOOSE_SYNC_PROVIDER, null, items, icons,
                    android.R.string.cancel).show(getFragmentManager(), null);

            return true;
        }
    };

    private OnPreferenceClickListener mUnlinkSync = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            mCurrentSyncProvider.unlink(PreferencesBackupFragment.this);
            mCurrentSyncProvider = null;
            setupSynchronizationPreference();
            return true;
        }
    };

    private OnPreferenceClickListener mBackupClick = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (mBackup.backupFileExists()) {
                MessageDialogFragment.newInstance(
                        PreferencesBackupFragment.this,
                        REQUEST_BACKUP_OVERWRITE,
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

    private OnPreferenceClickListener mRestoreClick = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            MessageDialogFragment.newInstance(PreferencesBackupFragment.this,
                    REQUEST_RESTORE, R.string.alert_restore_title,
                    getString(R.string.alert_restore_message),
                    R.string.restore, android.R.string.cancel).show(
                    getFragmentManager(), null);
            return true;
        }
    };

    private OnPreferenceClickListener mExportCSVClick = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (mCSVExportImport.anyExportFileExist()) {
                MessageDialogFragment.newInstance(
                        PreferencesBackupFragment.this,
                        REQUEST_EXPORT_CSV_OVERWRITE,
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

    private OnPreferenceClickListener mImportCSVClick = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            MessageDialogFragment.newInstance(PreferencesBackupFragment.this,
                    REQUEST_IMPORT_CSV, R.string.alert_import_csv_title,
                    getString(R.string.alert_import_csv_message),
                    R.string.import_, android.R.string.cancel).show(
                    getFragmentManager(), null);
            return true;
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mCurrentSyncProvider != null) {
            mCurrentSyncProvider.continueAuthentication(requestCode, resultCode, data);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof PreferencesActivity) {
            activity.onAttachFragment(this);
        } else {
            throw new ClassCastException(
                    "This fragment can only be attached to the PreferencesActivity!");
        }
    }

    @Override
    public void onAuthenticationFinished(boolean success,
                                         boolean remoteDataAvailable) {
        if (success) {
            setupSynchronizationPreference();
            startFirstSynchronisation(remoteDataAvailable);
        } else {
            Toast.makeText(getActivity(),
                    R.string.toast_sync_authentication_failed,
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onUnlinkingFinished() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_backup);
        mCurrentSyncProvider = AbstractSynchronizationProvider
                .getCurrent(getActivity());
        mBackup = new Backup();
        mCSVExportImport = new CSVExportImport();

        // Synchronization
        {
            setupSynchronizationPreference();
        }

        // Backup
        {
            Preference backup = findPreference("backup");
            backup.setEnabled(this.mBackup.canBackup());
            backup.setOnPreferenceClickListener(mBackupClick);
        }

        // Restore
        {
            setupRestorePreference();
        }

        // Export CSV
        {
            Preference export = findPreference("exportcsv");
            export.setEnabled(mCSVExportImport.canExport());
            export.setOnPreferenceClickListener(mExportCSVClick);
        }

        // Import CSV
        {
            setupImportCSVPreference();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().onAttachFragment(null);
    }

    @Override
    public void onDialogNegativeClick(int requestCode) {
        if (requestCode == REQUEST_FIRST_SYNC) {
            mCurrentSyncProvider.synchronize(DropboxSynchronizationProvider.SYNC_UPLOAD);
        }
    }

    @Override
    public void onDialogPositiveClick(int requestCode) {
        if (requestCode == REQUEST_FIRST_SYNC) {
            mCurrentSyncProvider.synchronize(DropboxSynchronizationProvider.SYNC_DOWNLOAD);
        } else if (requestCode == REQUEST_BACKUP_OVERWRITE) {
            doBackup();
        } else if (requestCode == REQUEST_RESTORE) {
            doRestore();
        } else if (requestCode == REQUEST_EXPORT_CSV_OVERWRITE) {
            doExportCSV();
        } else if (requestCode == REQUEST_IMPORT_CSV) {
            doImportCSV();
        }
    }

    @Override
    public void onDialogPositiveClick(int requestCode, int selectedPosition) {
        if (requestCode == REQUEST_CHOOSE_SYNC_PROVIDER) {
            mCurrentSyncProvider = AbstractSynchronizationProvider
                    .getAvailable(getActivity())[selectedPosition];
            mCurrentSyncProvider.startAuthentication(
                    PreferencesBackupFragment.this,
                    PreferencesBackupFragment.this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCurrentSyncProvider != null) {
            mCurrentSyncProvider.continueAuthentication(0, 0, null);
        }
    }

    private void doBackup() {
        if (mBackup.backup()) {
            Toast.makeText(getActivity(),
                    getString(R.string.toast_backup_success, Backup.FILE_NAME),
                    Toast.LENGTH_SHORT).show();
            setupRestorePreference();
        } else {
            Toast.makeText(getActivity(), R.string.toast_backup_failed,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void doExportCSV() {
        if (mCSVExportImport.export()) {
            Toast.makeText(getActivity(), R.string.toast_export_csv_succeeded,
                    Toast.LENGTH_SHORT).show();
            setupImportCSVPreference();
        } else {
            Toast.makeText(getActivity(), R.string.toast_export_csv_failed,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void doImportCSV() {
        if (mCSVExportImport.import_()) {
            Toast.makeText(getActivity(), R.string.toast_import_csv_success,
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(), R.string.toast_import_csv_failed,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void doRestore() {
        if (mBackup.restore()) {
            Toast.makeText(getActivity(), R.string.toast_restore_success,
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(), R.string.toast_restore_failed,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void setupImportCSVPreference() {
        Preference import_ = findPreference("importcsv");
        if (mCSVExportImport.canImport()) {
            import_.setSummary(getString(R.string.pref_summary_import_csv,
                    Backup.FILE_NAME));
            import_.setEnabled(true);
        } else {
            import_.setSummary(getString(
                    R.string.pref_summary_import_csv_no_data,
                    CSVExportImport.DIRECTORY));
            import_.setEnabled(false);
        }

        import_.setOnPreferenceClickListener(mImportCSVClick);
    }

    private void setupRestorePreference() {
        Preference restore = findPreference("restore");
        if (mBackup.canRestore()) {
            restore.setSummary(getString(R.string.pref_summary_restore,
                    Backup.FILE_NAME));
            restore.setEnabled(true);
        } else {
            restore.setSummary(getString(R.string.pref_summary_restore_no_data,
                    Backup.FILE_NAME));
            restore.setEnabled(false);
        }
        restore.setOnPreferenceClickListener(mRestoreClick);
    }

    private void setupSynchronizationPreference() {
        Preference sync = findPreference("sync_current_provider");
        Preference syncOnStart = findPreference("sync_on_start");
        Preference syncOnChange = findPreference("sync_on_change");

        if (mCurrentSyncProvider != null
                && mCurrentSyncProvider.isAuthenticated()) {
            String name = mCurrentSyncProvider.getAccountName();
            if (name == null) {
                name = mCurrentSyncProvider.getName();
            }

            sync.setTitle(mCurrentSyncProvider.getName());
            sync.setIcon(mCurrentSyncProvider.getIcon());
            sync.setSummary(getString(
                    R.string.pref_summary_sync_current_provider,
                    name));
            sync.setOnPreferenceClickListener(mUnlinkSync);
            syncOnStart.setEnabled(true);
            syncOnChange.setEnabled(true);
        } else {
            sync.setTitle(R.string.pref_title_sync_setup);
            sync.setIcon(R.drawable.ic_null);
            sync.setSummary(R.string.pref_summary_sync_setup);
            sync.setOnPreferenceClickListener(mSetupSync);
            syncOnStart.setEnabled(false);
            syncOnChange.setEnabled(false);
        }
    }

    private void startFirstSynchronisation(boolean remoteDataAvailable) {
        if (remoteDataAvailable) {
            MessageDialogFragment.newInstance(this, REQUEST_FIRST_SYNC,
                    R.string.alert_sync_first_sync_title,
                    getString(R.string.alert_sync_first_sync_message),
                    R.string.alert_sync_first_sync_download,
                    R.string.alert_sync_first_sync_upload).show(
                    getFragmentManager(), null);
        } else {
            mCurrentSyncProvider.synchronize(DropboxSynchronizationProvider.SYNC_UPLOAD);
        }
    }
}