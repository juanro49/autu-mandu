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
import android.os.Bundle;
import android.text.TextUtils;
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

import org.juanro.autumandu.DistanceEntryMode;
import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.PriceEntryMode;
import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.adapter.CarArrayAdapter;
import org.juanro.autumandu.gui.dialog.DatePickerDialogFragment;
import org.juanro.autumandu.gui.dialog.TimePickerDialogFragment;
import org.juanro.autumandu.gui.util.DateTimeInput;
import org.juanro.autumandu.gui.util.RefuelingValidator;
import org.juanro.autumandu.model.entity.FuelType;
import org.juanro.autumandu.model.entity.Station;
import org.juanro.autumandu.util.reminder.ReminderWorker;
import org.juanro.autumandu.viewmodel.RefuelingDetailViewModel;

import java.util.Date;
import java.util.List;

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

    private DistanceEntryMode mDistanceEntryMode;
    private PriceEntryMode mPriceEntryMode;

    private org.juanro.autumandu.viewmodel.RefuelingDetailViewModel mViewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(org.juanro.autumandu.viewmodel.RefuelingDetailViewModel.class);
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
            edtVolume.setText(priceData.volume);
            edtPrice.setText(priceData.price);

            updateMileageInputWarningVisibility();
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
        mViewModel.getFuelTypes().observe(getViewLifecycleOwner(), fuelTypes ->
            spnFuelType.setAdapter(new FuelTypeArrayAdapter(requireContext(), fuelTypes)));

        mViewModel.getStations().observe(getViewLifecycleOwner(), stations ->
            spnStation.setAdapter(new StationArrayAdapter(requireContext(), stations)));
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
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // Not used
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


    @Override
    protected boolean validate() {
        final Preferences prefs = new Preferences(requireContext());
        return RefuelingValidator.validate(prefs.getPriceEntryMode(), edtMileage, edtVolume, edtPrice);
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
                (float) getDoubleFromEditText(edtVolume),
                (float) getDoubleFromEditText(edtPrice),
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

        mViewModel.validateMileage(mileage, carId, date, mDistanceEntryMode, showWarning ->
                requireActivity().runOnUiThread(() -> {
                    if (isAdded()) {
                        txtMileageWarning.setVisibility(Boolean.TRUE.equals(showWarning) ? View.VISIBLE : View.GONE);
                    }
                }));
    }
}
