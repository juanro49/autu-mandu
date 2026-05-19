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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.core.util.Consumer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputLayout;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.adapter.CarArrayAdapter;
import org.juanro.autumandu.gui.adapter.RefuelingArrayAdapter;
import org.juanro.autumandu.gui.dialog.DatePickerDialogFragment;
import org.juanro.autumandu.gui.dialog.TimePickerDialogFragment;
import org.juanro.autumandu.gui.util.DateTimeInput;
import org.juanro.autumandu.model.entity.Trip;
import org.juanro.autumandu.model.entity.TripPrefab;
import org.juanro.autumandu.model.dto.RefuelingWithDetails;
import org.juanro.autumandu.viewmodel.TripDetailViewModel;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DataDetailTripFragment extends AbstractDataDetailFragment {
    private static final String TAG = "DataDetailTripFragment";
    private static final String LOCATION_COORDS_FORMAT = "%.4f, %.4f";
    private static final int PICK_DATE_REQUEST_CODE = 0;
    private static final int PICK_START_TIME_REQUEST_CODE = 1;
    private static final int PICK_DATE_END_REQUEST_CODE = 2;
    private static final int PICK_END_TIME_REQUEST_CODE = 3;

    public static DataDetailTripFragment newInstance(long id) {
        DataDetailTripFragment f = new DataDetailTripFragment();
        Bundle args = new Bundle();
        args.putLong(AbstractDataDetailFragment.EXTRA_ID, id);
        f.setArguments(args);
        return f;
    }

    private DateTimeInput edtDate;
    private DateTimeInput edtDateEnd;
    private DateTimeInput edtStartTime;
    private DateTimeInput edtEndTime;
    private AutoCompleteTextView edtRoute;
    private AutoCompleteTextView edtPurpose;
    private EditText edtOdometerStart;
    private EditText edtOdometerEnd;
    private EditText edtKmBusiness;
    private EditText edtKmPrivate;
    private EditText edtKmHomeWork;
    private EditText edtCostFuel;
    private EditText edtCostOther;
    private EditText edtVisitedCompanies;
    private AutoCompleteTextView edtDriver;
    private EditText edtOccupants;
    private EditText edtCargo;
    private EditText edtNote;
    private Spinner spnCar;
    private Spinner spnRefueling;
    private View btnShowRefueling;
    private TextView txtStartLocation;
    private TextView txtEndLocation;

    private TripDetailViewModel mViewModel;
    private boolean mAutoCalculated = false;
    private LocationManager mLocationManager;

    private final ActivityResultLauncher<String[]> mRequestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                if (Boolean.TRUE.equals(result.getOrDefault(android.Manifest.permission.ACCESS_FINE_LOCATION, false))
                        || Boolean.TRUE.equals(result.getOrDefault(android.Manifest.permission.ACCESS_COARSE_LOCATION, false))) {
                    Toast.makeText(requireContext(), R.string.trip_location_fetching, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), R.string.trip_location_permission_denied, Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(TripDetailViewModel.class);
        mViewModel.setTripId(mId);
        mLocationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    protected void fillFields(Bundle savedInstanceState, View v) {
        if (!isInEditMode()) {
            setupNewTripFields();
        } else {
            setupEditTripFields();
        }
    }

    private void setupNewTripFields() {
        Date now = new Date();
        edtDate.setDate(now);
        edtDateEnd.setDate(now);
        edtStartTime.setDate(now);
        edtEndTime.setDate(now);

        long carIdFromArgs = getArguments() != null ? getArguments().getLong(EXTRA_CAR_ID) : 0;
        long selectCarId = carIdFromArgs != 0 ? carIdFromArgs : new Preferences(requireContext()).getDefaultCar();
        selectSpinnerItemById(spnCar, selectCarId);

        updateOdometerFromLastTrip(selectCarId);
    }

    private void setupEditTripFields() {
        mViewModel.getTrip().observe(getViewLifecycleOwner(), trip -> {
            if (trip == null) {
                return;
            }

            edtDate.setDate(Date.from(trip.getDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            edtDateEnd.setDate(Date.from(trip.getDateEnd().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            edtStartTime.setDate(Date.from(trip.getTimeStart().atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant()));
            edtEndTime.setDate(Date.from(trip.getTimeEnd().atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant()));

            edtRoute.setText(trip.getRouteTarget());
            edtPurpose.setText(trip.getPurpose());
            edtOdometerStart.setText(String.valueOf(trip.getKmStart()));
            edtOdometerEnd.setText(String.valueOf(trip.getKmEnd()));

            edtKmBusiness.setText(String.valueOf(trip.getKmBusiness()));
            edtKmPrivate.setText(String.valueOf(trip.getKmPrivate()));
            edtKmHomeWork.setText(String.valueOf(trip.getKmHomeWork()));

            edtCostFuel.setText(trip.getFuelCost() != null ? String.valueOf(trip.getFuelCost()) : "");
            edtCostOther.setText(trip.getOtherCostsAmount() != null ? String.valueOf(trip.getOtherCostsAmount()) : "");

            edtVisitedCompanies.setText(trip.getCompaniesVisited());
            edtDriver.setText(trip.getDriver());
            edtOccupants.setText(trip.getOccupants() != null ? String.valueOf(trip.getOccupants()) : "");
            edtCargo.setText(trip.getCargo());
            edtNote.setText(trip.getOtherCostsDescription());

            selectSpinnerItemById(spnCar, trip.getCarId());
            if (trip.getRefuelingId() != null) {
                selectRefuelingItemById(trip.getRefuelingId());
            }
        });
    }

    private void selectRefuelingItemById(long refuelingId) {
        int count = spnRefueling.getCount();
        for (int i = 0; i < count; i++) {
            if (spnRefueling.getItemIdAtPosition(i) == refuelingId) {
                spnRefueling.setSelection(i);
                break;
            }
        }
    }

    private void updateOdometerFromLastTrip(long carId) {
        mViewModel.getLastKmEnd(carId, kmEnd -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> edtOdometerStart.setText(String.valueOf(kmEnd)));
            }
        });
    }

    @Override
    protected int getAlertDeleteMessage() {
        return R.string.alert_delete_trip_message;
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_data_detail_trip;
    }

    @Override
    protected int getTitleForEdit() {
        return R.string.title_edit_trip;
    }

    @Override
    protected int getTitleForNew() {
        return R.string.title_add_trip;
    }

    @Override
    protected void initFields(Bundle savedInstanceState, View v) {
        initViewReferences(v);
        setupDateTimePickers();
        setupCarSpinner();
        setupRefuelingSpinner();
        setupAutocomplete();
        setupOdometerListeners();
        setupValidationListeners();
    }

    private void initViewReferences(View v) {
        edtDate = new DateTimeInput(v.findViewById(R.id.edt_date), DateTimeInput.Mode.DATE);
        edtDateEnd = new DateTimeInput(v.findViewById(R.id.edt_date_end), DateTimeInput.Mode.DATE);
        edtStartTime = new DateTimeInput(v.findViewById(R.id.edt_start_time), DateTimeInput.Mode.TIME);
        edtEndTime = new DateTimeInput(v.findViewById(R.id.edt_end_time), DateTimeInput.Mode.TIME);

        edtRoute = v.findViewById(R.id.edt_route);
        edtPurpose = v.findViewById(R.id.edt_purpose);
        edtOdometerStart = v.findViewById(R.id.edt_odometer_start);
        edtOdometerEnd = v.findViewById(R.id.edt_odometer_end);

        edtKmBusiness = v.findViewById(R.id.edt_km_business);
        edtKmPrivate = v.findViewById(R.id.edt_km_private);
        edtKmHomeWork = v.findViewById(R.id.edt_km_home_work);

        edtCostFuel = v.findViewById(R.id.edt_cost_fuel);
        edtCostOther = v.findViewById(R.id.edt_cost_other);

        edtVisitedCompanies = v.findViewById(R.id.edt_visited_companies);
        edtDriver = v.findViewById(R.id.edt_driver);
        edtOccupants = v.findViewById(R.id.edt_occupants);
        edtCargo = v.findViewById(R.id.edt_cargo);
        edtNote = v.findViewById(R.id.edt_note);

        spnCar = v.findViewById(R.id.spn_car);
        spnRefueling = v.findViewById(R.id.spn_refueling);
        btnShowRefueling = v.findViewById(R.id.btn_show_refueling);

        TextInputLayout edtRouteInputLayout = v.findViewById(R.id.edt_route_input_layout);
        txtStartLocation = v.findViewById(R.id.txt_start_location);
        txtEndLocation = v.findViewById(R.id.txt_end_location);

        v.findViewById(R.id.btn_get_start_location).setOnClickListener(view -> fetchLocation(txtStartLocation));
        v.findViewById(R.id.btn_get_end_location).setOnClickListener(view -> fetchLocation(txtEndLocation));

        edtRouteInputLayout.setEndIconOnClickListener(view -> fetchLocationAndFillRoute());
    }

    private void fetchLocation(TextView target) {
        target.setText(R.string.trip_location_fetching);
        fetchCurrentLocation(location -> {
            if (location != null) {
                updateLocationText(target, location);
            } else {
                target.setText(R.string.trip_location_error);
            }
        });
    }

    private void fetchLocationAndFillRoute() {
        Context context = getContext();
        if (context == null || !isAdded()) return;
        Toast.makeText(context, R.string.trip_location_fetching, Toast.LENGTH_SHORT).show();

        fetchCurrentLocation(location -> {
            if (location != null) {
                reverseGeocode(location, address -> {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> edtRoute.setText(address));
                    }
                });
            } else {
                Toast.makeText(requireContext(), R.string.trip_location_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchCurrentLocation(java.util.function.Consumer<Location> callback) {
        Context context = getContext();
        if (context == null || !isAdded()) return;

        boolean fineGranted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!fineGranted && !coarseGranted) {
            mRequestPermissionLauncher.launch(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION});
            return;
        }

        if (!LocationManagerCompat.isLocationEnabled(mLocationManager)) {
            Toast.makeText(context, R.string.trip_location_disabled, Toast.LENGTH_SHORT).show();
            return;
        }

        getCurrentLocationCompat(LocationManager.NETWORK_PROVIDER, location -> {
            if (location != null) {
                callback.accept(location);
            } else {
                getCurrentLocationCompat(LocationManager.GPS_PROVIDER, locationGps -> {
                    if (locationGps != null) {
                        callback.accept(locationGps);
                    } else {
                        callback.accept(getLastKnownLocation());
                    }
                });
            }
        });
    }

    private void getCurrentLocationCompat(String provider, Consumer<Location> callback) {
        Context context = getContext();
        if (context == null || !isAdded()) return;
        try {
            LocationManagerCompat.getCurrentLocation(
                    mLocationManager,
                    provider,
                    (CancellationSignal) null,
                    ContextCompat.getMainExecutor(context),
                    location -> {
                        if (isAdded()) {
                            callback.accept(location);
                        }
                    }
            );
        } catch (SecurityException ignored) {
            callback.accept(null);
        }
    }

    private Location getLastKnownLocation() {
        Context context = getContext();
        if (context == null || ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        try {
            Location lastKnown = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (lastKnown == null) {
                lastKnown = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if (lastKnown == null) {
                lastKnown = mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            }
            return lastKnown;
        } catch (SecurityException ignored) {
            return null;
        }
    }

    private void updateLocationText(TextView target, Location location) {
        reverseGeocode(location, address -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> target.setText(address));
            }
        });
    }

    private void reverseGeocode(Location location, java.util.function.Consumer<String> callback) {
        Context context = getContext();
        if (context == null || !isAdded()) {
            return;
        }
        if (!Geocoder.isPresent()) {
            callback.accept(String.format(Locale.getDefault(), LOCATION_COORDS_FORMAT, location.getLatitude(), location.getLongitude()));
            return;
        }
        new Thread(() -> {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    String display = addr.getAddressLine(0);
                    callback.accept(display);
                } else {
                    callback.accept(String.format(Locale.getDefault(), LOCATION_COORDS_FORMAT, location.getLatitude(), location.getLongitude()));
                }
            } catch (IOException e) {
                Log.e(TAG, "Geocoder error", e);
                callback.accept(String.format(Locale.getDefault(), LOCATION_COORDS_FORMAT, location.getLatitude(), location.getLongitude()));
            }
        }).start();
    }

    private void setupDateTimePickers() {
        getParentFragmentManager().setFragmentResultListener(DatePickerDialogFragment.REQUEST_KEY, getViewLifecycleOwner(), (requestKey, result) -> {
            int requestCode = result.getInt(DatePickerDialogFragment.RESULT_REQUEST_CODE);
            Date date = new Date(result.getLong(DatePickerDialogFragment.RESULT_DATE));
            if (requestCode == PICK_DATE_REQUEST_CODE) {
                edtDate.setDate(date);
            } else if (requestCode == PICK_DATE_END_REQUEST_CODE) {
                edtDateEnd.setDate(date);
            }
        });
        getParentFragmentManager().setFragmentResultListener(TimePickerDialogFragment.REQUEST_KEY, getViewLifecycleOwner(), (requestKey, result) -> {
            int requestCode = result.getInt(TimePickerDialogFragment.RESULT_REQUEST_CODE);
            Date date = new Date(result.getLong(TimePickerDialogFragment.RESULT_TIME));
            if (requestCode == PICK_START_TIME_REQUEST_CODE) {
                edtStartTime.setDate(date);
            } else if (requestCode == PICK_END_TIME_REQUEST_CODE) {
                edtEndTime.setDate(date);
            }
        });

        edtDate.applyOnClickListener(PICK_DATE_REQUEST_CODE, getParentFragmentManager());
        edtDateEnd.applyOnClickListener(PICK_DATE_END_REQUEST_CODE, getParentFragmentManager());
        edtStartTime.applyOnClickListener(PICK_START_TIME_REQUEST_CODE, getParentFragmentManager());
        edtEndTime.applyOnClickListener(PICK_END_TIME_REQUEST_CODE, getParentFragmentManager());
    }

    private void setupCarSpinner() {
        mViewModel.getCars().observe(getViewLifecycleOwner(), cars -> {
            CarArrayAdapter adapter = new CarArrayAdapter(requireContext(), cars);
            spnCar.setAdapter(adapter);

            if (isInEditMode()) {
                mViewModel.getTrip().observe(getViewLifecycleOwner(), trip -> {
                    if (trip != null) {
                        selectSpinnerItemById(spnCar, trip.getCarId());
                    }
                });
            } else {
                long defaultCarId = new Preferences(requireContext()).getDefaultCar();
                selectSpinnerItemById(spnCar, defaultCarId);
            }
        });

        spnCar.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateRefuelingSpinner();
                updateAutocompleteForCar(id);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setupRefuelingSpinner() {
        spnRefueling.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                btnShowRefueling.setVisibility(id != -1 ? View.VISIBLE : View.GONE);
                if (id != -1) {
                    RefuelingWithDetails refueling = (RefuelingWithDetails) parent.getItemAtPosition(position);
                    if (refueling != null && (edtCostFuel.getText().toString().isEmpty() || mAutoCalculated)) {
                        edtCostFuel.setText(String.format(Locale.US, "%.2f", refueling.price()));
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        btnShowRefueling.setOnClickListener(v -> {
            long refuelingId = spnRefueling.getSelectedItemId();
            if (refuelingId != -1) {
                Intent intent = new Intent(requireContext(), org.juanro.autumandu.gui.DataDetailActivity.class);
                intent.putExtra(org.juanro.autumandu.gui.DataDetailActivity.EXTRA_EDIT, org.juanro.autumandu.gui.DataDetailActivity.EXTRA_EDIT_REFUELING);
                intent.putExtra(AbstractDataDetailFragment.EXTRA_ID, refuelingId);
                startActivity(intent);
            }
        });
    }

    private void updateRefuelingSpinner() {
        long carId = spnCar.getSelectedItemId();
        if (carId != AdapterView.INVALID_ROW_ID) {
            mViewModel.getRefuelingsForCar(carId).observe(getViewLifecycleOwner(), refuelings -> {
                RefuelingArrayAdapter adapter = new RefuelingArrayAdapter(requireContext(), refuelings);
                spnRefueling.setAdapter(adapter);

                if (isInEditMode()) {
                    mViewModel.getTrip().observe(getViewLifecycleOwner(), trip -> {
                        if (trip != null && trip.getRefuelingId() != null) {
                            selectRefuelingItemById(trip.getRefuelingId());
                        }
                    });
                }
            });
        }
    }

    private void setupAutocomplete() {
        long carId = spnCar.getSelectedItemId();
        if (carId != AdapterView.INVALID_ROW_ID) {
            updateAutocompleteForCar(carId);
        }
    }

    private void updateAutocompleteForCar(long carId) {
        mViewModel.getPrefabsByType(carId, "route").observe(getViewLifecycleOwner(), prefabs -> {
            List<String> values = prefabs.stream().map(TripPrefab::getValue).toList();
            edtRoute.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, values));
        });
        mViewModel.getPrefabsByType(carId, "purpose").observe(getViewLifecycleOwner(), prefabs -> {
            List<String> values = prefabs.stream().map(TripPrefab::getValue).toList();
            edtPurpose.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, values));
        });
        mViewModel.getPrefabsByType(carId, "driver").observe(getViewLifecycleOwner(), prefabs -> {
            List<String> values = prefabs.stream().map(TripPrefab::getValue).toList();
            edtDriver.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, values));
        });
    }

    private void setupOdometerListeners() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed
            }
            @Override
            public void afterTextChanged(Editable s) {
                if (mAutoCalculated) return;
                calculateDistanceAndAutoFill();
            }
        };
        edtOdometerStart.addTextChangedListener(watcher);
        edtOdometerEnd.addTextChangedListener(watcher);
    }

    private void calculateDistanceAndAutoFill() {
        try {
            int start = Integer.parseInt(edtOdometerStart.getText().toString());
            int end = Integer.parseInt(edtOdometerEnd.getText().toString());
            int diff = end - start;
            if (diff >= 0) {
                mAutoCalculated = true;
                // Auto-fill business km by default if all others are empty
                if (edtKmBusiness.getText().toString().isEmpty() && edtKmPrivate.getText().toString().isEmpty() && edtKmHomeWork.getText().toString().isEmpty()) {
                    edtKmBusiness.setText(String.valueOf(diff));
                }
                mAutoCalculated = false;
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "Failed to auto-calculate distance", e);
        }
    }

    private void setupValidationListeners() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed
            }
            @Override
            public void afterTextChanged(Editable s) {
                validate();
            }
        };
        edtKmBusiness.addTextChangedListener(watcher);
        edtKmPrivate.addTextChangedListener(watcher);
        edtKmHomeWork.addTextChangedListener(watcher);
    }

    @Override
    protected boolean validate() {
        boolean valid = true;
        edtOdometerEnd.setError(null);
        edtKmBusiness.setError(null);
        edtEndTime.setError(null);

        // Date/Time validation
        Date startD = edtDate.getDate();
        Date endD = edtDateEnd.getDate();
        Date startT = edtStartTime.getDate();
        Date endT = edtEndTime.getDate();

        if (startD != null && endD != null && startT != null && endT != null) {
            LocalDateTime start = LocalDateTime.of(
                    startD.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                    startT.toInstant().atZone(ZoneId.systemDefault()).toLocalTime());
            LocalDateTime end = LocalDateTime.of(
                    endD.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                    endT.toInstant().atZone(ZoneId.systemDefault()).toLocalTime());

            if (end.isBefore(start)) {
                edtEndTime.setError(getString(R.string.validate_error_end_before_start));
                valid = false;
            }
        }

        int start = getIntegerFromEditText(edtOdometerStart, 0);
        int end = getIntegerFromEditText(edtOdometerEnd, 0);

        if (end < start) {
            edtOdometerEnd.setError(getString(R.string.validate_error_odometer_end_smaller_than_start));
            valid = false;
        }

        int total = end - start;
        int business = getIntegerFromEditText(edtKmBusiness, 0);
        int privateKm = getIntegerFromEditText(edtKmPrivate, 0);
        int homeWork = getIntegerFromEditText(edtKmHomeWork, 0);

        if (total >= 0 && business + privateKm + homeWork != total) {
            String unit = new Preferences(requireContext()).getUnitDistance();
            edtKmBusiness.setError(getString(R.string.validate_error_sum_km_mismatch, total, unit));
            valid = false;
        }

        if (edtRoute.getText().toString().trim().isEmpty()) {
            edtRoute.setError(getString(R.string.validate_error_empty));
            valid = false;
        }

        if (edtPurpose.getText().toString().trim().isEmpty()) {
            edtPurpose.setError(getString(R.string.validate_error_empty));
            valid = false;
        }

        return valid;
    }

    @Override
    protected void saveAsync() {
        Trip trip = new Trip();
        if (isInEditMode()) {
            trip.setId(mId);
        }
        trip.setCarId(spnCar.getSelectedItemId());
        long refuelingId = spnRefueling.getSelectedItemId();
        trip.setRefuelingId(refuelingId != -1 ? refuelingId : null);

        Date date = edtDate.getDate();
        if (date != null) {
            trip.setDate(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        }

        Date dateEnd = edtDateEnd.getDate();
        if (dateEnd != null) {
            trip.setDateEnd(dateEnd.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        }

        Date startTime = edtStartTime.getDate();
        if (startTime != null) {
            trip.setTimeStart(startTime.toInstant().atZone(ZoneId.systemDefault()).toLocalTime());
        }

        Date endTime = edtEndTime.getDate();
        if (endTime != null) {
            trip.setTimeEnd(endTime.toInstant().atZone(ZoneId.systemDefault()).toLocalTime());
        }

        trip.setRouteTarget(edtRoute.getText().toString().trim());
        trip.setPurpose(edtPurpose.getText().toString().trim());
        trip.setKmStart(getIntegerFromEditText(edtOdometerStart, 0));
        trip.setKmEnd(getIntegerFromEditText(edtOdometerEnd, 0));

        trip.setKmBusiness(getIntegerFromEditText(edtKmBusiness, 0));
        trip.setKmPrivate(getIntegerFromEditText(edtKmPrivate, 0));
        trip.setKmHomeWork(getIntegerFromEditText(edtKmHomeWork, 0));

        trip.setFuelCost(getDoubleFromEditText(edtCostFuel));
        trip.setOtherCostsAmount(getDoubleFromEditText(edtCostOther));

        trip.setCompaniesVisited(edtVisitedCompanies.getText().toString().trim());
        trip.setDriver(edtDriver.getText().toString().trim());
        trip.setOccupants(getIntegerFromEditText(edtOccupants, 0));
        trip.setCargo(edtCargo.getText().toString().trim());
        trip.setOtherCostsDescription(edtNote.getText().toString().trim());

        mViewModel.save(trip, () -> requireActivity().runOnUiThread(() -> {
            if (isAdded()) {
                mOnItemActionListener.onItemSavedAsync(mId);
            }
        }));
    }

    @Override
    protected void onCopy() {
        mId = AbstractDataDetailFragment.EXTRA_ID_DEFAULT;
        if (getActivity() != null) {
            requireActivity().invalidateOptionsMenu();
            var actionBar = ((androidx.appcompat.app.AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(getTitleForNew());
            }
        }
    }

    @Override
    protected void deleteAsync() {
        mViewModel.delete(mId, () -> requireActivity().runOnUiThread(() -> {
            if (isAdded()) {
                mOnItemActionListener.onItemDeletedAsync();
            }
        }));
    }

    @Override
    protected long save() { return 0; }

    @Override
    protected void delete() {
        // Handled by deleteAsync
    }
}
