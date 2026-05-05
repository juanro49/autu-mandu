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
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.adapter.CarArrayAdapter;
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
                    updateFieldsFromReminder(reminder);
                    updateInitialCarSelection(reminder.getCarId());
                }
            });
        } else {
            setupNewReminderDefaults();
        }
    }

    private void updateFieldsFromReminder(Reminder reminder) {
        edtTitle.setText(reminder.getTitle());
        edtAfterDistance.setText(reminder.getAfterDistance() != null ? String.valueOf(reminder.getAfterDistance()) : "");
        edtAfterTime.setText(reminder.getAfterTimeSpanCount() != null ? String.valueOf(reminder.getAfterTimeSpanCount()) : "");

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
    }

    private void updateInitialCarSelection(long carId) {
        mInitialCarId = carId;
        selectSpinnerItemById(spnCar, mInitialCarId);
    }

    private void setupNewReminderDefaults() {
        mInitialCarId = getArguments() != null ? getArguments().getLong(EXTRA_CAR_ID, -1) : -1;
        if (mInitialCarId == -1) {
            Preferences prefs = new Preferences(requireContext());
            mInitialCarId = prefs.getDefaultCar();
        }
        viewModel.setSelectedCarId(mInitialCarId);
        edtStartDate.setDate(new Date());
    }

    @Override
    protected void initFields(Bundle savedInstanceState, View v) {
        final Preferences prefs = new Preferences(requireContext());
        initViewReferences(v);
        setupUnitHints(prefs);
        setupAfterTypeListener();
        setupAdvancedFieldsVisibility(v);
        setupPickers(v);
        setupCarSpinner();
        setupLatestMileageObserver();
    }

    private void initViewReferences(View v) {
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
        chkDismissed = v.findViewById(R.id.chk_dismissed);
    }

    private void setupUnitHints(Preferences prefs) {
        addUnitToHint(edtAfterDistance, R.string.hint_reminder_after_distance, prefs.getUnitDistance());
        addUnitToHint(edtStartMileage, R.string.hint_reminder_start_mileage, prefs.getUnitDistance());
    }

    private void setupAfterTypeListener() {
        spnAfterType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                edtAfterDistanceLayout.setVisibility(position == 0 || position == 1 ? View.VISIBLE : View.GONE);
                edtAfterTimeLayout.setVisibility(position == 0 || position == 2 ? View.VISIBLE : View.GONE);
                spnAfterTimeUnit.setVisibility(position == 0 || position == 2 ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // Not used
            }
        });
    }

    private void setupAdvancedFieldsVisibility(View v) {
        if (!isInEditMode()) {
            v.findViewById(R.id.txt_section_advanced).setVisibility(View.GONE);
            v.findViewById(R.id.edt_snoozed_until_input_layout).setVisibility(View.GONE);
            v.findViewById(R.id.btn_quit_snooze).setVisibility(View.GONE);
            v.findViewById(R.id.chk_dismissed).setVisibility(View.GONE);
        }
    }

    private void setupPickers(View v) {
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
        View btnQuitSnooze = v.findViewById(R.id.btn_quit_snooze);
        if (btnQuitSnooze != null) {
            btnQuitSnooze.setOnClickListener(v1 -> edtSnoozedUntil.setDate(null));
        }
    }

    private void setupCarSpinner() {
        spnCar.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                viewModel.setSelectedCarId(id);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // Not used
            }
        });

        viewModel.getActiveCars().observe(getViewLifecycleOwner(), cars -> {
            spnCar.setAdapter(new CarArrayAdapter(requireContext(), cars));

            if (mInitialCarId != -1) {
                selectSpinnerItemById(spnCar, mInitialCarId);
            }
        });
    }

    private void setupLatestMileageObserver() {
        if (!isInEditMode()) {
            viewModel.getLatestMileageForSelectedCar().observe(getViewLifecycleOwner(), mileage -> {
                if (mileage != null) {
                    edtStartMileage.setText(String.valueOf(mileage));
                }
            });
        }
    }

    @Override
    protected boolean validate() {
        boolean valid = !edtTitle.getText().toString().trim().isEmpty();
        if (valid) {
            int position = spnAfterType.getSelectedItemPosition();
            valid = switch (position) {
                case 0 -> !TextUtils.isEmpty(edtAfterDistance.getText()) && !TextUtils.isEmpty(edtAfterTime.getText());
                case 1 -> !TextUtils.isEmpty(edtAfterDistance.getText());
                default -> !TextUtils.isEmpty(edtAfterTime.getText());
            };
        }
        return valid;
    }

    @Override
    protected long save() {
        return 0; // Using saveAsync
    }

    @Override
    protected void saveAsync() {
        Reminder reminder = createReminderFromFields();
        viewModel.save(reminder, () -> requireActivity().runOnUiThread(() -> {
            if (isAdded()) {
                ReminderWorker.enqueueUpdate(requireContext());
                mOnItemActionListener.onItemSavedAsync(reminder.getId());
            }
        }));
    }

    private Reminder createReminderFromFields() {
        Reminder reminder = new Reminder();
        if (isInEditMode()) {
            reminder.setId(mId);
        }

        reminder.setTitle(edtTitle.getText().toString().trim());
        reminder.setCarId(spnCar.getSelectedItemId());

        int afterType = spnAfterType.getSelectedItemPosition();
        if (afterType == 0 || afterType == 1) {
            reminder.setAfterDistance(getIntegerFromEditText(edtAfterDistance, 0));
        } else {
            reminder.setAfterDistance(null);
        }

        if (afterType == 0 || afterType == 2) {
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
        return reminder;
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
        // Using deleteAsync
    }
}
