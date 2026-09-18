/*
 * Copyright 2012 Jan Kühle
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
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.juanro.autumandu.DistanceEntryMode;
import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.PriceEntryMode;
import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.adapter.CarArrayAdapter;
import org.juanro.autumandu.gui.dialog.DatePickerDialogFragment;
import org.juanro.autumandu.gui.dialog.TimePickerDialogFragment;
import org.juanro.autumandu.gui.util.DateTimeInput;
import org.juanro.autumandu.gui.util.RefuelingValidator;
import org.juanro.autumandu.model.entity.FuelCategory;
import org.juanro.autumandu.model.entity.FuelType;
import org.juanro.autumandu.model.entity.Station;
import org.juanro.autumandu.model.entity.Tank;
import org.juanro.autumandu.model.entity.Trip;
import org.juanro.autumandu.util.reminder.ReminderWorker;
import org.juanro.autumandu.viewmodel.RefuelingDetailViewModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class DataDetailRefuelingFragment extends AbstractDataDetailFragment {
    private static final int PICK_DATE_REQUEST_CODE = 0;
    private static final int PICK_TIME_REQUEST_CODE = 1;

    public static DataDetailRefuelingFragment newInstance(long id) {
        DataDetailRefuelingFragment f = new DataDetailRefuelingFragment();

        Bundle args = new Bundle();
        args.putLong(AbstractDataDetailFragment.EXTRA_ID, id);
        f.setArguments(args);

        return f;
    }

    private DateTimeInput edtDate;
    private DateTimeInput edtTime;
    private EditText edtMileage;
    private TextView txtMileageWarning;
    private EditText edtVolume;
    private CheckBox chkPartial;
    private EditText edtPrice;
    private Spinner spnFuelType;
    private Spinner spnStation;
    private EditText edtNote;
    private Spinner spnCar;

    private Spinner spnTank;
    private TextView txtLabelTank;
    private View btnToggleLevels;
    private EditText edtStartLevel;
    private EditText edtEndLevel;
    private TextView txtPrecisionWarning;

    private TextView txtSectionLinkedTrips;
    private RecyclerView lstLinkedTrips;

    private DistanceEntryMode mDistanceEntryMode;
    private PriceEntryMode mPriceEntryMode;

    private RefuelingDetailViewModel mViewModel;

    private List<Tank> mAvailableTanks = new ArrayList<>();
    private boolean mUpdatingFromLevels = false;
    private boolean mLevelsExpanded = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(RefuelingDetailViewModel.class);
        mViewModel.setRefuelingId(mId);
    }

    @Override
    protected void fillFields(Bundle savedInstanceState, View v) {
        if (!isInEditMode()) {
            setupNewRefuelingFields();
        } else {
            setupEditRefuelingFields();
        }
    }

    private void setupNewRefuelingFields() {
        edtDate.setDate(new Date());
        edtTime.setDate(new Date());

        long selectCarId = getArguments() != null ? getArguments().getLong(EXTRA_CAR_ID) : 0;
        if (selectCarId == 0) {
            selectCarId = new Preferences(requireContext()).getDefaultCar();
        }

        mViewModel.setCarIdForDefaults(selectCarId);

        mViewModel.getMostUsedFuelType().observe(getViewLifecycleOwner(), mostUsedFuelType -> {
            if (mostUsedFuelType != null) {
                selectSpinnerItemById(spnFuelType, mostUsedFuelType.getId());
            }
        });

        mViewModel.getMostUsedStation().observe(getViewLifecycleOwner(), mostUsedStation -> {
            if (mostUsedStation != null) {
                selectSpinnerItemById(spnStation, mostUsedStation.getId());
            }
        });
    }

    private void setupEditRefuelingFields() {
        mViewModel.getRefueling().observe(getViewLifecycleOwner(), refueling -> {
            if (refueling == null) return;

            mViewModel.getDisplayMileage(refueling, mDistanceEntryMode, mileage ->
                    requireActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            edtMileage.setText(String.valueOf(mileage));
                        }
                    }));

            edtDate.setDate(refueling.date());
            edtTime.setDate(refueling.date());
            chkPartial.setChecked(refueling.partial());
            edtNote.setText(refueling.note());

            selectSpinnerItemById(spnFuelType, refueling.fuelTypeId());
            selectSpinnerItemById(spnStation, refueling.stationId());
            selectSpinnerItemById(spnCar, refueling.carId());

            var priceData = mViewModel.getPriceEntryData(refueling, mPriceEntryMode);
            edtVolume.setText(priceData.volume());
            edtPrice.setText(priceData.price());

            if (refueling.startLevel() > 0 || refueling.endLevel() > 0) {
                edtStartLevel.setText(String.valueOf(refueling.startLevel()));
                edtEndLevel.setText(String.valueOf(refueling.endLevel()));
            }

            updateLevelVisibility();

            // Tank selection will be handled in Tank observer when list is loaded
            updateMileageInputWarningVisibility();
        });

        mViewModel.getLinkedTrips().observe(getViewLifecycleOwner(), trips -> {
            if (trips != null && !trips.isEmpty()) {
                txtSectionLinkedTrips.setVisibility(View.VISIBLE);
                lstLinkedTrips.setVisibility(View.VISIBLE);
                lstLinkedTrips.setAdapter(new LinkedTripAdapter(trips));
            } else {
                txtSectionLinkedTrips.setVisibility(View.GONE);
                lstLinkedTrips.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected int getAlertDeleteMessage() {
        return R.string.alert_delete_refueling_message;
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_data_detail_refueling;
    }

    @Override
    protected int getTitleForEdit() {
        return R.string.title_edit_refueling;
    }

    @Override
    protected int getTitleForNew() {
        return R.string.title_add_refueling;
    }

    @Override
    protected void initFields(Bundle savedInstanceState, View v) {
        final Preferences prefs = new Preferences(requireContext());

        initViewReferences(v);
        setupDateTimePickers();
        setupMileageValidation(prefs);
        setupPriceEntryMode(prefs);
        setupSpinners();
        setupCarSpinner();
        setupLevelInputs();
        setupLearningSuggestion();
    }

    private void initViewReferences(View v) {
        edtDate = new DateTimeInput(v.findViewById(R.id.edt_date),
                DateTimeInput.Mode.DATE);
        edtTime = new DateTimeInput(v.findViewById(R.id.edt_time),
                DateTimeInput.Mode.TIME);
        edtMileage = v.findViewById(R.id.edt_mileage);
        txtMileageWarning = v.findViewById(R.id.txt_mileage_input_warning);
        edtVolume = v.findViewById(R.id.edt_volume);
        chkPartial = v.findViewById(R.id.chk_partial);
        edtPrice = v.findViewById(R.id.edt_price);
        spnFuelType = v.findViewById(R.id.spn_fuel_type);
        spnStation = v.findViewById(R.id.spn_station);
        edtNote = v.findViewById(R.id.edt_note);
        spnCar = v.findViewById(R.id.spn_car);

        spnTank = v.findViewById(R.id.spn_tank);
        txtLabelTank = v.findViewById(R.id.txt_label_tank);
        btnToggleLevels = v.findViewById(R.id.btn_toggle_levels);
        edtStartLevel = v.findViewById(R.id.edt_start_level);
        edtEndLevel = v.findViewById(R.id.edt_end_level);
        txtPrecisionWarning = v.findViewById(R.id.txt_precision_warning);

        btnToggleLevels.setOnClickListener(view -> {
            mLevelsExpanded = true;
            updateLevelVisibility();
        });

        txtSectionLinkedTrips = v.findViewById(R.id.txt_section_linked_trips);
        lstLinkedTrips = v.findViewById(R.id.lst_linked_trips);
        lstLinkedTrips.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    private void setupDateTimePickers() {
        getParentFragmentManager().setFragmentResultListener(DatePickerDialogFragment.REQUEST_KEY, getViewLifecycleOwner(), (requestKey, result) -> {
            int requestCode = result.getInt(DatePickerDialogFragment.RESULT_REQUEST_CODE);
            Date date = new Date(result.getLong(DatePickerDialogFragment.RESULT_DATE));
            if (requestCode == PICK_DATE_REQUEST_CODE) {
                edtDate.setDate(date);
            }
        });
        getParentFragmentManager().setFragmentResultListener(TimePickerDialogFragment.REQUEST_KEY, getViewLifecycleOwner(), (requestKey, result) -> {
            int requestCode = result.getInt(TimePickerDialogFragment.RESULT_REQUEST_CODE);
            Date date = new Date(result.getLong(TimePickerDialogFragment.RESULT_TIME));
            if (requestCode == PICK_TIME_REQUEST_CODE) {
                edtTime.setDate(date);
            }
        });

        edtDate.applyOnClickListener(PICK_DATE_REQUEST_CODE,
                getParentFragmentManager());
        edtTime.applyOnClickListener(PICK_TIME_REQUEST_CODE,
                getParentFragmentManager());
    }

    private void setupMileageValidation(Preferences prefs) {
        mDistanceEntryMode = prefs.getDistanceEntryMode();
        addUnitToHint(edtMileage, mDistanceEntryMode.getNameResourceId(), prefs.getUnitDistance());

        txtMileageWarning.setVisibility(View.GONE);
        txtMileageWarning.setText(mDistanceEntryMode == DistanceEntryMode.TOTAL
                ? R.string.validate_error_mileage_out_of_range_total
                : R.string.validate_error_mileage_out_of_range_trip);
        edtMileage.setOnFocusChangeListener((v1, hasFocus) -> {
            if (!hasFocus) {
                updateMileageInputWarningVisibility();
            }
        });
    }

    private void setupPriceEntryMode(Preferences prefs) {
        mPriceEntryMode = prefs.getPriceEntryMode();
        String pricePerUnit = String.format("%s/%s", prefs.getUnitCurrency(), prefs.getUnitVolume());

        switch (mPriceEntryMode) {
            case TOTAL_AND_VOLUME -> {
                addUnitToHint(edtVolume, R.string.hint_volume, prefs.getUnitVolume());
                addUnitToHint(edtPrice, R.string.hint_price_total, prefs.getUnitCurrency());
            }
            case PER_UNIT_AND_TOTAL -> {
                addUnitToHint(edtVolume, R.string.hint_price_per_unit, pricePerUnit);
                addUnitToHint(edtPrice, R.string.hint_price_total, prefs.getUnitCurrency());
            }
            case PER_UNIT_AND_VOLUME -> {
                addUnitToHint(edtVolume, R.string.hint_volume, prefs.getUnitVolume());
                addUnitToHint(edtPrice, R.string.hint_price_per_unit, pricePerUnit);
            }
        }
    }

    private void setupSpinners() {
        mViewModel.getFuelTypes().observe(getViewLifecycleOwner(), fuelTypes -> {
            spnFuelType.setAdapter(new FuelTypeArrayAdapter(requireContext(), fuelTypes));

            spnFuelType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    FuelType selected = (FuelType) parent.getItemAtPosition(position);
                    if (selected != null) {
                        FuelCategory category = FuelCategory.fromKey(selected.getCategory());
                        updateVolumeHint(category);
                        updateTankVisibility(category);
                    }
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {
                }
            });
        });

        mViewModel.getStations().observe(getViewLifecycleOwner(), stations ->
            spnStation.setAdapter(new StationArrayAdapter(requireContext(), stations)));

        spnTank.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateLevelVisibility();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void updateTankVisibility(FuelCategory category) {
        List<Tank> matchingTanks = new ArrayList<>();
        for (Tank tank : mAvailableTanks) {
            if (tank.getFuelCategory().equals(category.getKey())) {
                matchingTanks.add(tank);
            }
        }

        if (matchingTanks.isEmpty()) {
            txtLabelTank.setVisibility(View.GONE);
            spnTank.setVisibility(View.GONE);
        } else if (matchingTanks.size() == 1 && !category.equals(FuelCategory.ADDITIVES)) {
            // Hide if only one tank and not additives (too noisy)
            txtLabelTank.setVisibility(View.GONE);
            spnTank.setVisibility(View.GONE);
            spnTank.setAdapter(new TankArrayAdapter(requireContext(), matchingTanks));
            spnTank.setSelection(0);
        } else {
            txtLabelTank.setVisibility(View.VISIBLE);
            spnTank.setVisibility(View.VISIBLE);
            spnTank.setAdapter(new TankArrayAdapter(requireContext(), matchingTanks));
        }

        updateLevelVisibility();
    }

    private void updateLevelVisibility() {
        Tank selectedTank = (Tank) spnTank.getSelectedItem();
        if (selectedTank == null) {
            btnToggleLevels.setVisibility(View.GONE);
            setFieldsVisibility(View.GONE);
            return;
        }

        boolean isElectricOrGas = selectedTank.getFuelCategory().equals(FuelCategory.ELECTRICITY.getKey()) ||
                selectedTank.getFuelCategory().equals(FuelCategory.GAS.getKey());
        boolean hasData = !TextUtils.isEmpty(edtStartLevel.getText()) || !TextUtils.isEmpty(edtEndLevel.getText());

        if (!isElectricOrGas) {
            // Gasoline/Diesel: Hide everything unless there is already data (e.g. from a manual DB edit)
            btnToggleLevels.setVisibility(View.GONE);
            setFieldsVisibility(hasData ? View.VISIBLE : View.GONE);
        } else {
            // Electric/Gas: Show dropdown to expand
            if (mLevelsExpanded || hasData) {
                btnToggleLevels.setVisibility(View.GONE);
                setFieldsVisibility(View.VISIBLE);
            } else {
                btnToggleLevels.setVisibility(View.VISIBLE);
                setFieldsVisibility(View.GONE);
            }
        }
    }

    private void setFieldsVisibility(int visibility) {
        View v = getView();
        if (v != null) {
            v.findViewById(R.id.edt_start_level_input_layout).setVisibility(visibility);
            v.findViewById(R.id.edt_end_level_input_layout).setVisibility(visibility);
            txtPrecisionWarning.setVisibility(visibility);
        }
    }

    private void updateVolumeHint(FuelCategory category) {
        String volumeUnit = category.getVolumeUnit(requireContext());
        String pricePerUnit = String.format("%s/%s", new Preferences(requireContext()).getUnitCurrency(), volumeUnit);

        switch (mPriceEntryMode) {
            case TOTAL_AND_VOLUME -> addUnitToHint(edtVolume, R.string.hint_volume, volumeUnit);
            case PER_UNIT_AND_TOTAL -> addUnitToHint(edtVolume, R.string.hint_price_per_unit, pricePerUnit);
            case PER_UNIT_AND_VOLUME -> {
                addUnitToHint(edtVolume, R.string.hint_volume, volumeUnit);
                addUnitToHint(edtPrice, R.string.hint_price_per_unit, pricePerUnit);
            }
        }
    }

    private static class FuelTypeArrayAdapter extends ArrayAdapter<FuelType> {
        public FuelTypeArrayAdapter(Context context, List<FuelType> items) {
            super(context, android.R.layout.simple_spinner_dropdown_item, items);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            TextView v = (TextView) super.getView(position, convertView, parent);
            FuelType item = getItem(position);
            if (item != null) {
                v.setText(item.getName());
            }
            return v;
        }

        @NonNull
        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            TextView v = (TextView) super.getDropDownView(position, convertView, parent);
            FuelType item = getItem(position);
            if (item != null) {
                v.setText(item.getName());
            }
            return v;
        }

        @Override
        public long getItemId(int position) {
            FuelType item = getItem(position);
            return item != null ? item.getId() : -1;
        }
    }

    private static class StationArrayAdapter extends ArrayAdapter<Station> {
        public StationArrayAdapter(Context context, List<Station> items) {
            super(context, android.R.layout.simple_spinner_dropdown_item, items);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            TextView v = (TextView) super.getView(position, convertView, parent);
            Station item = getItem(position);
            if (item != null) {
                v.setText(item.getName());
            }
            return v;
        }

        @NonNull
        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            TextView v = (TextView) super.getDropDownView(position, convertView, parent);
            Station item = getItem(position);
            if (item != null) {
                v.setText(item.getName());
            }
            return v;
        }

        @Override
        public long getItemId(int position) {
            Station item = getItem(position);
            return item != null ? item.getId() : -1;
        }
    }

    private static class TankArrayAdapter extends ArrayAdapter<Tank> {
        public TankArrayAdapter(Context context, List<Tank> items) {
            super(context, android.R.layout.simple_spinner_dropdown_item, items);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            TextView v = (TextView) super.getView(position, convertView, parent);
            Tank item = getItem(position);
            if (item != null) {
                v.setText(item.getName() != null ? item.getName() : item.getFuelCategory());
            }
            return v;
        }

        @NonNull
        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            TextView v = (TextView) super.getDropDownView(position, convertView, parent);
            Tank item = getItem(position);
            if (item != null) {
                v.setText(item.getName() != null ? item.getName() : item.getFuelCategory());
            }
            return v;
        }

        @Override
        public long getItemId(int position) {
            Tank item = getItem(position);
            return item != null ? item.getId() : -1;
        }
    }

    private void setupCarSpinner() {
        mViewModel.getCars().observe(getViewLifecycleOwner(), cars -> {
            spnCar.setAdapter(new CarArrayAdapter(requireContext(), cars));
            updateInitialCarSelection();
        });

        spnCar.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (!isInEditMode()) {
                    mViewModel.setCarIdForDefaults(id);
                }
                updateMileageInputWarningVisibility();
                observeTanks(id);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // Not used
            }
        });
    }

    private void observeTanks(long carId) {
        mViewModel.getTanksForCar(carId).removeObservers(getViewLifecycleOwner());
        mViewModel.getTanksForCar(carId).observe(getViewLifecycleOwner(), tanks -> {
            mAvailableTanks = tanks;
            FuelType selectedType = (FuelType) spnFuelType.getSelectedItem();
            if (selectedType != null) {
                updateTankVisibility(FuelCategory.fromKey(selectedType.getCategory()));
            }

            if (isInEditMode()) {
                mViewModel.getRefueling().observe(getViewLifecycleOwner(), refueling -> {
                    if (refueling != null) {
                        selectSpinnerItemById(spnTank, refueling.tankId());
                    }
                });
            }
        });
    }

    private void updateInitialCarSelection() {
        if (!isInEditMode()) {
            long selectCarId = getArguments() != null ? getArguments().getLong(EXTRA_CAR_ID) : 0;
            if (selectCarId == 0) {
                Preferences currentPrefs = new Preferences(requireContext());
                selectCarId = currentPrefs.getDefaultCar();
            }

            selectSpinnerItemById(spnCar, selectCarId);
        }
    }

    private void setupLevelInputs() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!mUpdatingFromLevels) {
                    calculateVolumeFromLevels();
                }
            }
        };

        edtStartLevel.addTextChangedListener(watcher);
        edtEndLevel.addTextChangedListener(watcher);

        edtVolume.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mUpdatingFromLevels = true; // Block auto-update while manual entry
            } else if (TextUtils.isEmpty(edtVolume.getText())) {
                mUpdatingFromLevels = false;
            }
        });
    }

    private void calculateVolumeFromLevels() {
        Tank tank = (Tank) spnTank.getSelectedItem();
        if (tank == null || tank.getCapacity() <= 0) return;

        double start = getDoubleFromEditText(edtStartLevel);
        double end = getDoubleFromEditText(edtEndLevel);

        if (end > start) {
            mUpdatingFromLevels = true;
            float volume = (float) (tank.getCapacity() * (end - start) / 100.0);
            edtVolume.setText(String.format(Locale.US, "%.2f", volume));
            mUpdatingFromLevels = false;
        }
    }

    private void setupLearningSuggestion() {
        mViewModel.getLearnedCapacity().observe(getViewLifecycleOwner(), capacity -> {
            Tank tank = (Tank) spnTank.getSelectedItem();
            if (tank == null) return;

            FuelType type = (FuelType) spnFuelType.getSelectedItem();
            String unit = type != null ? FuelCategory.fromKey(type.getCategory()).getVolumeUnit(requireContext()) : "";

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.learn_capacity_title)
                    .setMessage(getString(R.string.learn_capacity_message, capacity, unit))
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        mViewModel.updateTankCapacity(tank.getId(), capacity);
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        });
    }


    @Override
    protected boolean validate() {
        final Preferences prefs = new Preferences(requireContext());
        boolean valid = RefuelingValidator.validate(prefs.getPriceEntryMode(), edtMileage, edtVolume, edtPrice);

        // Capacity validation
        Tank selectedTank = (Tank) spnTank.getSelectedItem();
        if (selectedTank != null && selectedTank.getCapacity() > 0) {
            double volume = getDoubleFromEditText(edtVolume);
            if (volume > selectedTank.getCapacity()) {
                FuelType selectedType = (FuelType) spnFuelType.getSelectedItem();
                String unit = selectedType != null ? FuelCategory.fromKey(selectedType.getCategory()).getVolumeUnit(requireContext()) : "";
                edtVolume.setError(getString(R.string.validate_error_volume_exceeds_capacity, selectedTank.getCapacity(), unit));
                valid = false;
            }
        }

        // Ensure a tank is selected to avoid FOREIGN KEY exception
        if (selectedTank == null) {
            FuelType selectedType = (FuelType) spnFuelType.getSelectedItem();
            String category = selectedType != null ? FuelCategory.fromKey(selectedType.getCategory()).getName(requireContext()) : "";
            new MaterialAlertDialogBuilder(requireContext())
                    .setMessage(getString(R.string.validate_error_no_tank_for_category, category))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            valid = false;
        }

        return valid;
    }

    @Override
    protected void saveAsync() {
        mViewModel.save(new RefuelingDetailViewModel.SaveParams(
                isInEditMode() ? mId : null,
                getIntegerFromEditText(edtMileage, 0),
                DateTimeInput.getDateTime(edtDate.getDate(), edtTime.getDate()),
                chkPartial.isChecked(),
                edtNote.getText().toString().trim(),
                spnFuelType.getSelectedItemId(),
                spnStation.getSelectedItemId(),
                spnCar.getSelectedItemId(),
                spnTank.getSelectedItemId(),
                (float) getDoubleFromEditText(edtVolume),
                (float) getDoubleFromEditText(edtPrice),
                (float) getDoubleFromEditText(edtStartLevel),
                (float) getDoubleFromEditText(edtEndLevel),
                mDistanceEntryMode,
                mPriceEntryMode,
                () -> requireActivity().runOnUiThread(() -> {
                    if (isAdded()) {
                        ReminderWorker.enqueueUpdate(requireContext());
                        mOnItemActionListener.onItemSavedAsync(mId);
                    }
                })
        ));
    }

    @Override
    protected void deleteAsync() {
        mViewModel.delete(mId, () ->
            requireActivity().runOnUiThread(() -> {
                if (isAdded()) {
                    mOnItemActionListener.onItemDeletedAsync();
                }
            }));
    }

    @Override
    protected long save() {
        return 0; // Not used anymore as we use saveAsync
    }

    @Override
    protected void delete() {
        // Not used anymore as we use deleteAsync
    }

    private void updateMileageInputWarningVisibility() {
        if (TextUtils.isEmpty(edtMileage.getText())) {
            txtMileageWarning.setVisibility(View.GONE);
            return;
        }

        final int mileage = getIntegerFromEditText(edtMileage, 0);
        final long carId = spnCar.getSelectedItemId();
        final Date date = DateTimeInput.getDateTime(edtDate.getDate(), edtTime.getDate());

        // In a real implementation we should get the tankId here
        // For simplicity of validation, we'll use carId if tank is not yet selected
        long tankId = spnTank.getSelectedItemId();
        if (tankId == -1) tankId = carId; // Fallback for validation logic in VM

        mViewModel.validateMileage(mileage, tankId, date, mDistanceEntryMode, showWarning ->
                requireActivity().runOnUiThread(() -> {
                    if (isAdded()) {
                        txtMileageWarning.setVisibility(Boolean.TRUE.equals(showWarning) ? View.VISIBLE : View.GONE);
                    }
                }));
    }

    private class LinkedTripAdapter extends RecyclerView.Adapter<LinkedTripAdapter.ViewHolder> {
        private final List<Trip> mTrips;
        private final String mUnitDistance;

        public LinkedTripAdapter(List<Trip> trips) {
            mTrips = trips;
            mUnitDistance = new Preferences(requireContext()).getUnitDistance();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Trip trip = mTrips.get(position);
            holder.text1.setText(String.format("%s -> %s", trip.getRouteTarget(), trip.getPurpose()));
            holder.text2.setText(String.format(Locale.getDefault(), "%d %s", trip.getTotalDistance(), mUnitDistance));

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), org.juanro.autumandu.gui.DataDetailActivity.class);
                intent.putExtra(org.juanro.autumandu.gui.DataDetailActivity.EXTRA_EDIT, org.juanro.autumandu.gui.DataDetailActivity.EXTRA_EDIT_TRIP);
                intent.putExtra(AbstractDataDetailFragment.EXTRA_ID, trip.getId());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return mTrips.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1;
            TextView text2;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}
