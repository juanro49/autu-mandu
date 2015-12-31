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

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
    private static final int REQUEST_BACKUP_PERMISSIONS = 16;
    private static final int REQUEST_RESTORE_PERMISSIONS = 17;
    private static final int REQUEST_EXPORT_CSV_PERMISSIONS = 18;
    private static final int REQUEST_IMPORT_CSV_PERMISSIONS = 19;

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

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof PreferencesActivity) {
            ((PreferencesActivity) context).onAttachFragment(this);
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
            backup.setOnPreferenceClickListener(
                    createOnClickListenerToAskForStorageAccess(REQUEST_BACKUP_PERMISSIONS));
        }

        // Restore
        {
            setupRestorePreference();
        }

        // Export CSV
        {
            Preference export = findPreference("exportcsv");
            export.setOnPreferenceClickListener(
                    createOnClickListenerToAskForStorageAccess(REQUEST_EXPORT_CSV_PERMISSIONS));
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
            doBackup(true);
        } else if (requestCode == REQUEST_RESTORE) {
            doRestore(true);
        } else if (requestCode == REQUEST_EXPORT_CSV_OVERWRITE) {
            doExportCSV(true);
        } else if (requestCode == REQUEST_IMPORT_CSV) {
            doImportCSV(true);
        }
    }

    @Override
    public void onDialogNegativeClick(int requestCode) {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_BACKUP_PERMISSIONS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                doBackup(false);
            }
        } else if (requestCode == REQUEST_RESTORE_PERMISSIONS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                doRestore(false);
            }
        } else if (requestCode == REQUEST_EXPORT_CSV_PERMISSIONS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                doExportCSV(false);
            }
        } else if (requestCode == REQUEST_IMPORT_CSV_PERMISSIONS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                doImportCSV(false);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void doBackup(boolean userWasAskedForOverwrite) {
        if (mBackup.backupFileExists() && !userWasAskedForOverwrite) {
            MessageDialogFragment.newInstance(
                    PreferencesBackupFragment.this,
                    REQUEST_BACKUP_OVERWRITE,
                    R.string.alert_backup_overwrite_title,
                    getString(R.string.alert_backup_overwrite_message),
                    R.string.overwrite, android.R.string.cancel)
                    .show(getFragmentManager(), null);
        } else if (mBackup.backup()) {
            Toast.makeText(getActivity(),
                    getString(R.string.toast_backup_success, Backup.FILE_NAME),
                    Toast.LENGTH_SHORT).show();
            setupRestorePreference();
        } else {
            Toast.makeText(getActivity(), R.string.toast_backup_failed,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void doExportCSV(boolean userWasAskedForOverwrite) {
        if (mCSVExportImport.anyExportFileExist() && !userWasAskedForOverwrite) {
            MessageDialogFragment.newInstance(
                    PreferencesBackupFragment.this,
                    REQUEST_EXPORT_CSV_OVERWRITE,
                    R.string.alert_export_csv_overwrite_title,
                    getString(R.string.alert_export_csv_overwrite_message),
                    R.string.overwrite, android.R.string.cancel).show(
                    getFragmentManager(), null);
        } else if (mCSVExportImport.export()) {
            Toast.makeText(getActivity(), R.string.toast_export_csv_succeeded,
                    Toast.LENGTH_SHORT).show();
            setupImportCSVPreference();
        } else {
            Toast.makeText(getActivity(), R.string.toast_export_csv_failed,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void doImportCSV(boolean userWasAsked) {
        if (!userWasAsked) {
            MessageDialogFragment.newInstance(PreferencesBackupFragment.this,
                    REQUEST_IMPORT_CSV, R.string.alert_import_csv_title,
                    getString(R.string.alert_import_csv_message),
                    R.string.import_, android.R.string.cancel).show(
                    getFragmentManager(), null);
        } else if (mCSVExportImport.import_()) {
            Toast.makeText(getActivity(), R.string.toast_import_csv_success,
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(), R.string.toast_import_csv_failed,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void doRestore(boolean userWasAsked) {
        if (!userWasAsked) {
            MessageDialogFragment.newInstance(PreferencesBackupFragment.this,
                    REQUEST_RESTORE, R.string.alert_restore_title,
                    getString(R.string.alert_restore_message),
                    R.string.restore, android.R.string.cancel)
                    .show(getFragmentManager(), null);
        } else if (mBackup.restore()) {
            Toast.makeText(getActivity(), R.string.toast_restore_success,
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(), R.string.toast_restore_failed,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void setupImportCSVPreference() {
        Preference import_ = findPreference("importcsv");
        if (mCSVExportImport.allExportFilesExist()) {
            import_.setSummary(getString(R.string.pref_summary_import_csv,
                    Backup.FILE_NAME));
            import_.setEnabled(true);
        } else {
            import_.setSummary(getString(
                    R.string.pref_summary_import_csv_no_data,
                    CSVExportImport.DIRECTORY));
            import_.setEnabled(false);
        }

        import_.setOnPreferenceClickListener(
                createOnClickListenerToAskForStorageAccess(REQUEST_IMPORT_CSV_PERMISSIONS));
    }

    private void setupRestorePreference() {
        Preference restore = findPreference("restore");
        if (mBackup.backupFileExists()) {
            restore.setSummary(getString(R.string.pref_summary_restore,
                    Backup.FILE_NAME));
            restore.setEnabled(true);
        } else {
            restore.setSummary(getString(R.string.pref_summary_restore_no_data,
                    Backup.FILE_NAME));
            restore.setEnabled(false);
        }

        restore.setOnPreferenceClickListener(
                createOnClickListenerToAskForStorageAccess(REQUEST_RESTORE_PERMISSIONS));
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

    private OnPreferenceClickListener createOnClickListenerToAskForStorageAccess(final int requestCode) {
        final String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        return new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (ContextCompat.checkSelfPermission(getActivity(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED) {
                    onRequestPermissionsResult(requestCode, permissions,
                            new int[]{PackageManager.PERMISSION_GRANTED});
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        Toast.makeText(getActivity(), R.string.toast_need_storage_permission,
                                Toast.LENGTH_SHORT).show();
                    }

                    ActivityCompat.requestPermissions(getActivity(), permissions, requestCode);
                }

                return true;
            }
        };
    }
}