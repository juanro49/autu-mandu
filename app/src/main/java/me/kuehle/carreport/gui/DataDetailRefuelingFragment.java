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

package me.kuehle.carreport.gui;

import android.content.ContentUris;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Date;

import me.kuehle.carreport.DistanceEntryMode;
import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.PriceEntryMode;
import me.kuehle.carreport.R;
import me.kuehle.carreport.data.query.FuelTypeQueries;
import me.kuehle.carreport.data.query.RefuelingQueries;
import me.kuehle.carreport.gui.dialog.SupportDatePickerDialogFragment.SupportDatePickerDialogFragmentListener;
import me.kuehle.carreport.gui.dialog.SupportTimePickerDialogFragment.SupportTimePickerDialogFragmentListener;
import me.kuehle.carreport.gui.util.DateTimeInput;
import me.kuehle.carreport.gui.util.FormFieldGreaterEqualZeroOrEmptyValidator;
import me.kuehle.carreport.gui.util.FormFieldGreaterEqualZeroValidator;
import me.kuehle.carreport.gui.util.FormFieldGreaterZeroValidator;
import me.kuehle.carreport.gui.util.FormValidator;
import me.kuehle.carreport.provider.car.CarColumns;
import me.kuehle.carreport.provider.car.CarCursor;
import me.kuehle.carreport.provider.car.CarSelection;
import me.kuehle.carreport.provider.fueltype.FuelTypeColumns;
import me.kuehle.carreport.provider.fueltype.FuelTypeCursor;
import me.kuehle.carreport.provider.fueltype.FuelTypeSelection;
import me.kuehle.carreport.provider.refueling.RefuelingContentValues;
import me.kuehle.carreport.provider.refueling.RefuelingCursor;
import me.kuehle.carreport.provider.refueling.RefuelingSelection;

