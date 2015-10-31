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

import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

import java.util.Date;

import me.kuehle.carreport.DistanceEntryMode;
import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.data.query.FuelTypeQueries;
import me.kuehle.carreport.data.query.RefuelingQueries;
import me.kuehle.carreport.gui.dialog.SupportDatePickerDialogFragment.SupportDatePickerDialogFragmentListener;
import me.kuehle.carreport.gui.dialog.SupportTimePickerDialogFragment.SupportTimePickerDialogFragmentListener;
import me.kuehle.carreport.gui.util.AbstractFormFieldValidator;
import me.kuehle.carreport.gui.util.DateTimeInput;
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
    private EditText edtVolume;
    private CheckBox chkPartial;
    private EditText edtPrice;
    private Spinner spnDistanceEntryMode;
    private Spinner spnFuelType;
    private EditText edtNote;
    private Spinner spnCar;

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
            edtVolume.setText(String.valueOf(refueling.getVolume()));
            chkPartial.setChecked(refueling.getPartial());
            edtPrice.setText(String.valueOf(refueling.getPrice()));
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

            if (getDistanceEntryMode() == DistanceEntryMode.TRIP) {
                RefuelingCursor previousRefueling = getPreviousRefueling();
                if (previousRefueling.moveToNext()) {
                    edtMileage.setText(String.valueOf(refueling.getMileage() - previousRefueling.getMileage()));
                } else {
                    edtMileage.setText(String.valueOf(refueling.getMileage() - refueling.getCarInitialMileage()));
                }
            } else {
                edtMileage.setText(String.valueOf(refueling.getMileage()));
            }
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
    protected int getToastDeletedMessage() {
        return R.string.toast_refueling_deleted;
    }

    @Override
    protected int getToastSavedMessage() {
        return R.string.toast_refueling_saved;
    }

    @Override
    protected void initFields(Bundle savedInstanceState, View v) {
        final Preferences prefs = new Preferences(getActivity());

        edtDate = new DateTimeInput((EditText) v.findViewById(R.id.edt_date),
                DateTimeInput.Mode.DATE);
        edtTime = new DateTimeInput((EditText) v.findViewById(R.id.edt_time),
                DateTimeInput.Mode.TIME);
        edtMileage = (EditText) v.findViewById(R.id.edt_mileage);
        TextInputLayout edtMileageInputLayout = (TextInputLayout) v.findViewById(R.id.edt_mileage_input_layout);
        edtVolume = (EditText) v.findViewById(R.id.edt_volume);
        chkPartial = (CheckBox) v.findViewById(R.id.chk_partial);
        edtPrice = (EditText) v.findViewById(R.id.edt_price);
        spnDistanceEntryMode = (Spinner) v.findViewById(R.id.spn_distance_entry_mode);
        spnFuelType = (Spinner) v.findViewById(R.id.spn_fuel_type);
        edtNote = (EditText) v.findViewById(R.id.edt_note);
        spnCar = (Spinner) v.findViewById(R.id.spn_car);

        // Date and time
        edtDate.applyOnClickListener(DataDetailRefuelingFragment.this, PICK_DATE_REQUEST_CODE,
                getFragmentManager());
        edtTime.applyOnClickListener(DataDetailRefuelingFragment.this, PICK_TIME_REQUEST_CODE,
                getFragmentManager());

        // Units
        addUnitToHint(edtVolume, R.string.hint_volume, prefs.getUnitVolume());
        addUnitToHint(edtPrice, R.string.hint_price, prefs.getUnitCurrency());

        // Distance entry mode
        ArrayAdapter<String> distanceEntryModeAdapter = new ArrayAdapter<>(
                getActivity(), android.R.layout.simple_spinner_dropdown_item);
        distanceEntryModeAdapter.add(getString(DistanceEntryMode.TRIP.nameResourceId));
        distanceEntryModeAdapter.add(getString(DistanceEntryMode.TOTAL.nameResourceId));
        spnDistanceEntryMode.setAdapter(distanceEntryModeAdapter);
        spnDistanceEntryMode
                .setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parentView,
                                               View selectedItemView, int position, long id) {
                        DistanceEntryMode mode = getDistanceEntryMode();
                        addUnitToHint(edtMileage, mode.nameResourceId, prefs.getUnitDistance());
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parentView) {
                    }
                });

        if (prefs.getDistanceEntryMode() != DistanceEntryMode.SHOW_SELECTOR) {
            if (prefs.getDistanceEntryMode() == DistanceEntryMode.TRIP) {
                spnDistanceEntryMode.setSelection(0);
            } else if (prefs.getDistanceEntryMode() == DistanceEntryMode.TOTAL) {
                spnDistanceEntryMode.setSelection(1);
            }

            addUnitToHint(edtMileage, getDistanceEntryMode().nameResourceId, prefs.getUnitDistance());

            // When hiding the distance mode spinner, we also have to adjust the layout params
            // for the mileage edit text, because it depends on the spinner.
            spnDistanceEntryMode.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) edtMileageInputLayout
                    .getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
            }
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
        FormValidator validator = new FormValidator();

        validator.add(new FormFieldGreaterZeroValidator(edtMileage));
        validator.add(new FormFieldGreaterZeroValidator(edtVolume));
        validator.add(new FormFieldGreaterZeroValidator(edtPrice));

        // Check if entered mileage is between the mileage of the
        // previous and next refueling.
        if (getDistanceEntryMode() == DistanceEntryMode.TOTAL) {
            validator.add(new AbstractFormFieldValidator(edtMileage) {
                @Override
                protected boolean isValid() {
                    int mileage = getIntegerFromEditText(edtMileage, 0);
                    RefuelingCursor previousRefueling = getPreviousRefueling();
                    RefuelingCursor nextRefueling = getNextRefueling();

                    return !((previousRefueling.moveToNext() && previousRefueling.getMileage() >= mileage)
                            || (nextRefueling.moveToNext() && nextRefueling.getMileage() <= mileage));
                }

                @Override
                protected int getMessage() {
                    return R.string.validate_error_mileage_out_of_range_total;
                }
            });
        } else {
            validator.add(new AbstractFormFieldValidator(edtMileage) {
                @Override
                protected boolean isValid() {
                    int mileage = getIntegerFromEditText(edtMileage, 0);
                    RefuelingCursor previousRefueling = getPreviousRefueling();
                    RefuelingCursor nextRefueling = getNextRefueling();

                    return !(previousRefueling.moveToNext() && nextRefueling.moveToNext()
                            && previousRefueling.getMileage() + mileage >= nextRefueling.getMileage());
                }

                @Override
                protected int getMessage() {
                    return R.string.validate_error_mileage_out_of_range_trip;
                }
            });
        }

        return validator.validate();
    }

    @Override
    protected void save() {
        int mileage = getIntegerFromEditText(edtMileage, 0);
        RefuelingCursor previousRefueling = getPreviousRefueling();
        if (getDistanceEntryMode() == DistanceEntryMode.TRIP && previousRefueling.moveToNext()) {
            mileage += previousRefueling.getMileage();
        }

        RefuelingContentValues values = new RefuelingContentValues();
        values.putDate(DateTimeInput.getDateTime(edtDate.getDate(), edtTime.getDate()));
        values.putMileage(mileage);
        values.putVolume((float) getDoubleFromEditText(edtVolume, 0));
        values.putPartial(chkPartial.isChecked());
        values.putPrice((float) getDoubleFromEditText(edtPrice, 0));
        values.putNote(edtNote.getText().toString().trim());
        values.putFuelTypeId(spnFuelType.getSelectedItemId());
        values.putCarId(spnCar.getSelectedItemId());

        if (isInEditMode()) {
            RefuelingSelection where = new RefuelingSelection().id(mId);
            values.update(getActivity().getContentResolver(), where);
        } else {
            values.insert(getActivity().getContentResolver());
        }
    }

    @Override
    protected void delete() {
        new RefuelingSelection().id(mId).delete(getActivity().getContentResolver());
    }

    private DistanceEntryMode getDistanceEntryMode() {
        if (spnDistanceEntryMode.getSelectedItemPosition() == 0) {
            return DistanceEntryMode.TRIP;
        } else {
            return DistanceEntryMode.TOTAL;
        }
    }

    private RefuelingCursor getPreviousRefueling() {
        Date date = DateTimeInput.getDateTime(edtDate.getDate(), edtTime.getDate());

        return RefuelingQueries.getPrevious(getActivity(), spnCar.getSelectedItemId(), date);
    }

    private RefuelingCursor getNextRefueling() {
        Date date = DateTimeInput.getDateTime(edtDate.getDate(), edtTime.getDate());

        return RefuelingQueries.getNext(getActivity(), spnCar.getSelectedItemId(), date);
    }
}