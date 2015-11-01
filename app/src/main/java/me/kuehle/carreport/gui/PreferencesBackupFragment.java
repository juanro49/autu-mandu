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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

import me.kuehle.carreport.R;
import me.kuehle.carreport.gui.dialog.MessageDialogFragment;
import me.kuehle.carreport.gui.dialog.MessageDialogFragment.MessageDialogFragmentListener;
import me.kuehle.carreport.util.backup.Backup;
import me.kuehle.carreport.util.backup.CSVExportImport;
import me.kuehle.carreport.util.sync.AbstractSyncProvider;
import me.kuehle.carreport.util.sync.Authenticator;
import me.kuehle.carreport.util.sync.SyncProviders;

public class PreferencesBackupFragment extends PreferenceFragment implements
        MessageDialogFragmentListener {
    private static final String TAG = "PreferencesBackupFragme";

    private static final int REQUEST_BACKUP_OVERWRITE = 12;
    private static final int REQUEST_RESTORE = 13;
    private static final int REQUEST_EXPORT_CSV_OVERWRITE = 14;
    private static final int REQUEST_IMPORT_CSV = 15;

    private AccountManager mAccountManager;
    private Backup mBackup;
    private CSVExportImport mCSVExportImport;

    private OnPreferenceClickListener mSetupSync = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            mAccountManager.addAccount(Authenticator.ACCOUNT_TYPE, null, null, null, getActivity(), new AccountManagerCallback<Bundle>() {
                @Override
                public void run(AccountManagerFuture<Bundle> future) {
                    try {
                        future.getResult();
                        setupSynchronizationPreference();
                    } catch (OperationCanceledException | IOException | AuthenticatorException e) {
                        e.printStackTrace();
                    }
                }
            }, null);

            return true;
        }
    };

    private OnPreferenceClickListener mUnlinkSync = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            Account[] accounts = mAccountManager.getAccountsByType(Authenticator.ACCOUNT_TYPE);
            mAccountManager.removeAccount(accounts[0], new AccountManagerCallback<Boolean>() {
                @Override
                public void run(AccountManagerFuture<Boolean> future) {
                    try {
                        if (future.getResult()) {
                            setupSynchronizationPreference();
                        }
                    } catch (OperationCanceledException | IOException | AuthenticatorException e) {
                        Log.e(TAG, "Error removing sync account.", e);
                    }
                }
            }, null);

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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof PreferencesActivity) {
            activity.onAttachFragment(this);
        } else {
            throw new ClassCastException("This fragment can only be attached to the PreferencesActivity!");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_backup);
        mAccountManager = AccountManager.get(getActivity());
        mBackup = new Backup(getActivity());
        mCSVExportImport = new CSVExportImport(getActivity());

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
    public void onDialogPositiveClick(int requestCode) {
        if (requestCode == REQUEST_BACKUP_OVERWRITE) {
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
    public void onDialogNegativeClick(int requestCode) {
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
        Preference sync = findPreference("sync");
        Account[] accounts = mAccountManager.getAccountsByType(Authenticator.ACCOUNT_TYPE);
        if (accounts.length > 0) {
            AbstractSyncProvider syncProvider = SyncProviders.getSyncProviderByAccount(
                    getActivity(), accounts[0]);
            sync.setTitle(syncProvider.getName());
            sync.setIcon(syncProvider.getIcon());
            sync.setSummary(getString(
                    R.string.pref_summary_sync_current_provider,
                    accounts[0].name));
            sync.setOnPreferenceClickListener(mUnlinkSync);
        } else {
            sync.setTitle(R.string.pref_title_sync_setup);
            sync.setIcon(R.drawable.ic_null);
            sync.setSummary(R.string.pref_summary_sync_setup);
            sync.setOnPreferenceClickListener(mSetupSync);
        }
    }
}