public class DataDetailRefuelingFragment extends AbstractDataDetailFragment
        implements SupportDatePickerDialogFragmentListener,
        SupportTimePickerDialogFragmentListener {
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
    private EditText edtNote;
    private Spinner spnCar;

    private DistanceEntryMode mDistanceEntryMode;
    private PriceEntryMode mPriceEntryMode;

    @Override
    public void onDialogPositiveClick(int requestCode, Date date) {
        if (requestCode == PICK_DATE_REQUEST_CODE) {
            edtDate.setDate(date);
        } else if (requestCode == PICK_TIME_REQUEST_CODE) {
            edtTime.setDate(date);
        }
    }

    @Override
    protected void fillFields(Bundle savedInstanceState, View v) {
        if (!isInEditMode()) {
            Preferences prefs = new Preferences(getActivity());

            edtDate.setDate(new Date());
            edtTime.setDate(new Date());

            long selectCarId = getArguments().getLong(EXTRA_CAR_ID);
            if (selectCarId == 0) {
                selectCarId = prefs.getDefaultCar();
            }

            for (int pos = 0; pos < spnCar.getCount(); pos++) {
                if (spnCar.getItemIdAtPosition(pos) == selectCarId) {
                    spnCar.setSelection(pos);
                }
            }

            // By default select most often used fuel type for this car.
            long mostUsedFuelTypeId = FuelTypeQueries.getMostUsedId(getActivity(), selectCarId);
            if (mostUsedFuelTypeId > 0) {
                for (int pos = 0; pos < spnFuelType.getCount(); pos++) {
                    if (spnFuelType.getItemIdAtPosition(pos) == mostUsedFuelTypeId) {
                        spnFuelType.setSelection(pos);
                    }
                }
            }
        } else {
            RefuelingCursor refueling = new RefuelingSelection().id(mId).query(getActivity().getContentResolver());
            refueling.moveToNext();

            edtDate.setDate(refueling.getDate());
            edtTime.setDate(refueling.getDate());
            chkPartial.setChecked(refueling.getPartial());
            edtNote.setText(refueling.getNote());

            for (int pos = 0; pos < spnFuelType.getCount(); pos++) {
                if (spnFuelType.getItemIdAtPosition(pos) == refueling.getFuelTypeId()) {
                    spnFuelType.setSelection(pos);
                }
            }

            for (int pos = 0; pos < spnCar.getCount(); pos++) {
                if (spnCar.getItemIdAtPosition(pos) == refueling.getCarId()) {
                    spnCar.setSelection(pos);
                }
            }

            if (mDistanceEntryMode == DistanceEntryMode.TRIP) {
                RefuelingCursor previousRefueling = getPreviousRefueling();
                if (previousRefueling.moveToNext()) {
                    edtMileage.setText(String.valueOf(refueling.getMileage() - previousRefueling.getMileage()));
                } else {
                    edtMileage.setText(String.valueOf(refueling.getMileage() - refueling.getCarInitialMileage()));
                }
            } else {
                edtMileage.setText(String.valueOf(refueling.getMileage()));
            }

            if (mPriceEntryMode == PriceEntryMode.TOTAL_AND_VOLUME) {
                edtVolume.setText(String.valueOf(refueling.getVolume()));
                if (refueling.getPrice() != 0.0f) {
                    edtPrice.setText(String.valueOf(refueling.getPrice()));
                }
            } else if (mPriceEntryMode == PriceEntryMode.PER_UNIT_AND_TOTAL) {
                edtVolume.setText(String.valueOf(refueling.getPrice() / refueling.getVolume()));
                edtPrice.setText(String.valueOf(refueling.getPrice()));
            } else if (mPriceEntryMode == PriceEntryMode.PER_UNIT_AND_VOLUME) {
                edtVolume.setText(String.valueOf(refueling.getVolume()));
                if (refueling.getPrice() != 0.0f) {
                    edtPrice.setText(String.valueOf(refueling.getPrice() / refueling.getVolume()));
                }
            }

            updateMileageInputWarningVisibility();
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
        final Preferences prefs = new Preferences(getActivity());

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
        edtNote = v.findViewById(R.id.edt_note);
        spnCar = v.findViewById(R.id.spn_car);

        // Date and time
        edtDate.applyOnClickListener(DataDetailRefuelingFragment.this, PICK_DATE_REQUEST_CODE,
                getFragmentManager());
        edtTime.applyOnClickListener(DataDetailRefuelingFragment.this, PICK_TIME_REQUEST_CODE,
                getFragmentManager());

        // Distance entry mode
        mDistanceEntryMode = prefs.getDistanceEntryMode();
        addUnitToHint(edtMileage, mDistanceEntryMode.nameResourceId, prefs.getUnitDistance());

        // Mileage warning
        txtMileageWarning.setVisibility(View.GONE);
        txtMileageWarning.setText(mDistanceEntryMode == DistanceEntryMode.TOTAL
                ? R.string.validate_error_mileage_out_of_range_total
                : R.string.validate_error_mileage_out_of_range_trip);
        edtMileage.setOnFocusChangeListener((v1, hasFocus) -> updateMileageInputWarningVisibility());

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

        // Fuel Type
        FuelTypeQueries.ensureAtLeastOne(getActivity());

        FuelTypeCursor fuelType = new FuelTypeSelection().query(getActivity().getContentResolver(),
                null, FuelTypeColumns.NAME + " COLLATE UNICODE");
        spnFuelType.setAdapter(new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_spinner_dropdown_item,
                fuelType, new String[]{FuelTypeColumns.NAME}, new int[]{android.R.id.text1}, 0));

        // Car
        CarCursor car = new CarSelection().query(getActivity().getContentResolver(), null,
                CarColumns.NAME + " COLLATE UNICODE");
        spnCar.setAdapter(new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_spinner_dropdown_item,
                car, new String[]{CarColumns.NAME}, new int[]{android.R.id.text1}, 0));
    }

    @Override
    protected boolean validate() {
        final Preferences prefs = new Preferences(getActivity());
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
    protected long save() {
        RefuelingCursor previousRefueling = getPreviousRefueling();
        boolean hasPrevious = previousRefueling.moveToFirst();

        int mileage = getIntegerFromEditText(edtMileage, 0);
        if (hasPrevious && mDistanceEntryMode == DistanceEntryMode.TRIP) {
            mileage += previousRefueling.getMileage();
        }

        RefuelingContentValues values = new RefuelingContentValues();
        values.putDate(DateTimeInput.getDateTime(edtDate.getDate(), edtTime.getDate()));
        values.putMileage(mileage);
        values.putPartial(chkPartial.isChecked());
        values.putNote(edtNote.getText().toString().trim());
        values.putFuelTypeId(spnFuelType.getSelectedItemId());
        values.putCarId(spnCar.getSelectedItemId());

        float volume = (float) getDoubleFromEditText(edtVolume, 0);
        float price = (float) getDoubleFromEditText(edtPrice, 0);
        if (mPriceEntryMode == PriceEntryMode.TOTAL_AND_VOLUME) {
            values.putVolume(volume);
            values.putPrice(price);
        } else if (mPriceEntryMode == PriceEntryMode.PER_UNIT_AND_TOTAL) {
            values.putVolume(price / volume);
            values.putPrice(price);
        } else if (mPriceEntryMode == PriceEntryMode.PER_UNIT_AND_VOLUME) {
            values.putVolume(volume);
            values.putPrice(volume * price);
        }

        if (isInEditMode()) {
            RefuelingSelection where = new RefuelingSelection().id(mId);
            values.update(getActivity().getContentResolver(), where);
            return mId;
        } else {
            Uri uri = values.insert(getActivity().getContentResolver());
            return ContentUris.parseId(uri);
        }
    }

    @Override
    protected void delete() {
        new RefuelingSelection().id(mId).delete(getActivity().getContentResolver());
    }

    private RefuelingCursor getPreviousRefueling() {
        Date date = DateTimeInput.getDateTime(edtDate.getDate(), edtTime.getDate());

        return RefuelingQueries.getPrevious(getActivity(), spnCar.getSelectedItemId(), date);
    }

    private RefuelingCursor getNextRefueling() {
        Date date = DateTimeInput.getDateTime(edtDate.getDate(), edtTime.getDate());

        return RefuelingQueries.getNext(getActivity(), spnCar.getSelectedItemId(), date);
    }

    private void updateMileageInputWarningVisibility() {
        boolean showWarning;
        if (TextUtils.isEmpty(edtMileage.getText())) {
            showWarning = false;
        } else {
            int mileage = getIntegerFromEditText(edtMileage, 0);
            RefuelingCursor previousRefueling = getPreviousRefueling();
            RefuelingCursor nextRefueling = getNextRefueling();

            if (mDistanceEntryMode == DistanceEntryMode.TOTAL) {
                showWarning = (previousRefueling.moveToNext() && previousRefueling.getMileage() >= mileage) ||
                        (nextRefueling.moveToNext() && nextRefueling.getMileage() <= mileage);
            } else {
                showWarning = previousRefueling.moveToNext() &&
                        nextRefueling.moveToNext() &&
                        previousRefueling.getMileage() + mileage >= nextRefueling.getMileage();
            }
        }

        txtMileageWarning.setVisibility(showWarning ? View.VISIBLE : View.GONE);
    }
}