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

import android.content.ContentUris;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import java.util.Date;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.data.query.CarQueries;
import me.kuehle.carreport.gui.dialog.SupportDatePickerDialogFragment;
import me.kuehle.carreport.gui.util.DateTimeInput;
import me.kuehle.carreport.gui.util.FormFieldGreaterZeroValidator;
import me.kuehle.carreport.gui.util.FormFieldNotEmptyValidator;
import me.kuehle.carreport.gui.util.FormValidator;
import me.kuehle.carreport.gui.util.SimpleAnimator;
import me.kuehle.carreport.provider.car.CarColumns;
import me.kuehle.carreport.provider.car.CarCursor;
import me.kuehle.carreport.provider.car.CarSelection;
import me.kuehle.carreport.provider.reminder.ReminderContentValues;
import me.kuehle.carreport.provider.reminder.ReminderCursor;
import me.kuehle.carreport.provider.reminder.ReminderSelection;
import me.kuehle.carreport.provider.reminder.TimeSpanUnit;

public class DataDetailReminderFragment extends AbstractDataDetailFragment implements
        SupportDatePickerDialogFragment.SupportDatePickerDialogFragmentListener {
    private static final int REQUEST_PICK_START_DATE = 1;
    private static final int REQUEST_PICK_SNOOZED_UNTIL = 2;

    private EditText mEdtTitle;
    private Spinner mSpnCar;
    private Spinner mSpnAfterType;
    private EditText mEdtAfterDistance;
    private EditText mEdtAfterTime;
    private Spinner mSpnAfterTimeUnit;
    private EditText mEdtStartMileage;
    private DateTimeInput mEdtStartDate;
    private DateTimeInput mEdtSnoozedUntil;
    private View mBtnQuitSnooze;
    private CheckBox mChkDismissed;

    private SimpleAnimator mEdtAfterDistanceAnimator;
    private SimpleAnimator mEdtAfterTimeAnimator;
    private SimpleAnimator mSpnAfterTimeUnitAnimator;

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
    protected void initFields(Bundle savedInstanceState, View v) {
        Preferences prefs = new Preferences(getActivity());

        mEdtTitle = (EditText) v.findViewById(R.id.edt_title);
        mSpnCar = (Spinner) v.findViewById(R.id.spn_car);
        mSpnAfterType = (Spinner) v.findViewById(R.id.spn_after_type);
        mEdtAfterDistance = (EditText) v.findViewById(R.id.edt_after_distance);
        TextInputLayout mEdtAfterDistanceInputLayout = (TextInputLayout) v.findViewById(R.id.edt_after_distance_input_layout);
        mEdtAfterTime = (EditText) v.findViewById(R.id.edt_after_time);
        TextInputLayout mEdtAfterTimeInputLayout = (TextInputLayout) v.findViewById(R.id.edt_after_time_input_layout);
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
        CarCursor car = new CarSelection().query(getActivity().getContentResolver(), null,
                CarColumns.NAME + " COLLATE UNICODE");
        mSpnCar.setAdapter(new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_spinner_dropdown_item,
                car, new String[]{CarColumns.NAME}, new int[]{android.R.id.text1}, 0));

        mSpnCar.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private int mLastPosition = ListView.INVALID_POSITION;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isInEditMode() || mLastPosition != ListView.INVALID_POSITION) {
                    mEdtStartMileage.setText(String.valueOf(CarQueries.getLatestMileage(getActivity(), id)));
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

            for (int pos = 0; pos < mSpnCar.getCount(); pos++) {
                if (mSpnCar.getItemIdAtPosition(pos) == selectCarId) {
                    mSpnCar.setSelection(pos);
                }
            }

            mEdtStartDate.setDate(new Date());

            mEdtSnoozedUntil.getEditText().setVisibility(View.GONE);
            mBtnQuitSnooze.setVisibility(View.GONE);
            mChkDismissed.setVisibility(View.GONE);
        } else {
            ReminderCursor reminder = new ReminderSelection().id(mId).query(getActivity().getContentResolver());
            reminder.moveToNext();

            mEdtTitle.setText(reminder.getTitle());
            for (int pos = 0; pos < mSpnCar.getCount(); pos++) {
                if (mSpnCar.getItemIdAtPosition(pos) == reminder.getCarId()) {
                    mSpnCar.setSelection(pos);
                }
            }

            if (reminder.getAfterDistance() != null && reminder.getAfterTimeSpanUnit() != null) {
                mSpnAfterType.setSelection(0);
            } else if (reminder.getAfterDistance() != null) {
                mSpnAfterType.setSelection(1);
            } else {
                mSpnAfterType.setSelection(2);
            }

            if (reminder.getAfterDistance() != null) {
                mEdtAfterDistance.setText(String.valueOf(reminder.getAfterDistance()));
            }

            if (reminder.getAfterTimeSpanUnit() != null) {
                mEdtAfterTime.setText(String.valueOf(reminder.getAfterTimeSpanCount()));
                mSpnAfterTimeUnit.setSelection(reminder.getAfterTimeSpanUnit().ordinal());
            }

            mEdtStartMileage.setText(String.valueOf(reminder.getStartMileage()));
            mEdtStartDate.setDate(reminder.getStartDate());
            if (reminder.getSnoozedUntil() != null) {
                mEdtSnoozedUntil.setDate(reminder.getSnoozedUntil());
            }

            mChkDismissed.setChecked(reminder.getNotificationDismissed());
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
    protected long save() {
        Integer afterDistance = null;
        Integer afterTimeSpanCount = null;
        TimeSpanUnit afterTimeSpanUnit = null;
        if (mSpnAfterType.getSelectedItemPosition() == 0) { // Distance and time
            afterDistance = getIntegerFromEditText(mEdtAfterDistance, 0);
            afterTimeSpanCount = getIntegerFromEditText(mEdtAfterTime, 0);
            afterTimeSpanUnit = TimeSpanUnit.values()[mSpnAfterTimeUnit.getSelectedItemPosition()];
        } else if (mSpnAfterType.getSelectedItemPosition() == 1) { // Distance only
            afterDistance = getIntegerFromEditText(mEdtAfterDistance, 0);
        } else { // Time only
            afterTimeSpanCount = getIntegerFromEditText(mEdtAfterTime, 0);
            afterTimeSpanUnit = TimeSpanUnit.values()[mSpnAfterTimeUnit.getSelectedItemPosition()];
        }

        ReminderContentValues values = new ReminderContentValues();
        values.putTitle(mEdtTitle.getText().toString());
        values.putAfterDistance(afterDistance);
        values.putAfterTimeSpanCount(afterTimeSpanCount);
        values.putAfterTimeSpanUnit(afterTimeSpanUnit);
        values.putStartMileage(getIntegerFromEditText(mEdtStartMileage, 0));
        values.putStartDate(mEdtStartDate.getDate());
        values.putSnoozedUntil(mEdtSnoozedUntil.getDate());
        values.putNotificationDismissed(mChkDismissed.isChecked());
        values.putCarId(mSpnCar.getSelectedItemId());

        if (isInEditMode()) {
            ReminderSelection where = new ReminderSelection().id(mId);
            values.update(getActivity().getContentResolver(), where);
            return mId;
        } else {
            Uri uri = values.insert(getActivity().getContentResolver());
            return ContentUris.parseId(uri);
        }
    }

    @Override
    public void delete() {
        new ReminderSelection().id(mId).delete(getActivity().getContentResolver());
    }
}
