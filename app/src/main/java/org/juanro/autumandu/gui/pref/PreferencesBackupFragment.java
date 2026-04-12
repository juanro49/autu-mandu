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

package org.juanro.autumandu.gui.pref;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.TwoStatePreference;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.dialog.MessageDialogFragment;
import org.juanro.autumandu.util.backup.CSVExportImport;
import org.juanro.autumandu.util.sync.AbstractSyncProvider;
import org.juanro.autumandu.util.sync.Authenticator;
import org.juanro.autumandu.util.sync.SyncProviders;
import org.juanro.autumandu.viewmodel.BackupViewModel;

import java.io.IOException;

public class PreferencesBackupFragment extends PreferenceFragmentCompat {
    private static final String TAG = "PreferencesBackupFragme";

    private static final int REQUEST_BACKUP_OVERWRITE = 12;
    private static final int REQUEST_RESTORE = 13;
    private static final int REQUEST_EXPORT_CSV_OVERWRITE = 14;
    private static final int REQUEST_IMPORT_CSV = 15;
    private static final int REQUEST_BACKUP_PERMISSIONS = 16;
    private static final int REQUEST_RESTORE_PERMISSIONS = 17;
    private static final int REQUEST_EXPORT_CSV_PERMISSIONS = 18;
    private static final int REQUEST_IMPORT_CSV_PERMISSIONS = 19;
    private static final int REQUEST_AUTO_BACKUP_PERMISSIONS = 21;
    private static final int REQUEST_BACKUP_FOLDER_PERMISSIONS = 23;
    private static final int REQUEST_RESTORE_BACKUP_FOLDER_PERMISSIONS = 24;

    private AccountManager mAccountManager;
    private BackupViewModel mViewModel;
    private Preferences prefs;

