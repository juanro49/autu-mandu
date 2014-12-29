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
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.activeandroid.Model;

import java.util.Date;
import java.util.List;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.OtherCost;
import me.kuehle.carreport.gui.dialog.SupportDatePickerDialogFragment.SupportDatePickerDialogFragmentListener;
import me.kuehle.carreport.gui.dialog.SupportTimePickerDialogFragment.SupportTimePickerDialogFragmentListener;
import me.kuehle.carreport.gui.util.DateTimeInput;
import me.kuehle.carreport.gui.util.FormFieldGreaterZeroValidator;
import me.kuehle.carreport.gui.util.FormValidator;
import me.kuehle.carreport.gui.util.SimpleAnimator;
import me.kuehle.carreport.util.Recurrence;
import me.kuehle.carreport.util.RecurrenceInterval;

public class DataDetailOtherFragment extends AbstractDataDetailFragment
        implements SupportDatePickerDialogFragmentListener,
        SupportTimePickerDialogFragmentListener {
    private static final int PICK_DATE_REQUEST_CODE = 0;
    private static final int PICK_TIME_REQUEST_CODE = 1;
    private static final int PICK_END_DATE_REQUEST_CODE = 2;

    public static DataDetailOtherFragment newInstance(long id, boolean allowCancel) {
        DataDetailOtherFragment f = new DataDetailOtherFragment();

        Bundle args = new Bundle();
        args.putLong(AbstractDataDetailFragment.EXTRA_ID, id);
        args.putBoolean(AbstractDataDetailFragment.EXTRA_ALLOW_CANCEL, allowCancel);
        f.setArguments(args);

        return f;
    }

    private AutoCompleteTextView edtTitle;
    private DateTimeInput edtDate;
    private DateTimeInput edtTime;
    private EditText edtMileage;
    private EditText edtPrice;
    private Spinner spnRepeat;
    private CheckBox chkEndDate;
    private SimpleAnimator chkEndDateAnimator;
    private SimpleAnimator edtEndDateAnimator;
    private DateTimeInput edtEndDate;
    private EditText edtNote;
    private Spinner spnCar;

    private List<Car> cars;

    @Override
    public void onDialogPositiveClick(int requestCode, Date date) {
        switch (requestCode) {
            case PICK_DATE_REQUEST_CODE:
                edtDate.setDate(date);
                break;
            case PICK_TIME_REQUEST_CODE:
                edtTime.setDate(date);
                break;
            case PICK_END_DATE_REQUEST_CODE:
                edtEndDate.setDate(date);
                break;
        }
    }

    @Override
    protected void fillFields(Bundle savedInstanceState, View v) {
        if (!isInEditMode()) {
            Preferences prefs = new Preferences(getActivity());

            edtDate.setDate(new Date());
            edtTime.setDate(new Date());

            edtEndDate.setDate(new Date());

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
            OtherCost other = (OtherCost) editItem;

            edtDate.setDate(other.date);
            edtTime.setDate(other.date);
            edtTitle.setText(String.valueOf(other.title));
            if (other.mileage > -1) {
                edtMileage.setText(String.valueOf(other.mileage));
            }
            edtPrice.setText(String.valueOf(other.price));
            spnRepeat.setSelection(other.recurrence.getInterval().getValue());
            if (other.recurrence.getInterval() != RecurrenceInterval.ONCE) {
                chkEndDate.setVisibility(View.VISIBLE);
                if (other.endDate != null) {
                    chkEndDate.setChecked(true);
                }
            }

            edtEndDate.setDate(other.endDate == null ? new Date() : other.endDate);
            edtNote.setText(other.note);

            for (int pos = 0; pos < cars.size(); pos++) {
                if (cars.get(pos).id.equals(other.car.id)) {
                    spnCar.setSelection(pos);
                }
            }
        }

        if (spnRepeat.getSelectedItemPosition() == 0) {
            chkEndDate.getLayoutParams().height = 0;
            chkEndDate.setAlpha(0);
        }
        if (!chkEndDate.isChecked()) {
            edtEndDate.getEditText().getLayoutParams().height = 0;
            edtEndDate.getEditText().setAlpha(0);
        }
    }

    @Override
    protected int getAlertDeleteMessage() {
        return R.string.alert_delete_other_message;
    }

    @Override
    protected Model getEditItem(long id) {
        return OtherCost.load(OtherCost.class, id);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_data_detail_other;
    }

    @Override
    protected int getTitleForEdit() {
        return R.string.title_edit_other;
    }

    @Override
    protected int getTitleForNew() {
        return R.string.title_add_other;
    }

    @Override
    protected int getToastDeletedMessage() {
        return R.string.toast_other_deleted;
    }

    @Override
    protected int getToastSavedMessage() {
        return R.string.toast_other_saved;
    }

    @Override
    protected void initFields(Bundle savedInstanceState, View v) {
        Preferences prefs = new Preferences(getActivity());

        edtTitle = (AutoCompleteTextView) v.findViewById(R.id.edt_title);
        edtDate = new DateTimeInput((EditText) v.findViewById(R.id.edt_date),
                DateTimeInput.Mode.DATE);
        edtTime = new DateTimeInput((EditText) v.findViewById(R.id.edt_time),
                DateTimeInput.Mode.TIME);
        edtMileage = (EditText) v.findViewById(R.id.edt_mileage);
        edtPrice = (EditText) v.findViewById(R.id.edt_price);
        spnRepeat = (Spinner) v.findViewById(R.id.spn_repeat);
        chkEndDate = (CheckBox) v.findViewById(R.id.chk_end_date);
        chkEndDateAnimator = new SimpleAnimator(getActivity(), chkEndDate,
                SimpleAnimator.Property.Height);
        edtEndDate = new DateTimeInput((EditText) v.findViewById(R.id.edt_end_date),
                DateTimeInput.Mode.DATE);
        edtEndDateAnimator = new SimpleAnimator(getActivity(), edtEndDate.getEditText(),
                SimpleAnimator.Property.Height);
        edtNote = (EditText) v.findViewById(R.id.edt_note);
        spnCar = (Spinner) v.findViewById(R.id.spn_car);

        // Title
        ArrayAdapter<String> titleAdapter = new ArrayAdapter<>(
                getActivity(), android.R.layout.simple_dropdown_item_1line,
                OtherCost.getAllTitles());
        edtTitle.setAdapter(titleAdapter);

        // Date + Time
        edtDate.applyOnClickListener(DataDetailOtherFragment.this, PICK_DATE_REQUEST_CODE,
                getFragmentManager());
        edtTime.applyOnClickListener(DataDetailOtherFragment.this, PICK_TIME_REQUEST_CODE,
                getFragmentManager());

        // Units
        addUnitToHint(edtMileage, prefs.getUnitDistance());
        addUnitToHint(edtPrice, prefs.getUnitCurrency());

        // Repeat
        spnRepeat.setOnItemSelectedListener(new OnItemSelectedListener() {
            private int mLastPosition = 0;

            @Override
            public void onItemSelected(AdapterView<?> parentView,
                                       View selectedItemView, int position, long id) {
                if (position > 0 && mLastPosition == 0) {
                    chkEndDateAnimator.show();
                } else if (position == 0 && mLastPosition > 0) {
                    chkEndDate.setChecked(false);
                    chkEndDateAnimator.hide();
                }

                mLastPosition = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        chkEndDate.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if (isChecked) {
                    edtEndDateAnimator.show();
                } else {
                    edtEndDateAnimator.hide();
                }
            }
        });

        edtEndDate.applyOnClickListener(DataDetailOtherFragment.this, PICK_END_DATE_REQUEST_CODE,
                getFragmentManager());

        ArrayAdapter<String> carAdapter = new ArrayAdapter<>(
                getActivity(), android.R.layout.simple_spinner_dropdown_item);
        cars = Car.getAll();
        for (Car car : cars) {
            carAdapter.add(car.name);
        }

        spnCar.setAdapter(carAdapter);
    }

    @Override
    protected void save() {
        String title = edtTitle.getText().toString().trim();
        Date date = DateTimeInput.getDateTime(edtDate.getDate(), edtTime.getDate());
        int mileage = getIntegerFromEditText(edtMileage, -1);
        float price = (float) getDoubleFromEditText(edtPrice, 0);
        RecurrenceInterval repInterval = RecurrenceInterval
                .getByValue(spnRepeat.getSelectedItemPosition());
        Recurrence recurrence = new Recurrence(repInterval);
        Date endDate = null;
        if (repInterval != RecurrenceInterval.ONCE && chkEndDate.isChecked()) {
            endDate = edtEndDate.getDate();
        }
        String note = edtNote.getText().toString().trim();
        Car car = cars.get(spnCar.getSelectedItemPosition());

        if (!isInEditMode()) {
            new OtherCost(title, date, endDate, mileage, price, recurrence, note, car).save();
        } else {
            OtherCost other = (OtherCost) editItem;
            other.title = title;
            other.date = date;
            other.endDate = endDate;
            other.mileage = mileage;
            other.price = price;
            other.recurrence = recurrence;
            other.note = note;
            other.car = car;
            other.save();
        }
    }

    @Override
    protected boolean validate() {
        FormValidator validator = new FormValidator();
        validator.add(new FormFieldGreaterZeroValidator(edtPrice));
        return validator.validate();
    }
}