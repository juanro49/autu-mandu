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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import org.juanro.autumandu.AutuManduApplication;
import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.AuthenticatorAddAccountActivity;
import org.juanro.autumandu.util.sync.AbstractSyncProvider;
import org.juanro.autumandu.util.sync.SyncManager;
import org.juanro.autumandu.util.sync.SyncProviders;
import org.juanro.autumandu.viewmodel.BackupViewModel;

public class PreferencesBackupFragment extends PreferenceFragmentCompat {
    public static final String EXTRA_IMPORT_CSV_URI = "import_csv_uri";
    public static final String EXTRA_RESTORE_DB_URI = "restore_db_uri";

    private static final String PREFERENCE_AUTO_BACKUP = "behavior_auto_backup";


    private static final int REQUEST_AUTO_BACKUP_PERMISSIONS = 1;
    private static final int REQUEST_BACKUP_PERMISSIONS = 2;
    private static final int REQUEST_RESTORE_PERMISSIONS = 3;
    private static final int REQUEST_EXPORT_PERMISSIONS = 4;
    private static final int REQUEST_IMPORT_PERMISSIONS = 5;

    private int mPendingPermissionRequestCode = -1;
    private BackupViewModel mViewModel;

    private final ActivityResultLauncher<String> mRequestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (Boolean.TRUE.equals(isGranted)) {
                    if (mPendingPermissionRequestCode == REQUEST_AUTO_BACKUP_PERMISSIONS) {
                        SwitchPreferenceCompat autoBackup = findPreference(PREFERENCE_AUTO_BACKUP);
                        if (autoBackup != null) {
                            autoBackup.setChecked(true);
                        }
                    } else if (mPendingPermissionRequestCode != -1) {
                        performActionAfterPermissionGranted(mPendingPermissionRequestCode);
                    }
                } else {
                    Toast.makeText(requireActivity(), R.string.toast_need_storage_permission, Toast.LENGTH_LONG).show();
                    if (mPendingPermissionRequestCode == REQUEST_AUTO_BACKUP_PERMISSIONS) {
                        SwitchPreferenceCompat autoBackup = findPreference(PREFERENCE_AUTO_BACKUP);
                        if (autoBackup != null) {
                            autoBackup.setChecked(false);
                        }
                    }
                }
                mPendingPermissionRequestCode = -1;
            }
    );

    private final ActivityResultLauncher<Intent> mOpenDocumentTreeLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        requireContext().getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        new Preferences(requireContext()).setBackupPath(uri.toString());
                        updateBackupPathSummary();
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> mSetupSyncLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                    updateSyncPreference();
                }
            }
    );

    private final ActivityResultLauncher<String[]> mOpenFileLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    performRestore(uri);
                }
            }
    );

    private final OnPreferenceClickListener mBackup = createOnClickListenerToAskForStorageAccess(REQUEST_BACKUP_PERMISSIONS);
    private final OnPreferenceClickListener mRestore = createOnClickListenerToAskForStorageAccess(REQUEST_RESTORE_PERMISSIONS);
    private final OnPreferenceClickListener mExport = createOnClickListenerToAskForStorageAccess(REQUEST_EXPORT_PERMISSIONS);
    private final OnPreferenceClickListener mImport = createOnClickListenerToAskForStorageAccess(REQUEST_IMPORT_PERMISSIONS);

    private final OnPreferenceClickListener mSelectBackupFolder = preference -> {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        mOpenDocumentTreeLauncher.launch(intent);
        return true;
    };

    private final OnPreferenceClickListener mRestoreDefaultBackupFolder = preference -> {
        new Preferences(requireContext()).restoreDefaultBackupPath();
        updateBackupPathSummary();
        return true;
    };

    private final OnPreferenceClickListener mSetupSync = preference -> {
        Intent intent = new Intent(requireContext(), AuthenticatorAddAccountActivity.class);
        mSetupSyncLauncher.launch(intent);
        return true;
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_backup);

        mViewModel = new ViewModelProvider(this).get(BackupViewModel.class);

        Bundle args = getArguments();
        if (args != null && args.containsKey(EXTRA_RESTORE_DB_URI)) {
            performRestore(androidx.core.os.BundleCompat.getParcelable(args, EXTRA_RESTORE_DB_URI, Uri.class));
        }

        SwitchPreferenceCompat autoBackup = findPreference(PREFERENCE_AUTO_BACKUP);
        if (autoBackup != null) {
            autoBackup.setOnPreferenceChangeListener(createOnChangeListenerToAskForStorageAccess());
        }

        Preference backup = findPreference("backup_now");
        if (backup != null) {
            backup.setOnPreferenceClickListener(mBackup);
        }

        Preference restore = findPreference("restore_now");
        if (restore != null) {
            restore.setOnPreferenceClickListener(mRestore);
        }

        Preference export = findPreference("export_csv");
        if (export != null) {
            export.setOnPreferenceClickListener(mExport);
        }

        Preference importCsv = findPreference("import_csv");
        if (importCsv != null) {
            importCsv.setOnPreferenceClickListener(mImport);
        }

        Preference selectBackupFolder = findPreference("backup_folder");
        if (selectBackupFolder != null) {
            selectBackupFolder.setOnPreferenceClickListener(mSelectBackupFolder);
        }

        Preference restoreDefaultBackupFolder = findPreference("backup_folder_default");
        if (restoreDefaultBackupFolder != null) {
            restoreDefaultBackupFolder.setOnPreferenceClickListener(mRestoreDefaultBackupFolder);
        }

        updateBackupPathSummary();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSyncPreference();
    }

    private void updateBackupPathSummary() {
        Preference selectBackupFolder = findPreference("backup_folder");
        if (selectBackupFolder != null) {
            String path = new Preferences(requireContext()).getBackupPath();
            selectBackupFolder.setSummary(path);
        }
    }

    private void performActionAfterPermissionGranted(int requestCode) {
        switch (requestCode) {
            case REQUEST_BACKUP_PERMISSIONS -> performBackup();
            case REQUEST_RESTORE_PERMISSIONS -> performRestore();
            case REQUEST_EXPORT_PERMISSIONS -> performExport();
            case REQUEST_IMPORT_PERMISSIONS -> performImport();
            default -> {
                // Not a permission request that requires an action
            }
        }
    }

    private void performBackup() {
        mViewModel.runBackup(
                fileName -> {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), getString(R.string.toast_backup_succeeded, fileName), Toast.LENGTH_SHORT).show());
                    }
                },
                message -> {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), R.string.alert_backup_failed, Toast.LENGTH_SHORT).show());
                    }
                }
        );
    }

    private void performRestore() {
        mOpenFileLauncher.launch(new String[]{"*/*"});
    }

    private void performRestore(Uri uri) {
        Context context = requireContext();
        new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle(R.string.alert_restore_title)
                .setMessage(R.string.alert_restore_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> mViewModel.runRestore(
                        uri,
                        () -> {
                            if (isAdded()) {
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(context, R.string.toast_restore_succeeded, Toast.LENGTH_SHORT).show();
                                    AutuManduApplication.recreateAllActivities();
                                    requireActivity().finish();
                                });
                            }
                        },
                        message -> {
                            if (isAdded()) {
                                requireActivity().runOnUiThread(() ->
                                        Toast.makeText(context, R.string.alert_restore_failed, Toast.LENGTH_SHORT).show());
                            }
                        }
                ))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void performExport() {
        mViewModel.runExportCSV(
                () -> {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), R.string.toast_export_csv_succeeded, Toast.LENGTH_SHORT).show());
                    }
                },
                message -> {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), R.string.alert_export_csv_failed, Toast.LENGTH_SHORT).show());
                    }
                }
        );
    }

    private void performImport() {
        Context context = requireContext();
        if (mViewModel.getCsvExportImport().anyExportFileExist()) {
            new androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle(R.string.alert_import_csv_title)
                    .setMessage(R.string.alert_import_csv_message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> mViewModel.runImportCSV(
                            () -> {
                                if (isAdded()) {
                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(context, R.string.toast_import_csv_succeeded, Toast.LENGTH_SHORT).show();
                                        AutuManduApplication.recreateAllActivities();
                                        requireActivity().finish();
                                    });
                                }
                            },
                            message -> {
                                if (isAdded()) {
                                    requireActivity().runOnUiThread(() ->
                                            Toast.makeText(context, R.string.alert_import_csv_failed, Toast.LENGTH_SHORT).show());
                                }
                            }
                    ))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {
            Toast.makeText(context, R.string.pref_summary_import_csv_no_data, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSyncPreference() {
        Preference sync = findPreference("sync_setup");
        if (sync == null) return;

        android.accounts.Account account = SyncManager.getCurrentSyncAccount(requireContext());
        if (account != null) {
            AbstractSyncProvider syncProvider = SyncProviders.getSyncProviderByAccount(requireContext(), account);
            if (syncProvider != null) {
                sync.setIcon(syncProvider.getIcon());
            }
            sync.setSummary(account.name);
            sync.setOnPreferenceClickListener(preference -> {
                AbstractSyncProvider provider = SyncProviders.getSyncProviderByAccount(requireContext(), account);
                String providerName = provider != null ? provider.getName() : "";

                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle(providerName)
                        .setMessage(account.name)
                        .setPositiveButton(R.string.menu_synchronize, (dialog, which) -> SyncManager.runSyncOnce(requireContext()))
                        .setNeutralButton(R.string.menu_remove_account, (dialog, which) -> {
                            android.accounts.AccountManager.get(requireContext()).removeAccount(account, null, null, null);
                            updateSyncPreference();
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                return true;
            });
        } else {
            sync.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_null));
            sync.setSummary(R.string.pref_summary_sync_setup);
            sync.setOnPreferenceClickListener(mSetupSync);
        }
    }

    private Preference.OnPreferenceChangeListener createOnChangeListenerToAskForStorageAccess() {
        return (preference, o) -> {
            if (Boolean.TRUE.equals(o) && android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                            PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(requireActivity(), R.string.toast_need_storage_permission, Toast.LENGTH_LONG).show();
                }

                mPendingPermissionRequestCode = REQUEST_AUTO_BACKUP_PERMISSIONS;
                mRequestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                return false;
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
}