    private final ActivityResultLauncher<Intent> mOpenFileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri backupUri = result.getData().getData();
                    if (backupUri != null) {
                        Log.v(TAG, "Restoring from URI " + backupUri);
                        doRestoreFinal(backupUri);
                    } else {
                        Toast.makeText(requireActivity(), R.string.pref_summary_restore_file_seems_wrong,
                                Toast.LENGTH_LONG).show();
                    }
                }
            });

    private final ActivityResultLauncher<Intent> mOpenDocumentTreeLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri baseDocumentTreeUri = result.getData().getData();
                    if (baseDocumentTreeUri != null) {
                        final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        requireActivity().getContentResolver().takePersistableUriPermission(baseDocumentTreeUri, takeFlags);
                        prefs = new Preferences(requireContext());
                        prefs.setBackupPath(baseDocumentTreeUri.toString());
                        mViewModel.getCsvExportImport().init();
                        setupImportCSVPreference();
                    }
                }
            });

    private int mPendingPermissionRequestCode = -1;
    private final ActivityResultLauncher<String> mRequestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    performActionAfterPermissionGranted(mPendingPermissionRequestCode);
                }
                mPendingPermissionRequestCode = -1;
            });

    private final OnPreferenceClickListener mSetupSync = preference -> {
        mAccountManager.addAccount(Authenticator.ACCOUNT_TYPE, null, null, null, getActivity(), future -> {
            try {
                future.getResult();
                setupSynchronizationPreference();
            } catch (OperationCanceledException | IOException | AuthenticatorException e) {
                Log.e(TAG, "Error adding sync account.", e);
            }
        }, null);

        return true;
    };

    private final OnPreferenceClickListener mUnlinkSync = preference -> {
        Account[] accounts = mAccountManager.getAccountsByType(Authenticator.ACCOUNT_TYPE);
        mAccountManager.removeAccount(accounts[0], getActivity(), future -> {
            try {
                future.getResult();
                setupSynchronizationPreference();
            } catch (OperationCanceledException | IOException | AuthenticatorException e) {
                Log.e(TAG, "Error removing sync account.", e);
            }
        }, null);

        return true;
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_backup, rootKey);

        mAccountManager = AccountManager.get(requireContext());
        mViewModel = new ViewModelProvider(this).get(BackupViewModel.class);

        // Synchronization
        setupSynchronizationPreference();

        // Backup
        Preference backup = findPreference("backup");
        if (backup != null) {
            backup.setOnPreferenceClickListener(
                    createOnClickListenerToAskForStorageAccess(REQUEST_BACKUP_PERMISSIONS));
        }

        Preference autoBackup = findPreference("behavior_auto_backup");
        if (autoBackup != null) {
            autoBackup.setOnPreferenceChangeListener(
                    createOnChangeListenerToAskForStorageAccess());
        }

        // Restore
        setupRestorePreference();

        // Backup Folder
        Preference backupFolder = findPreference("backup_folder");
        if (backupFolder != null) {
            backupFolder.setOnPreferenceClickListener(
                createOnClickListenerToAskForStorageAccess(REQUEST_BACKUP_FOLDER_PERMISSIONS));
        }

        Preference restoreBackupFolder = findPreference("restore_folder");
        if (restoreBackupFolder != null) {
            restoreBackupFolder.setOnPreferenceClickListener(
                createOnClickListenerToAskForStorageAccess(REQUEST_RESTORE_BACKUP_FOLDER_PERMISSIONS));
        }

        // Export CSV
        Preference export = findPreference("exportcsv");
        if (export != null) {
            export.setOnPreferenceClickListener(
                    createOnClickListenerToAskForStorageAccess(REQUEST_EXPORT_CSV_PERMISSIONS));
        }

        // Import CSV
        setupImportCSVPreference();

        // Register Fragment Result Listener
        getParentFragmentManager().setFragmentResultListener(MessageDialogFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
            int action = bundle.getInt(MessageDialogFragment.RESULT_ACTION);
            int requestCode = bundle.getInt(MessageDialogFragment.RESULT_REQUEST_CODE);
            if (action == MessageDialogFragment.ACTION_POSITIVE) {
                onDialogPositiveClick(requestCode);
            }
        });
    }

    private void onDialogPositiveClick(int requestCode) {
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

    private void performActionAfterPermissionGranted(int requestCode) {
        if (requestCode == REQUEST_BACKUP_PERMISSIONS) {
            doBackup(false);
        } else if (requestCode == REQUEST_RESTORE_PERMISSIONS) {
            doRestore(false);
        } else if (requestCode == REQUEST_EXPORT_CSV_PERMISSIONS) {
            doExportCSV(false);
        } else if (requestCode == REQUEST_IMPORT_CSV_PERMISSIONS) {
            doImportCSV(false);
        } else if (requestCode == REQUEST_AUTO_BACKUP_PERMISSIONS) {
            enableAutoBackup();
        } else if (requestCode == REQUEST_BACKUP_FOLDER_PERMISSIONS) {
            changeBackupDirectory();
        } else if (requestCode == REQUEST_RESTORE_BACKUP_FOLDER_PERMISSIONS) {
            prefs = new Preferences(requireContext());
            prefs.restoreDefaultBackupPath();
            Toast.makeText(requireActivity(), getString(R.string.pref_default_backup_path_restored) + prefs.getBackupPath(),
                Toast.LENGTH_LONG).show();
        }
    }

    private void enableAutoBackup() {
        TwoStatePreference autoBackupPref = findPreference("behavior_auto_backup");
        if (autoBackupPref != null) {
            autoBackupPref.setChecked(true);
        }
    }

    private void doBackup(boolean userWasAskedForOverwrite) {
        if (mViewModel.getBackup().backupFileExists() && !userWasAskedForOverwrite) {
            MessageDialogFragment.newInstance(
                    REQUEST_BACKUP_OVERWRITE,
                    R.string.alert_backup_overwrite_title,
                    getString(R.string.alert_backup_overwrite_message),
                    R.string.overwrite, android.R.string.cancel)
                    .show(getParentFragmentManager(), null);
        } else {
            mViewModel.runBackup(() -> requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireActivity(),
                        getString(R.string.toast_backup_succeeded, mViewModel.getBackup().getBackupDir().getUri()),
                        Toast.LENGTH_SHORT).show();
                setupRestorePreference();
            }), message -> requireActivity().runOnUiThread(() -> showError(getString(R.string.alert_backup_failed))));
        }
    }

    private void doExportCSV(boolean userWasAskedForOverwrite) {
        if (mViewModel.getCsvExportImport().anyExportFileExist() && !userWasAskedForOverwrite) {
            MessageDialogFragment.newInstance(
                    REQUEST_EXPORT_CSV_OVERWRITE,
                    R.string.alert_export_csv_overwrite_title,
                    getString(R.string.alert_export_csv_overwrite_message),
                    R.string.overwrite, android.R.string.cancel).show(
                    getParentFragmentManager(), null);
        } else {
            mViewModel.runExportCSV(() -> requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireActivity(), R.string.toast_export_csv_succeeded,
                        Toast.LENGTH_SHORT).show();
                setupImportCSVPreference();
            }), message -> requireActivity().runOnUiThread(() ->
                    showError(getString(R.string.alert_export_csv_failed, message))));
        }
    }

    private void doImportCSV(boolean userWasAsked) {
        if (!userWasAsked) {
            MessageDialogFragment.newInstance(REQUEST_IMPORT_CSV, R.string.alert_import_csv_title,
                    getString(R.string.alert_import_csv_message),
                    R.string.import_, android.R.string.cancel).show(
                    getParentFragmentManager(), null);
        } else {
            mViewModel.runImportCSV(() -> requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireActivity(), R.string.toast_import_csv_succeeded,
                            Toast.LENGTH_SHORT).show()), message -> requireActivity().runOnUiThread(() ->
                    showError(getString(R.string.alert_import_csv_failed, message))));
        }
    }

    private void doRestore(boolean userWasAsked) {
        if (!userWasAsked) {
            MessageDialogFragment.newInstance(REQUEST_RESTORE, R.string.alert_restore_title,
                    getString(R.string.alert_restore_message),
                    R.string.restore, android.R.string.cancel)
                    .show(getParentFragmentManager(), null);
        } else {
            Intent chooserSpecIntent = new Intent(Intent.ACTION_GET_CONTENT);
            chooserSpecIntent.addCategory(Intent.CATEGORY_OPENABLE);
            chooserSpecIntent.setType("*/*");

            try {
                mOpenFileLauncher.launch(Intent.createChooser(
                        chooserSpecIntent,
                        getString(R.string.pref_action_select_backup_file)));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(requireActivity(), R.string.pref_summary_no_file_selector,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void doRestoreFinal(Uri backupUri) {
        mViewModel.runRestore(backupUri, () -> requireActivity().runOnUiThread(() ->
                Toast.makeText(requireActivity(), R.string.toast_restore_succeeded,
                        Toast.LENGTH_SHORT).show()), message -> requireActivity().runOnUiThread(() ->
                showError(getString(R.string.alert_restore_failed))));
    }

    private void setupImportCSVPreference() {
        Preference import_ = findPreference("importcsv");
        if (import_ == null) return;

        if (mViewModel.getCsvExportImport().allExportFilesExist()) {
            import_.setSummary(R.string.pref_summary_import_csv);
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
        if (restore == null) return;

        restore.setSummary(getString(R.string.pref_summary_restore));
        restore.setEnabled(true);
        restore.setOnPreferenceClickListener(
                createOnClickListenerToAskForStorageAccess(REQUEST_RESTORE_PERMISSIONS));
    }

    private void setupSynchronizationPreference() {
        Preference sync = findPreference("sync");
        if (sync == null) return;

        Account[] accounts = mAccountManager.getAccountsByType(Authenticator.ACCOUNT_TYPE);
        if (accounts.length > 0) {
            AbstractSyncProvider syncProvider = SyncProviders.getSyncProviderByAccount(
                    requireContext(), accounts[0]);
            if (syncProvider != null) {
                sync.setTitle(syncProvider.getName());
                sync.setIcon(syncProvider.getIcon());
                sync.setSummary(getString(
                        R.string.pref_summary_sync_current_provider,
                        accounts[0].name));
                sync.setOnPreferenceClickListener(mUnlinkSync);
            }
        } else {
            sync.setTitle(R.string.pref_title_sync_setup);
            sync.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_null));
            sync.setSummary(R.string.pref_summary_sync_setup);
            sync.setOnPreferenceClickListener(mSetupSync);
        }
    }

    private Preference.OnPreferenceChangeListener createOnChangeListenerToAskForStorageAccess() {
        return (preference, o) -> {
            if (((Boolean) o)) {
                if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                                PackageManager.PERMISSION_GRANTED) {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        Toast.makeText(requireActivity(), R.string.toast_need_storage_permission, Toast.LENGTH_LONG).show();
                    }

                    mPendingPermissionRequestCode = REQUEST_AUTO_BACKUP_PERMISSIONS;
                    mRequestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    return false;
                }
            }
            return true;
        };
    }

    private OnPreferenceClickListener createOnClickListenerToAskForStorageAccess(final int requestCode) {
        return preference -> {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                            PackageManager.PERMISSION_GRANTED) {
                performActionAfterPermissionGranted(requestCode);
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(requireActivity(), R.string.toast_need_storage_permission, Toast.LENGTH_LONG).show();
                }

                mPendingPermissionRequestCode = requestCode;
                mRequestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }

            return true;
        };
    }

    private void showError(String message) {
        MessageDialogFragment.newInstance(0, R.string.alert_error_title,
                message, android.R.string.ok, null)
                .show(getParentFragmentManager(), null);
    }

    public void changeBackupDirectory() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        mOpenDocumentTreeLauncher.launch(intent);
    }
}
