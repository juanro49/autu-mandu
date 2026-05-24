/*
 * Copyright 2026 Juanro49
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

package org.juanro.autumandu.gui.dialog;

import static org.juanro.autumandu.AutuManduApplication.recreateAllActivities;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import org.juanro.autumandu.R;
import org.juanro.autumandu.util.backup.CSVExportImport;
import org.juanro.autumandu.viewmodel.BackupViewModel;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GeneralCSVImportDialogFragment extends DialogFragment {
    public static final String TAG = "GeneralCSVImportDialog";

    private Spinner spnTable;
    private TextView txtPreviewTitle;
    private TextView txtPreviewContent;

    private BackupViewModel viewModel;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(BackupViewModel.class);

        View view = getLayoutInflater().inflate(R.layout.fragment_general_csv_import, null);

        txtPreviewTitle = view.findViewById(R.id.txt_preview_title);
        txtPreviewContent = view.findViewById(R.id.txt_preview_content);
        spnTable = view.findViewById(R.id.spn_table);

        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btn_import).setOnClickListener(v -> startImport());

        setupTableSpinner();

        return new AlertDialog.Builder(requireContext())
                .setTitle(R.string.alert_import_csv_title)
                .setView(view)
                .create();
    }

    private void setupTableSpinner() {
        String[] tables = CSVExportImport.getTableNames();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, tables);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnTable.setAdapter(adapter);

        spnTable.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                showPreview(tables[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void showPreview(String tableName) {
        List<Map<String, String>> preview = viewModel.getCsvExportImport().previewTable(tableName, 3);
        if (preview.isEmpty()) {
            txtPreviewTitle.setVisibility(View.GONE);
            txtPreviewContent.setVisibility(View.GONE);
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (Map<String, String> row : preview) {
            sb.append(row.toString()).append("\n\n");
        }

        txtPreviewTitle.setVisibility(View.VISIBLE);
        txtPreviewContent.setVisibility(View.VISIBLE);
        txtPreviewContent.setText(sb.toString());
    }

    private void startImport() {
        Context context = requireContext();
        viewModel.runImportCSV(
                result -> {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            final String toastMessage = String.format(Locale.getDefault(), getString(R.string.toast_import_csv_general_result), result.getSuccessCount(), result.getFailedCount())
                                    + (result.getFailedCount() > 0 ? "\n\n" + result.getErrorSummary() : "");

                            Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show();
                            recreateAllActivities();
                            requireActivity().finish();
                            dismiss();
                        });
                    }
                },
                error -> {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(context, R.string.alert_import_csv_failed, Toast.LENGTH_SHORT).show());
                    }
                }
        );
    }
}
