/*
 * Copyright 2015 Jan Kühle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.kuehle.carreport.gui;

import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import com.activeandroid.Model;

import java.util.Date;
import java.util.List;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.Reminder;
import me.kuehle.carreport.gui.dialog.SupportDatePickerDialogFragment;
import me.kuehle.carreport.gui.util.DateTimeInput;
import me.kuehle.carreport.gui.util.FormFieldGreaterZeroValidator;
import me.kuehle.carreport.gui.util.FormFieldNotEmptyValidator;
import me.kuehle.carreport.gui.util.FormValidator;
import me.kuehle.carreport.gui.util.SimpleAnimator;
import me.kuehle.carreport.util.TimeSpan;
import me.kuehle.carreport.util.TimeSpanUnit;

public class DataDetailReminderFragment extends AbstractDataDetailFragment implements
        SupportDatePickerDialogFragment.SupportDatePickerDialogFragmentListener {
    private static final int REQUEST_PICK_START_DATE = 1;
    private static final int REQUEST_PICK_SNOOZED_UNTIL = 2;

    private EditText mEdtTitle;
    private Spinner mSpnCar;
    private Spinner mSpnAfterType;
    private EditText mEdtAfterDistance;
    private TextInputLayout mEdtAfterDistanceInputLayout;
    private EditText mEdtAfterTime;
    private TextInputLayout mEdtAfterTimeInputLayout;
    private Spinner mSpnAfterTimeUnit;
    private EditText mEdtStartMileage;
    private DateTimeInput mEdtStartDate;
    private DateTimeInput mEdtSnoozedUntil;
    private View mBtnQuitSnooze;
    private CheckBox mChkDismissed;

    private SimpleAnimator mEdtAfterDistanceAnimator;
    private SimpleAnimator mEdtAfterTimeAnimator;
    private SimpleAnimator mSpnAfterTimeUnitAnimator;

    private List<Car> mCars;

    @Override
    public void onDialogPositiveClick(int requestCode, Date date) {
        if (requestCode == REQUEST_PICK_START_DATE) {
            mEdtStartDate.setDate(date);
        } else if (requestCode == REQUEST_PICK_SNOOZED_UNTIL) {
            mEdtSnoozedUntil.setDate(date);
        }
    }

    @Override
    protected int getAlertDeleteMessage() {
        return R.string.alert_delete_reminder_message;
    }

    @Override
    protected Model getEditItem(long id) {
        return Reminder.load(Reminder.class, id);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_data_detail_reminder;
    }

    @Override
    protected int getTitleForEdit() {
        return R.string.title_edit_reminder;
    }

    @Override
    protected int getTitleForNew() {
        return R.string.title_add_reminder;
    }

    @Override
    protected int getToastDeletedMessage() {
        return R.string.toast_reminder_deleted;
    }

    @Override
    protected int getToastSavedMessage() {
        return R.string.toast_reminder_saved;
    }

    @Override
    protected void initFields(Bundle savedInstanceState, View v) {
        Preferences prefs = new Preferences(getActivity());

        mEdtTitle = (EditText) v.findViewById(R.id.edt_title);
        mSpnCar = (Spinner) v.findViewById(R.id.spn_car);
        mSpnAfterType = (Spinner) v.findViewById(R.id.spn_after_type);
        mEdtAfterDistance = (EditText) v.findViewById(R.id.edt_after_distance);
        mEdtAfterDistanceInputLayout = (TextInputLayout) v.findViewById(R.id.edt_after_distance_input_layout);
        mEdtAfterTime = (EditText) v.findViewById(R.id.edt_after_time);
        mEdtAfterTimeInputLayout = (TextInputLayout) v.findViewById(R.id.edt_after_time_input_layout);
        mSpnAfterTimeUnit = (Spinner) v.findViewById(R.id.spn_after_time_unit);
        mEdtStartMileage = (EditText) v.findViewById(R.id.edt_start_mileage);
        mEdtStartDate = new DateTimeInput((EditText) v.findViewById(R.id.edt_start_date),
                DateTimeInput.Mode.DATE);
        mEdtSnoozedUntil = new DateTimeInput((EditText) v.findViewById(R.id.edt_snoozed_until),
                DateTimeInput.Mode.DATE);
        mBtnQuitSnooze = v.findViewById(R.id.btn_quit_snooze);
        mChkDismissed = (CheckBox) v.findViewById(R.id.chk_dismissed);

        mEdtAfterDistanceAnimator = new SimpleAnimator(getActivity(), mEdtAfterDistanceInputLayout,
                SimpleAnimator.Property.Height);
        mEdtAfterTimeAnimator = new SimpleAnimator(getActivity(), mEdtAfterTimeInputLayout,
                SimpleAnimator.Property.Height);
        mSpnAfterTimeUnitAnimator = new SimpleAnimator(getActivity(), mSpnAfterTimeUnit,
                SimpleAnimator.Property.Height);

        // Units
        addUnitToHint(mEdtAfterDistance, R.string.hint_reminder_after_distance,
                prefs.getUnitDistance());
        addUnitToHint(mEdtStartMileage, R.string.hint_reminder_start_mileage,
                prefs.getUnitDistance());

        // Car
        mCars = Car.getAll();
        ArrayAdapter<String> carAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item);
        for (Car car : mCars) {
            carAdapter.add(car.name);
        }

        mSpnCar.setAdapter(carAdapter);

        mSpnCar.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private int mLastPosition = ListView.INVALID_POSITION;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isInEditMode() || mLastPosition != ListView.INVALID_POSITION) {
                    Car car = mCars.get(position);
                    mEdtStartMileage.setText(String.valueOf(car.getLatestMileage()));
                }

                mLastPosition = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mLastPosition = ListView.INVALID_POSITION;
            }
        });

        // After types
        mSpnAfterType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private int mLastPosition = 0;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) { // Distance or time
                    if (mLastPosition == 1) {
                        mEdtAfterTimeAnimator.show();
                        mSpnAfterTimeUnitAnimator.show();
                    } else if (mLastPosition == 2) {
                        mEdtAfterDistanceAnimator.show();
                    }
                } else if (position == 1) { // Distance only
                    if (mLastPosition == 0) {
                        mEdtAfterTimeAnimator.hide();
                        mSpnAfterTimeUnitAnimator.hide();
                    } else if (mLastPosition == 2) {
                        mEdtAfterDistanceAnimator.show();
                        mEdtAfterTimeAnimator.hide();
                        mSpnAfterTimeUnitAnimator.hide();
                    }
                } else if (position == 2) { // Time only
                    if (mLastPosition == 0) {
                        mEdtAfterDistanceAnimator.hide();
                    } else if (mLastPosition == 1) {
                        mEdtAfterDistanceAnimator.hide();
                        mEdtAfterTimeAnimator.show();
                        mSpnAfterTimeUnitAnimator.show();
                    }
                }

                mLastPosition = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Date fields
        mEdtStartDate.applyOnClickListener(DataDetailReminderFragment.this, REQUEST_PICK_START_DATE,
                getFragmentManager());
        mEdtSnoozedUntil.applyOnClickListener(DataDetailReminderFragment.this,
                REQUEST_PICK_SNOOZED_UNTIL, getFragmentManager());

        // Quit snooze
        mBtnQuitSnooze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEdtSnoozedUntil.setDate(null);
            }
        });
    }

    @Override
    protected void fillFields(Bundle savedInstanceState, View v) {
        if (!isInEditMode()) {
            Preferences prefs = new Preferences(getActivity());

            long selectCarId = getArguments().getLong(EXTRA_CAR_ID);
            if (selectCarId == 0) {
                selectCarId = prefs.getDefaultCar();
            }
            for (int pos = 0; pos < mCars.size(); pos++) {
                if (mCars.get(pos).id == selectCarId) {
                    mSpnCar.setSelection(pos);
                }
            }

            mEdtStartDate.setDate(new Date());

            mEdtSnoozedUntil.getEditText().setVisibility(View.GONE);
            mBtnQuitSnooze.setVisibility(View.GONE);
            mChkDismissed.setVisibility(View.GONE);
        } else {
            Reminder reminder = (Reminder) editItem;

            mEdtTitle.setText(reminder.title);
            for (int pos = 0; pos < mCars.size(); pos++) {
                if (mCars.get(pos).id.equals(reminder.car.id)) {
                    mSpnCar.setSelection(pos);
                }
            }

            if (reminder.afterDistance != null && reminder.afterTime != null) {
                mSpnAfterType.setSelection(0);
            } else if (reminder.afterDistance != null) {
                mSpnAfterType.setSelection(1);
            } else {
                mSpnAfterType.setSelection(2);
            }

            if (reminder.afterDistance != null) {
                mEdtAfterDistance.setText(reminder.afterDistance.toString());
            }

            if (reminder.afterTime != null) {
                mEdtAfterTime.setText(String.valueOf(reminder.afterTime.getCount()));
                mSpnAfterTimeUnit.setSelection(reminder.afterTime.getUnit().getValue());
            }

            mEdtStartMileage.setText(String.valueOf(reminder.startMileage));
            mEdtStartDate.setDate(reminder.startDate);
            if (reminder.snoozedUntil != null) {
                mEdtSnoozedUntil.setDate(reminder.snoozedUntil);
            }

            mChkDismissed.setChecked(reminder.notificationDismissed);
        }
    }

    @Override
    protected boolean validate() {
        FormValidator validator = new FormValidator();

        validator.add(new FormFieldNotEmptyValidator(mEdtTitle));
        validator.add(new FormFieldGreaterZeroValidator(mEdtStartMileage));

        if (mSpnAfterType.getSelectedItemPosition() == 0) { // Distance and time
            validator.add(new FormFieldGreaterZeroValidator(mEdtAfterDistance));
            validator.add(new FormFieldGreaterZeroValidator(mEdtAfterTime));
        } else if (mSpnAfterType.getSelectedItemPosition() == 1) { // Distance only
            validator.add(new FormFieldGreaterZeroValidator(mEdtAfterDistance));
        } else { // Time only
            validator.add(new FormFieldGreaterZeroValidator(mEdtAfterTime));
        }

        return validator.validate();
    }

    @Override
    protected void save() {
        String title = mEdtTitle.getText().toString();
        Car car = mCars.get(mSpnCar.getSelectedItemPosition());
        Integer afterDistance = null;
        TimeSpan afterTime = null;
        if (mSpnAfterType.getSelectedItemPosition() == 0) { // Distance and time
            afterDistance = getIntegerFromEditText(mEdtAfterDistance, 0);
            afterTime = new TimeSpan(
                    TimeSpanUnit.getByValue(mSpnAfterTimeUnit.getSelectedItemPosition()),
                    getIntegerFromEditText(mEdtAfterTime, 0));
        } else if (mSpnAfterType.getSelectedItemPosition() == 1) { // Distance only
            afterDistance = getIntegerFromEditText(mEdtAfterDistance, 0);
        } else { // Time only
            afterTime = new TimeSpan(
                    TimeSpanUnit.getByValue(mSpnAfterTimeUnit.getSelectedItemPosition()),
                    getIntegerFromEditText(mEdtAfterTime, 0));
        }

        int startMileage = getIntegerFromEditText(mEdtStartMileage, 0);
        Date startDate = mEdtStartDate.getDate();
        Date snoozedUntil = mEdtSnoozedUntil.getDate();
        boolean dismissed = mChkDismissed.isChecked();

        if (!isInEditMode()) {
            new Reminder(title, afterTime, afterDistance, startDate, startMileage, car).save();
        } else {
            Reminder reminder = (Reminder) editItem;
            reminder.title = title;
            reminder.afterDistance = afterDistance;
            reminder.afterTime = afterTime;
            reminder.startMileage = startMileage;
            reminder.startDate = startDate;
            reminder.car = car;
            reminder.snoozedUntil = snoozedUntil;
            reminder.notificationDismissed = dismissed;
            reminder.save();
        }
    }
}
