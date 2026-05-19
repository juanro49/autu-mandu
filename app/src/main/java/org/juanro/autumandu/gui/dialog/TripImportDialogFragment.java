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

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.adapter.CarArrayAdapter;
import org.juanro.autumandu.util.backup.CSVTripFormat;
import org.juanro.autumandu.util.backup.CsvTripImporter;
import org.juanro.autumandu.util.backup.ImportResult;
import org.juanro.autumandu.viewmodel.TripViewModel;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TripImportDialogFragment extends DialogFragment {
    public static final String TAG = "TripImportDialog";

    private Spinner spnCar;
    private Spinner spnFormat;
    private TextView txtFileName;
    private TextView txtPreviewTitle;
    private TextView txtPreviewContent;
    private Button btnImport;

    private Uri selectedUri;
    private CsvTripImporter importer;
    private TripViewModel viewModel;

    private final ActivityResultLauncher<String[]> mOpenFileLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    onFileSelected(uri);
                }
            }
    );

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        importer = new CsvTripImporter(requireContext());
        viewModel = new ViewModelProvider(this, new TripViewModel.Factory(requireActivity().getApplication())).get(TripViewModel.class);

        View view = getLayoutInflater().inflate(R.layout.fragment_trip_import, null);

        txtFileName = view.findViewById(R.id.txt_file_name);
        txtPreviewTitle = view.findViewById(R.id.txt_preview_title);
        txtPreviewContent = view.findViewById(R.id.txt_preview_content);
        spnCar = view.findViewById(R.id.spn_car);
        spnFormat = view.findViewById(R.id.spn_format);
        btnImport = view.findViewById(R.id.btn_import);

        view.findViewById(R.id.btn_select_file).setOnClickListener(v -> mOpenFileLauncher.launch(new String[]{"text/*", "application/csv"}));
        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dismiss());
        btnImport.setOnClickListener(v -> startImport());

        setupCarSpinner();
        setupFormatSpinner();

        return new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();
    }

    private void setupCarSpinner() {
        viewModel.getCars().observe(this, cars -> {
            spnCar.setAdapter(new CarArrayAdapter(requireContext(), cars));

            long defaultCarId = new Preferences(requireContext()).getDefaultCar();
            for (int i = 0; i < spnCar.getCount(); i++) {
                if (spnCar.getItemIdAtPosition(i) == defaultCarId) {
                    spnCar.setSelection(i);
                    break;
                }
            }
        });
    }

    private void setupFormatSpinner() {
        String[] formats = {"Auto Detect", "Generic CSV", "Skoda CSV"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, formats);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnFormat.setAdapter(adapter);
    }

    private void onFileSelected(Uri uri) {
        selectedUri = uri;
        txtFileName.setText(uri.getLastPathSegment());

        CSVTripFormat detected = importer.detectFormat(uri);
        if (detected == CSVTripFormat.SKODA) {
            spnFormat.setSelection(2);
        } else {
            spnFormat.setSelection(1);
        }

        // Leveraging guessMapping and setColumnMapping to improve compatibility with custom CSVs
        Map<String, String> guessedMapping = importer.guessMapping(uri);
        if (!guessedMapping.isEmpty()) {
            importer.setColumnMapping(guessedMapping);
        }

        showPreview(uri);
        btnImport.setEnabled(true);
    }

    private void showPreview(Uri uri) {
        List<Map<String, String>> preview = importer.previewData(uri, 3);
        if (preview.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        for (Map<String, String> row : preview) {
            sb.append(row.toString()).append("\n\n");
        }

        txtPreviewTitle.setVisibility(View.VISIBLE);
        txtPreviewContent.setVisibility(View.VISIBLE);
        txtPreviewContent.setText(sb.toString());
    }

    private void startImport() {
        long carId = spnCar.getSelectedItemId();
        if (carId == -1) {
            Toast.makeText(requireContext(), "Please select a car", Toast.LENGTH_SHORT).show();
            return;
        }

        // Run import in background
        new Thread(() -> {
            ImportResult result = importer.importTrips(selectedUri, carId);
            requireActivity().runOnUiThread(() -> {
                String message = String.format(Locale.getDefault(), getString(R.string.toast_import_csv_result), result.getSuccessCount(), result.getFailedCount());
                if (result.getFailedCount() > 0) {
                    message += "\n\n" + result.getErrorSummary();
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                dismiss();
            });
        }).start();
    }
}
