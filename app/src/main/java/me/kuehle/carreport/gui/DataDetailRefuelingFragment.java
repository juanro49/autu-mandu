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

import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import com.activeandroid.Model;

import java.util.Date;
import java.util.List;

import me.kuehle.carreport.DistanceEntryMode;
import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.FuelTank;
import me.kuehle.carreport.db.FuelType;
import me.kuehle.carreport.db.Refueling;
import me.kuehle.carreport.gui.dialog.SupportDatePickerDialogFragment.SupportDatePickerDialogFragmentListener;
import me.kuehle.carreport.gui.dialog.SupportTimePickerDialogFragment.SupportTimePickerDialogFragmentListener;
import me.kuehle.carreport.gui.util.AbstractFormFieldValidator;
import me.kuehle.carreport.gui.util.DateTimeInput;
import me.kuehle.carreport.gui.util.FormFieldGreaterZeroValidator;
import me.kuehle.carreport.gui.util.FormValidator;

public class DataDetailRefuelingFragment extends AbstractDataDetailFragment
        implements SupportDatePickerDialogFragmentListener,
        SupportTimePickerDialogFragmentListener {
    private class FuelTypeHolder {
        public FuelTank tank;
        public FuelType type;

        public FuelTypeHolder(FuelTank tank, FuelType type) {
            this.tank = tank;
            this.type = type;
        }
    }

    private static final int PICK_DATE_REQUEST_CODE = 0;
    private static final int PICK_TIME_REQUEST_CODE = 1;

    public static DataDetailRefuelingFragment newInstance(long id, boolean allowCancel) {
        DataDetailRefuelingFragment f = new DataDetailRefuelingFragment();

        Bundle args = new Bundle();
        args.putLong(AbstractDataDetailFragment.EXTRA_ID, id);
        args.putBoolean(AbstractDataDetailFragment.EXTRA_ALLOW_CANCEL, allowCancel);
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

    private List<Car> cars;
    private SparseArray<FuelTypeHolder> fuelTypePositionIDMap;

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

            long selectCar = getArguments().getLong(EXTRA_CAR_ID);
            if (selectCar == 0) {
                selectCar = prefs.getDefaultCar();
            }
            for (int pos = 0; pos < cars.size(); pos++) {
                if (cars.get(pos).id == selectCar) {
                    spnCar.setSelection(pos);
                }
            }
        } else {
            Refueling refueling = (Refueling) editItem;

            edtDate.setDate(refueling.date);
            edtTime.setDate(refueling.date);
            edtVolume.setText(String.valueOf(refueling.volume));
            chkPartial.setChecked(refueling.partial);
            edtPrice.setText(String.valueOf(refueling.price));
            edtNote.setText(refueling.note);

            for (int pos = 0; pos < cars.size(); pos++) {
                if (cars.get(pos).id.equals(refueling.fuelTank.car.id)) {
                    spnCar.setSelection(pos);
                }
            }

            Refueling previousRefueling = getPreviousRefueling();
            if (getDistanceEntryMode() == DistanceEntryMode.TRIP
                    && previousRefueling != null) {
                edtMileage.setText(String.valueOf(refueling.mileage
                        - previousRefueling.mileage));
            } else {
                edtMileage.setText(String.valueOf(refueling.mileage));
            }
        }
    }

    @Override
    protected int getAlertDeleteMessage() {
        return R.string.alert_delete_refueling_message;
    }

    @Override
    protected Model getEditItem(long id) {
        return Refueling.load(Refueling.class, id);
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
        addUnitToHint(edtVolume, prefs.getUnitVolume());
        addUnitToHint(edtPrice, prefs.getUnitCurrency());

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
                        edtMileage.setHint(mode.nameResourceId);
                        addUnitToHint(edtMileage, prefs.getUnitDistance());
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

            edtMileage.setHint(getDistanceEntryMode().nameResourceId);
            addUnitToHint(edtMileage, prefs.getUnitDistance());

            // When hiding the distance mode spinner, we also have to adjust the layout params
            // for the mileage edit text, because it depends on the spinner.
            spnDistanceEntryMode.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) edtMileage
                    .getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        }

        // Car
        ArrayAdapter<String> carAdapter = new ArrayAdapter<>(
                getActivity(), android.R.layout.simple_spinner_dropdown_item);
        cars = Car.getAll();
        for (Car car : cars) {
            carAdapter.add(car.name);
        }

        spnCar.setAdapter(carAdapter);

        spnCar.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView,
                                       View selectedItemView, int position, long id) {
                Car car = cars.get(position);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        getActivity(),
                        android.R.layout.simple_spinner_dropdown_item);
                fuelTypePositionIDMap = new SparseArray<>();

                List<FuelTank> fuelTanks = car.fuelTanks();
                for (FuelTank fuelTank : fuelTanks) {
                    List<FuelType> fuelTypes = fuelTank.fuelTypes();
                    for (FuelType fuelType : fuelTypes) {
                        adapter.add(String.format("%s (%s)", fuelType.name,
                                fuelTank.name));
                        fuelTypePositionIDMap.append(adapter.getCount() - 1,
                                new FuelTypeHolder(fuelTank, fuelType));
                    }
                }

                spnFuelType.setAdapter(adapter);

                if (isInEditMode()) {
                    Refueling refueling = (Refueling) editItem;
                    boolean matchFound = false;
                    for (int i = 0; i < fuelTypePositionIDMap.size(); i++) {
                        FuelTypeHolder holder = fuelTypePositionIDMap
                                .valueAt(i);
                        if (refueling.fuelType.equals(holder.type)
                                && refueling.fuelTank.equals(holder.tank)) {
                            spnFuelType.setSelection(fuelTypePositionIDMap
                                    .keyAt(i));
                            matchFound = true;
                        }
                    }

                    if (!matchFound) {
                        adapter.add(String.format("%s (%s)",
                                refueling.fuelType.name,
                                refueling.fuelTank.name));
                        fuelTypePositionIDMap.append(adapter.getCount() - 1,
                                new FuelTypeHolder(refueling.fuelTank,
                                        refueling.fuelType));
                        spnFuelType.setSelection(adapter.getCount() - 1);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
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
                    Refueling previousRefueling = getPreviousRefueling();
                    Refueling nextRefueling = getNextRefueling();

                    return !((previousRefueling != null && previousRefueling.mileage >= mileage)
                            || (nextRefueling != null && nextRefueling.mileage <= mileage));
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
                    Refueling previousRefueling = getPreviousRefueling();
                    Refueling nextRefueling = getNextRefueling();

                    return !(previousRefueling != null && nextRefueling != null
                            && previousRefueling.mileage + mileage >= nextRefueling.mileage);
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
        Date date = DateTimeInput.getDateTime(edtDate.getDate(), edtTime.getDate());
        int mileage = getIntegerFromEditText(edtMileage, 0);
        float volume = (float) getDoubleFromEditText(edtVolume, 0);
        boolean partial = chkPartial.isChecked();
        float price = (float) getDoubleFromEditText(edtPrice, 0);
        FuelTypeHolder holder = fuelTypePositionIDMap.get(spnFuelType
                .getSelectedItemPosition());
        String note = edtNote.getText().toString().trim();

        Refueling previousRefueling = getPreviousRefueling();
        if (getDistanceEntryMode() == DistanceEntryMode.TRIP
                && previousRefueling != null) {
            mileage += previousRefueling.mileage;
        }

        if (!isInEditMode()) {
            new Refueling(date, mileage, volume, price, partial, note,
                    holder.type, holder.tank).save();
        } else {
            Refueling refueling = (Refueling) editItem;
            refueling.date = date;
            refueling.mileage = mileage;
            refueling.volume = volume;
            refueling.price = price;
            refueling.partial = partial;
            refueling.note = note;
            refueling.fuelType = holder.type;
            refueling.fuelTank = holder.tank;
            refueling.save();
        }
    }

    private DistanceEntryMode getDistanceEntryMode() {
        if (spnDistanceEntryMode.getSelectedItemPosition() == 0) {
            return DistanceEntryMode.TRIP;
        } else {
            return DistanceEntryMode.TOTAL;
        }
    }

    private Refueling getPreviousRefueling() {
        Car car = cars.get(spnCar.getSelectedItemPosition());
        Date date = DateTimeInput.getDateTime(edtDate.getDate(), edtTime.getDate());

        return Refueling.getPrevious(car, date);
    }

    private Refueling getNextRefueling() {
        Car car = cars.get(spnCar.getSelectedItemPosition());
        Date date = DateTimeInput.getDateTime(edtDate.getDate(), edtTime.getDate());

        return Refueling.getNext(car, date);
    }
}