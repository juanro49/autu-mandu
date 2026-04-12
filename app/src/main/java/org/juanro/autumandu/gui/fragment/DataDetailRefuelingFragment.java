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
import org.juanro.autumandu.gui.dialog.DatePickerDialogFragment;
import org.juanro.autumandu.gui.dialog.TimePickerDialogFragment;
import org.juanro.autumandu.gui.util.DateTimeInput;
import org.juanro.autumandu.gui.util.FormFieldGreaterEqualZeroOrEmptyValidator;
import org.juanro.autumandu.gui.util.FormFieldGreaterEqualZeroValidator;
import org.juanro.autumandu.gui.util.FormFieldGreaterZeroValidator;
import org.juanro.autumandu.gui.util.FormValidator;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.FuelType;
import org.juanro.autumandu.model.entity.Refueling;
import org.juanro.autumandu.model.entity.Station;
import org.juanro.autumandu.util.reminder.ReminderWorker;

import java.util.Date;

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
            edtDate.setDate(new Date());
            edtTime.setDate(new Date());

            long selectCarId = getArguments() != null ? getArguments().getLong(EXTRA_CAR_ID) : 0;
            if (selectCarId == 0) {
                Preferences prefs = new Preferences(requireContext());
                selectCarId = prefs.getDefaultCar();
            }

            mViewModel.setCarIdForDefaults(selectCarId);

            mViewModel.getMostUsedFuelType().observe(getViewLifecycleOwner(), mostUsedFuelType -> {
                if (mostUsedFuelType != null) {
                    for (int pos = 0; pos < spnFuelType.getCount(); pos++) {
                        if (spnFuelType.getItemIdAtPosition(pos) == mostUsedFuelType.getId()) {
                            spnFuelType.setSelection(pos);
                            break;
                        }
                    }
                }
            });

            mViewModel.getMostUsedStation().observe(getViewLifecycleOwner(), mostUsedStation -> {
                if (mostUsedStation != null) {
                    for (int pos = 0; pos < spnStation.getCount(); pos++) {
                        if (spnStation.getItemIdAtPosition(pos) == mostUsedStation.getId()) {
                            spnStation.setSelection(pos);
                            break;
                        }
                    }
                }
            });
        } else {
            mViewModel.getRefueling().observe(getViewLifecycleOwner(), refueling -> {
                if (refueling == null) return;

                if (mDistanceEntryMode == DistanceEntryMode.TRIP) {
                    mViewModel.getPreviousRefueling(refueling.carId(), refueling.date(), previousRefueling ->
                        requireActivity().runOnUiThread(() -> {
                            if (isAdded()) {
                                if (previousRefueling != null) {
                                    edtMileage.setText(String.valueOf(refueling.mileage() - previousRefueling.getMileage()));
                                } else {
                                    edtMileage.setText(String.valueOf(refueling.mileage() - refueling.carInitialMileage()));
                                }
                            }
                        }));
                } else {
                    edtMileage.setText(String.valueOf(refueling.mileage()));
                }

                edtDate.setDate(refueling.date());
                edtTime.setDate(refueling.date());
                chkPartial.setChecked(refueling.partial());
                edtNote.setText(refueling.note());

                for (int pos = 0; pos < spnFuelType.getCount(); pos++) {
                    if (spnFuelType.getItemIdAtPosition(pos) == refueling.fuelTypeId()) {
                        spnFuelType.setSelection(pos);
                        break;
                    }
                }

                for (int pos = 0; pos < spnStation.getCount(); pos++) {
                    if (spnStation.getItemIdAtPosition(pos) == refueling.stationId()) {
                        spnStation.setSelection(pos);
                        break;
                    }
                }

                for (int pos = 0; pos < spnCar.getCount(); pos++) {
                    if (spnCar.getItemIdAtPosition(pos) == refueling.carId()) {
                        spnCar.setSelection(pos);
                        break;
                    }
                }

                if (mPriceEntryMode == PriceEntryMode.TOTAL_AND_VOLUME) {
                    edtVolume.setText(String.valueOf(refueling.volume()));
                    if (refueling.price() != 0.0f) {
                        edtPrice.setText(String.valueOf(refueling.price()));
                    }
                } else if (mPriceEntryMode == PriceEntryMode.PER_UNIT_AND_TOTAL) {
                    edtVolume.setText(String.valueOf(refueling.price() / refueling.volume()));
                    edtPrice.setText(String.valueOf(refueling.price()));
                } else if (mPriceEntryMode == PriceEntryMode.PER_UNIT_AND_VOLUME) {
                    edtVolume.setText(String.valueOf(refueling.volume()));
                    if (refueling.price() != 0.0f) {
                        edtPrice.setText(String.valueOf(refueling.price() / refueling.volume()));
                    }
                }

                updateMileageInputWarningVisibility();
            });
        }
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

        // Date and time
        edtDate.applyOnClickListener(PICK_DATE_REQUEST_CODE,
                getParentFragmentManager());
        edtTime.applyOnClickListener(PICK_TIME_REQUEST_CODE,
                getParentFragmentManager());

        // Distance entry mode
        mDistanceEntryMode = prefs.getDistanceEntryMode();
        addUnitToHint(edtMileage, mDistanceEntryMode.getNameResourceId(), prefs.getUnitDistance());

        // Mileage warning
        txtMileageWarning.setVisibility(View.GONE);
        txtMileageWarning.setText(mDistanceEntryMode == DistanceEntryMode.TOTAL
                ? R.string.validate_error_mileage_out_of_range_total
                : R.string.validate_error_mileage_out_of_range_trip);
        edtMileage.setOnFocusChangeListener((v1, hasFocus) -> {
            if (!hasFocus) {
                updateMileageInputWarningVisibility();
            }
        });

        // Price entry mode
        mPriceEntryMode = prefs.getPriceEntryMode();
        String pricePerUnit = String.format("%s/%s", prefs.getUnitCurrency(), prefs.getUnitVolume());
        if (mPriceEntryMode == PriceEntryMode.TOTAL_AND_VOLUME) {
            addUnitToHint(edtVolume, R.string.hint_volume, prefs.getUnitVolume());
            addUnitToHint(edtPrice, R.string.hint_price_total, prefs.getUnitCurrency());
        } else if (mPriceEntryMode == PriceEntryMode.PER_UNIT_AND_TOTAL) {
            addUnitToHint(edtVolume, R.string.hint_price_per_unit, pricePerUnit);
            addUnitToHint(edtPrice, R.string.hint_price_total, prefs.getUnitCurrency());
        } else if (mPriceEntryMode == PriceEntryMode.PER_UNIT_AND_VOLUME) {
            addUnitToHint(edtVolume, R.string.hint_volume, prefs.getUnitVolume());
            addUnitToHint(edtPrice, R.string.hint_price_per_unit, pricePerUnit);
        }

        mViewModel.getFuelTypes().observe(getViewLifecycleOwner(), fuelTypes ->
            spnFuelType.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_dropdown_item, fuelTypes) {
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
            }));

        mViewModel.getStations().observe(getViewLifecycleOwner(), stations ->
            spnStation.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_dropdown_item, stations) {
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
            }));

        mViewModel.getCars().observe(getViewLifecycleOwner(), cars -> {
            spnCar.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_dropdown_item, cars) {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    TextView v = (TextView) super.getView(position, convertView, parent);
                    Car item = getItem(position);
                    if (item != null) {
                        v.setText(item.getName());
                    }
                    return v;
                }

                @NonNull
                @Override
                public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    TextView v = (TextView) super.getDropDownView(position, convertView, parent);
                    Car item = getItem(position);
                    if (item != null) {
                        v.setText(item.getName());
                    }
                    return v;
                }

                @Override
                public long getItemId(int position) {
                    Car item = getItem(position);
                    return item != null ? item.getId() : -1;
                }
            });

            if (!isInEditMode()) {
                long selectCarId = getArguments() != null ? getArguments().getLong(EXTRA_CAR_ID) : 0;
                if (selectCarId == 0) {
                    Preferences currentPrefs = new Preferences(requireContext());
                    selectCarId = currentPrefs.getDefaultCar();
                }

                for (int pos = 0; pos < spnCar.getCount(); pos++) {
                    if (spnCar.getItemIdAtPosition(pos) == selectCarId) {
                        spnCar.setSelection(pos);
                        break;
                    }
                }
            }
        });
    }

    @Override
    protected boolean validate() {
        final Preferences prefs = new Preferences(requireContext());
        FormValidator validator = new FormValidator();

        validator.add(new FormFieldGreaterZeroValidator(edtMileage));
        validator.add(new FormFieldGreaterZeroValidator(edtVolume));
        if (prefs.getPriceEntryMode() == PriceEntryMode.TOTAL_AND_VOLUME ||
                prefs.getPriceEntryMode() == PriceEntryMode.PER_UNIT_AND_VOLUME) {
            validator.add(new FormFieldGreaterEqualZeroOrEmptyValidator(edtPrice));
        } else {
            validator.add(new FormFieldGreaterEqualZeroValidator(edtPrice));
        }

        return validator.validate();
    }

    @Override
    protected void saveAsync() {
        mViewModel.getPreviousRefueling(spnCar.getSelectedItemId(), DateTimeInput.getDateTime(edtDate.getDate(), edtTime.getDate()), previousRefueling -> {
            Refueling refueling;
            if (isInEditMode()) {
                refueling = new Refueling();
                refueling.setId(mId);
            } else {
                refueling = new Refueling();
            }

            int mileage = getIntegerFromEditText(edtMileage, 0);
            if (previousRefueling != null && mDistanceEntryMode == DistanceEntryMode.TRIP) {
                mileage += previousRefueling.getMileage();
            }

            refueling.setDate(DateTimeInput.getDateTime(edtDate.getDate(), edtTime.getDate()));
            refueling.setMileage(mileage);
            refueling.setPartial(chkPartial.isChecked());
            refueling.setNote(edtNote.getText().toString().trim());
            refueling.setFuelTypeId(spnFuelType.getSelectedItemId());
            refueling.setStationId(spnStation.getSelectedItemId());
            refueling.setCarId(spnCar.getSelectedItemId());

            float volume = (float) getDoubleFromEditText(edtVolume);
            float price = (float) getDoubleFromEditText(edtPrice);
            if (mPriceEntryMode == PriceEntryMode.TOTAL_AND_VOLUME) {
                refueling.setVolume(volume);
                refueling.setPrice(price);
            } else if (mPriceEntryMode == PriceEntryMode.PER_UNIT_AND_TOTAL) {
                refueling.setVolume(price / volume);
                refueling.setPrice(price);
            } else if (mPriceEntryMode == PriceEntryMode.PER_UNIT_AND_VOLUME) {
                refueling.setVolume(volume);
                refueling.setPrice(volume * price);
            }

            mViewModel.save(refueling, () ->
                requireActivity().runOnUiThread(() -> {
                    if (isAdded()) {
                        ReminderWorker.enqueueUpdate(requireContext());
                        mOnItemActionListener.onItemSavedAsync(refueling.getId());
                    }
                }));
        });
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

        mViewModel.getPreviousRefueling(carId, date, previousRefueling ->
            mViewModel.getNextRefueling(carId, date, nextRefueling -> {
                boolean showWarning;
                if (mDistanceEntryMode == DistanceEntryMode.TOTAL) {
                    showWarning = (previousRefueling != null && previousRefueling.getMileage() >= mileage) ||
                            (nextRefueling != null && nextRefueling.getMileage() <= mileage);
                } else {
                    showWarning = previousRefueling != null &&
                            nextRefueling != null &&
                            previousRefueling.getMileage() + mileage >= nextRefueling.getMileage();
                }

                requireActivity().runOnUiThread(() -> {
                    if (isAdded()) {
                        txtMileageWarning.setVisibility(showWarning ? View.VISIBLE : View.GONE);
                    }
                });
            }));
    }
}
