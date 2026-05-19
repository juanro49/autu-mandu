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

package org.juanro.autumandu.gui.fragment;

import android.os.Bundle;
import android.util.SparseArray;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

import android.net.Uri;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.DataDetailActivity;
import org.juanro.autumandu.gui.dialog.TripImportDialogFragment;
import org.juanro.autumandu.model.dto.TripWithDetails;
import org.juanro.autumandu.model.entity.Trip;
import org.juanro.autumandu.util.backup.CSVTripFormat;
import org.juanro.autumandu.util.backup.TripExporter;
import org.juanro.autumandu.viewmodel.TripViewModel;

public class DataListTripFragment extends AbstractDataListFragment<TripWithDetails> implements MenuProvider {
    private DateTimeFormatter dateFormatter;
    private DateTimeFormatter timeFormatter;
    private String unitDistance;
    private String unitCurrency;

    private TripViewModel viewModel;

    private final ActivityResultLauncher<String> mCreateFileLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/csv"),
            uri -> {
                if (uri != null) {
                    performExport(uri);
                }
            }
    );

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        var context = requireContext();
        dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
        timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);

        var prefs = new Preferences(context);
        unitDistance = prefs.getUnitDistance();
        unitCurrency = prefs.getUnitCurrency();

        viewModel = new ViewModelProvider(this, new TripViewModel.Factory(requireActivity().getApplication())).get(TripViewModel.class);
        viewModel.setCarId(carId);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.trip_list, menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.menu_import_csv) {
            new TripImportDialogFragment().show(getChildFragmentManager(), TripImportDialogFragment.TAG);
            return true;
        } else if (menuItem.getItemId() == R.id.menu_export_csv) {
            mCreateFileLauncher.launch("trips_export.csv");
            return true;
        }
        return false;
    }

    private void performExport(Uri uri) {
        // Retrieve raw Trip list from DB for export to ensure we have all data
        // Must be done in background to avoid blocking the main thread
        var context = requireContext();
        new Thread(() -> {
            List<Trip> trips = viewModel.getTripsForCar(carId);

            requireActivity().runOnUiThread(() -> {
                if (trips == null || trips.isEmpty()) {
                    Toast.makeText(context, "No trips to export", Toast.LENGTH_SHORT).show();
                    return;
                }

                TripExporter exporter = new TripExporter(context);
                if (exporter.exportToCsv(trips, uri, CSVTripFormat.GENERIC)) {
                    Toast.makeText(context, R.string.toast_export_csv_succeeded, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, String.format(getString(R.string.alert_export_csv_failed), "Unknown error"), Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    @Override
    protected LiveData<List<TripWithDetails>> getLiveData() {
        return viewModel.getTrips();
    }

    @Override
    protected int getAlertDeleteManyMessage() {
        // We should add this string to strings.xml if we want it specific
        return R.string.alert_delete_refuelings_message;
    }

    @Override
    protected int getExtraEdit() {
        return DataDetailActivity.EXTRA_EDIT_TRIP;
    }

    @Override
    protected SparseArray<String> getItemData(TripWithDetails trip) {
        var data = new SparseArray<String>(10);

        data.put(R.id.title, trip.routeTarget());
        data.put(R.id.subtitle, trip.purpose());

        String dateTime = String.format("%s %s - %s",
                dateFormatter.format(trip.date()),
                timeFormatter.format(trip.timeStart()),
                timeFormatter.format(trip.timeEnd()));
        data.put(R.id.date, dateTime);

        if (trip.stationName() != null) {
            data.put(R.id.station, trip.stationName());
        } else if (trip.driver() != null && !trip.driver().isEmpty()) {
            data.put(R.id.station, trip.driver());
        }

        data.put(R.id.data1, String.format(Locale.getDefault(), "%d %s", trip.kmEnd(), unitDistance));
        data.put(R.id.data1_calculated, String.format(Locale.getDefault(), "+ %d %s",
                trip.getTotalDistance(), unitDistance));

        data.put(R.id.data2, String.format(Locale.getDefault(), "%.2f %s",
                trip.getTotalCost(), unitCurrency));

        String category;
        if (trip.kmBusiness() > 0) category = getString(R.string.trip_category_business);
        else if (trip.kmHomeWork() > 0) category = getString(R.string.trip_category_home_work);
        else category = getString(R.string.trip_category_private);
        data.put(R.id.data2_calculated, category);

        return data;
    }

    @Override
    protected boolean isMissingData(TripWithDetails trip) {
        return false;
    }

    @Override
    protected long getItemId(TripWithDetails item) {
        return item.id();
    }

    @Override
    protected void deleteItem(long id) {
        // Need a way to get the trip by ID or just delete by ID in ViewModel
        // For now, let's assume we can load it.
        viewModel.deleteTrip(new Trip() {{ setId(id); }}, null);
    }
}
