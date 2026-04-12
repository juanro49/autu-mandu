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

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.dialog.DatePickerDialogFragment;
import org.juanro.autumandu.gui.util.DateTimeInput;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.Reminder;
import org.juanro.autumandu.model.entity.helper.TimeSpanUnit;
import org.juanro.autumandu.util.reminder.ReminderWorker;
import org.juanro.autumandu.viewmodel.ReminderDetailViewModel;

import java.util.Date;

/**
 * Fragment to edit reminder details.
 */
public class DataDetailReminderFragment extends AbstractDataDetailFragment {
    private static final int PICK_START_DATE_REQUEST_CODE = 0;
    private static final int PICK_SNOOZED_UNTIL_REQUEST_CODE = 1;

    private EditText edtTitle;
    private Spinner spnCar;
    private Spinner spnAfterType;
    private EditText edtAfterDistance;
    private EditText edtAfterTime;
    private Spinner spnAfterTimeUnit;
    private View edtAfterDistanceLayout;
    private View edtAfterTimeLayout;
    private EditText edtStartMileage;
    private DateTimeInput edtStartDate;
    private DateTimeInput edtSnoozedUntil;
    private CheckBox chkDismissed;

    private long mInitialCarId = -1;
    private ReminderDetailViewModel viewModel;

    public static DataDetailReminderFragment newInstance(long id) {
        DataDetailReminderFragment f = new DataDetailReminderFragment();
        Bundle args = new Bundle();
        args.putLong(AbstractDataDetailFragment.EXTRA_ID, id);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ReminderDetailViewModel.class);
        viewModel.setReminderId(mId);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_data_detail_reminder;
    }

    @Override
    protected int getTitleForNew() {
        return R.string.title_add_reminder;
    }

    @Override
    protected int getTitleForEdit() {
        return R.string.title_edit_reminder;
    }

    @Override
    protected int getAlertDeleteMessage() {
        return R.string.alert_delete_reminder_message;
    }

    @Override
    protected void fillFields(Bundle savedInstanceState, View v) {
        if (isInEditMode()) {
            viewModel.getReminder().observe(getViewLifecycleOwner(), reminder -> {
                if (reminder != null) {
                    edtTitle.setText(reminder.getTitle());
                    if (reminder.getAfterDistance() != null) {
                        edtAfterDistance.setText(String.valueOf(reminder.getAfterDistance()));
                    } else {
                        edtAfterDistance.setText("");
                    }
                    if (reminder.getAfterTimeSpanCount() != null) {
                        edtAfterTime.setText(String.valueOf(reminder.getAfterTimeSpanCount()));
                    } else {
                        edtAfterTime.setText("");
                    }

                    if (reminder.getAfterDistance() != null && reminder.getAfterTimeSpanCount() != null) {
                        spnAfterType.setSelection(0);
                    } else if (reminder.getAfterDistance() != null) {
                        spnAfterType.setSelection(1);
                    } else {
                        spnAfterType.setSelection(2);
                    }

                    if (reminder.getAfterTimeSpanUnit() != null) {
                        spnAfterTimeUnit.setSelection(reminder.getAfterTimeSpanUnit().ordinal());
                    }

                    edtStartMileage.setText(String.valueOf(reminder.getStartMileage()));
                    edtStartDate.setDate(reminder.getStartDate());
                    edtSnoozedUntil.setDate(reminder.getSnoozedUntil());
                    chkDismissed.setChecked(reminder.isNotificationDismissed());

                    mInitialCarId = reminder.getCarId();
                    for (int pos = 0; pos < spnCar.getCount(); pos++) {
                        if (spnCar.getItemIdAtPosition(pos) == mInitialCarId) {
                            spnCar.setSelection(pos);
                            break;
                        }
                    }
                }
            });
        } else {
            mInitialCarId = getArguments() != null ? getArguments().getLong(EXTRA_CAR_ID, -1) : -1;
            if (mInitialCarId == -1) {
                Preferences prefs = new Preferences(requireContext());
                mInitialCarId = prefs.getDefaultCar();
            }
            viewModel.setSelectedCarId(mInitialCarId);
            edtStartDate.setDate(new Date());
        }
    }

    @Override
    protected void initFields(Bundle savedInstanceState, View v) {
        final Preferences prefs = new Preferences(requireContext());

        edtTitle = v.findViewById(R.id.edt_title);
        spnCar = v.findViewById(R.id.spn_car);
        spnAfterType = v.findViewById(R.id.spn_after_type);
        edtAfterDistance = v.findViewById(R.id.edt_after_distance);
        edtAfterDistanceLayout = v.findViewById(R.id.edt_after_distance_input_layout);
        edtAfterTime = v.findViewById(R.id.edt_after_time);
        edtAfterTimeLayout = v.findViewById(R.id.edt_after_time_input_layout);
        spnAfterTimeUnit = v.findViewById(R.id.spn_after_time_unit);
        edtStartMileage = v.findViewById(R.id.edt_start_mileage);
        edtStartDate = new DateTimeInput(v.findViewById(R.id.edt_start_date), DateTimeInput.Mode.DATE);
        edtSnoozedUntil = new DateTimeInput(v.findViewById(R.id.edt_snoozed_until), DateTimeInput.Mode.DATE);
        View edtSnoozedUntilLayout = v.findViewById(R.id.edt_snoozed_until_input_layout);
        ImageButton btnQuitSnooze = v.findViewById(R.id.btn_quit_snooze);
        chkDismissed = v.findViewById(R.id.chk_dismissed);
        View txtSectionAdvanced = v.findViewById(R.id.txt_section_advanced);

        addUnitToHint(edtAfterDistance, R.string.hint_reminder_after_distance, prefs.getUnitDistance());
        addUnitToHint(edtStartMileage, R.string.hint_reminder_start_mileage, prefs.getUnitDistance());

        spnAfterType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                edtAfterDistanceLayout.setVisibility(position == 0 || position == 1 ? View.VISIBLE : View.GONE);
                edtAfterTimeLayout.setVisibility(position == 0 || position == 2 ? View.VISIBLE : View.GONE);
                spnAfterTimeUnit.setVisibility(position == 0 || position == 2 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        if (!isInEditMode()) {
            txtSectionAdvanced.setVisibility(View.GONE);
            edtSnoozedUntilLayout.setVisibility(View.GONE);
            btnQuitSnooze.setVisibility(View.GONE);
            chkDismissed.setVisibility(View.GONE);
        }

        getParentFragmentManager().setFragmentResultListener(DatePickerDialogFragment.REQUEST_KEY, getViewLifecycleOwner(), (requestKey, result) -> {
            int requestCode = result.getInt(DatePickerDialogFragment.RESULT_REQUEST_CODE);
            Date date = new Date(result.getLong(DatePickerDialogFragment.RESULT_DATE));
            if (requestCode == PICK_START_DATE_REQUEST_CODE) {
                edtStartDate.setDate(date);
            } else if (requestCode == PICK_SNOOZED_UNTIL_REQUEST_CODE) {
                edtSnoozedUntil.setDate(date);
            }
        });

        edtStartDate.applyOnClickListener(PICK_START_DATE_REQUEST_CODE, getParentFragmentManager());
        edtSnoozedUntil.applyOnClickListener(PICK_SNOOZED_UNTIL_REQUEST_CODE, getParentFragmentManager());

        btnQuitSnooze.setOnClickListener(v1 -> edtSnoozedUntil.setDate(null));

        spnCar.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                viewModel.setSelectedCarId(id);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        if (!isInEditMode()) {
            viewModel.getLatestMileageForSelectedCar().observe(getViewLifecycleOwner(), mileage -> {
                if (mileage != null) {
                    edtStartMileage.setText(String.valueOf(mileage));
                }
            });
        }

        viewModel.getActiveCars().observe(getViewLifecycleOwner(), cars -> {
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

            if (mInitialCarId != -1) {
                for (int pos = 0; pos < spnCar.getCount(); pos++) {
                    if (spnCar.getItemIdAtPosition(pos) == mInitialCarId) {
                        spnCar.setSelection(pos);
                        break;
                    }
                }
            }
        });
    }

    @Override
    protected boolean validate() {
        boolean valid = !edtTitle.getText().toString().trim().isEmpty();
        if (valid) {
            if (spnAfterType.getSelectedItemPosition() == 0) {
                valid = !TextUtils.isEmpty(edtAfterDistance.getText()) && !TextUtils.isEmpty(edtAfterTime.getText());
            } else if (spnAfterType.getSelectedItemPosition() == 1) {
                valid = !TextUtils.isEmpty(edtAfterDistance.getText());
            } else {
                valid = !TextUtils.isEmpty(edtAfterTime.getText());
            }
        }
        return valid;
    }

    @Override
    protected long save() {
        return 0; // No se usa, usamos saveAsync
    }

    @Override
    protected void saveAsync() {
        Reminder reminder = new Reminder();
        if (isInEditMode()) {
            reminder.setId(mId);
        }

        reminder.setTitle(edtTitle.getText().toString().trim());
        reminder.setCarId(spnCar.getSelectedItemId());

        if (spnAfterType.getSelectedItemPosition() == 0 || spnAfterType.getSelectedItemPosition() == 1) {
            reminder.setAfterDistance(getIntegerFromEditText(edtAfterDistance, 0));
        } else {
            reminder.setAfterDistance(null);
        }

        if (spnAfterType.getSelectedItemPosition() == 0 || spnAfterType.getSelectedItemPosition() == 2) {
            reminder.setAfterTimeSpanCount(getIntegerFromEditText(edtAfterTime, 0));
            reminder.setAfterTimeSpanUnit(TimeSpanUnit.values()[spnAfterTimeUnit.getSelectedItemPosition()]);
        } else {
            reminder.setAfterTimeSpanCount(null);
            reminder.setAfterTimeSpanUnit(null);
        }

        reminder.setStartMileage(getIntegerFromEditText(edtStartMileage, 0));
        Date startDate = edtStartDate.getDate();
        reminder.setStartDate(startDate != null ? startDate : new Date());
        reminder.setSnoozedUntil(edtSnoozedUntil.getDate());
        reminder.setNotificationDismissed(chkDismissed.isChecked());

        viewModel.save(reminder, () -> requireActivity().runOnUiThread(() -> {
            if (isAdded()) {
                ReminderWorker.enqueueUpdate(requireContext());
                mOnItemActionListener.onItemSavedAsync(reminder.getId());
            }
        }));
    }

    @Override
    protected void deleteAsync() {
        if (mId != EXTRA_ID_DEFAULT) {
            viewModel.delete(mId, () -> {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        if (mOnItemActionListener != null) {
                            mOnItemActionListener.onItemDeletedAsync();
                        }
                    });
                }
            });
        }
    }

    @Override
    protected void delete() {
    }
}
