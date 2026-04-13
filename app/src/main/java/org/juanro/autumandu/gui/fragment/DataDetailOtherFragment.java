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
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputLayout;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.dialog.DatePickerDialogFragment;
import org.juanro.autumandu.gui.dialog.TimePickerDialogFragment;
import org.juanro.autumandu.gui.util.DateTimeInput;
import org.juanro.autumandu.gui.util.FormFieldGreaterZeroValidator;
import org.juanro.autumandu.gui.util.FormValidator;
import org.juanro.autumandu.gui.util.SimpleAnimator;
import org.juanro.autumandu.model.entity.OtherCost;
import org.juanro.autumandu.model.entity.helper.RecurrenceInterval;
import org.juanro.autumandu.util.reminder.ReminderWorker;
import org.juanro.autumandu.viewmodel.OtherDetailViewModel;

import java.util.Date;

/**
 * Fragment to edit other cost/income details.
 */
public class DataDetailOtherFragment extends AbstractDataDetailFragment {
    public static final String EXTRA_OTHER_TYPE = "other_type";
    public static final int EXTRA_OTHER_TYPE_EXPENDITURE = 0;
    public static final int EXTRA_OTHER_TYPE_INCOME = 1;

    private static final int PICK_DATE_REQUEST_CODE = 0;
    private static final int PICK_TIME_REQUEST_CODE = 1;
    private static final int PICK_END_DATE_REQUEST_CODE = 2;

    public static DataDetailOtherFragment newInstance(long id) {
        var f = new DataDetailOtherFragment();
        var args = new Bundle();
        args.putLong(AbstractDataDetailFragment.EXTRA_ID, id);
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

    private long mInitialCarId = -1;
    private Boolean cachedIsExpenditure = null;
    private OtherDetailViewModel viewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(OtherDetailViewModel.class);
        viewModel.setId(mId);
        viewModel.setType(isExpenditure() ? 0 : 1);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_data_detail_other;
    }

    @Override
    protected void fillFields(Bundle savedInstanceState, View v) {
        getParentFragmentManager().setFragmentResultListener(DatePickerDialogFragment.REQUEST_KEY, getViewLifecycleOwner(), (requestKey, result) -> {
            int requestCode = result.getInt(DatePickerDialogFragment.RESULT_REQUEST_CODE);
            var date = new Date(result.getLong(DatePickerDialogFragment.RESULT_DATE));
            switch (requestCode) {
                case PICK_DATE_REQUEST_CODE -> edtDate.setDate(date);
                case PICK_END_DATE_REQUEST_CODE -> edtEndDate.setDate(date);
            }
        });

        getParentFragmentManager().setFragmentResultListener(TimePickerDialogFragment.REQUEST_KEY, getViewLifecycleOwner(), (requestKey, result) -> {
            int requestCode = result.getInt(TimePickerDialogFragment.RESULT_REQUEST_CODE);
            if (requestCode == PICK_TIME_REQUEST_CODE) {
                edtTime.setDate(new Date(result.getLong(TimePickerDialogFragment.RESULT_TIME)));
            }
        });

        if (isInEditMode()) {
            viewModel.getOtherCost().observe(getViewLifecycleOwner(), otherCost -> {
                if (otherCost != null) {
                    cachedIsExpenditure = otherCost.getPrice() > 0;
                    edtDate.setDate(otherCost.getDate());
                    edtTime.setDate(otherCost.getDate());
                    edtTitle.setText(otherCost.getTitle());
                    if (otherCost.getMileage() != null && otherCost.getMileage() > -1) {
                        edtMileage.setText(String.valueOf(otherCost.getMileage()));
                    }

                    if (isExpenditure()) {
                        edtPrice.setText(String.valueOf(otherCost.getPrice()));
                    } else {
                        edtPrice.setText(String.valueOf(-otherCost.getPrice()));
                    }

                    spnRepeat.setSelection(otherCost.getRecurrenceInterval().ordinal());
                    if (otherCost.getRecurrenceInterval() != RecurrenceInterval.ONCE) {
                        if (otherCost.getEndDate() != null) {
                            chkEndDate.setChecked(true);
                            edtEndDateAnimator.show();
                        }
                    } else {
                        chkEndDate.setChecked(false);
                        chkEndDateAnimator.hide();
                        edtEndDateAnimator.hide();
                    }

                    edtEndDate.setDate(otherCost.getEndDate() == null ? new Date() : otherCost.getEndDate());
                    edtNote.setText(otherCost.getNote());

                    mInitialCarId = otherCost.getCarId();
                    for (int pos = 0; pos < spnCar.getCount(); pos++) {
                        if (spnCar.getItemIdAtPosition(pos) == mInitialCarId) {
                            spnCar.setSelection(pos);
                            break;
                        }
                    }

                    if (spnRepeat.getSelectedItemPosition() == 0) {
                        chkEndDateAnimator.hide();
                    }
                    if (!chkEndDate.isChecked()) {
                        edtEndDateAnimator.hide();
                    }
                }
            });
        } else {
            edtDate.setDate(new Date());
            edtTime.setDate(new Date());
            edtEndDate.setDate(new Date());

            // Initial visibility for new items
            chkEndDateAnimator.hide();
            edtEndDateAnimator.hide();
        }
    }

    @Override
    protected int getAlertDeleteMessage() {
        return isExpenditure() ? R.string.alert_delete_other_expenditure_message : R.string.alert_delete_other_income_message;
    }

    @Override
    protected int getTitleForEdit() {
        return isExpenditure() ? R.string.title_edit_other_expenditure : R.string.title_edit_other_income;
    }

    @Override
    protected int getTitleForNew() {
        return isExpenditure() ? R.string.title_add_other_expenditure : R.string.title_add_other_income;
    }

    @Override
    protected void initFields(Bundle savedInstanceState, View v) {
        var prefs = new Preferences(requireContext());

        edtTitle = v.findViewById(R.id.edt_title);
        edtDate = new DateTimeInput(v.findViewById(R.id.edt_date), DateTimeInput.Mode.DATE);
        edtTime = new DateTimeInput(v.findViewById(R.id.edt_time), DateTimeInput.Mode.TIME);
        edtMileage = v.findViewById(R.id.edt_mileage);
        edtPrice = v.findViewById(R.id.edt_price);
        spnRepeat = v.findViewById(R.id.spn_repeat);
        chkEndDate = v.findViewById(R.id.chk_end_date);
        chkEndDateAnimator = new SimpleAnimator(getActivity(), chkEndDate, SimpleAnimator.Property.Height);
        edtEndDate = new DateTimeInput(v.findViewById(R.id.edt_end_date), DateTimeInput.Mode.DATE);
        TextInputLayout edtEndDateInputLayout = v.findViewById(R.id.edt_end_date_input_layout);
        edtEndDateAnimator = new SimpleAnimator(getActivity(), edtEndDateInputLayout, SimpleAnimator.Property.Height);
        edtNote = v.findViewById(R.id.edt_note);
        spnCar = v.findViewById(R.id.spn_car);

        // Title
        viewModel.getTitles().observe(getViewLifecycleOwner(), titles ->
            edtTitle.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_dropdown_item, titles.toArray(new String[0]))));

        // Date + Time
        edtDate.applyOnClickListener(PICK_DATE_REQUEST_CODE, getParentFragmentManager());
        edtTime.applyOnClickListener(PICK_TIME_REQUEST_CODE, getParentFragmentManager());

        // Units
        addUnitToHint(edtMileage, R.string.hint_mileage_optional, prefs.getUnitDistance());
        addUnitToHint(edtPrice, R.string.hint_price, prefs.getUnitCurrency());

        // Repeat
        spnRepeat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private int lastPosition = 0;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && lastPosition == 0) {
                    chkEndDateAnimator.show();
                } else if (position == 0 && lastPosition > 0) {
                    chkEndDate.setChecked(false);
                    chkEndDateAnimator.hide();
                    edtEndDateAnimator.hide();
                }
                lastPosition = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        chkEndDate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                edtEndDateAnimator.show();
            } else {
                edtEndDateAnimator.hide();
            }
        });

        edtEndDate.applyOnClickListener(PICK_END_DATE_REQUEST_CODE, getParentFragmentManager());

        // Car
        viewModel.getCars().observe(getViewLifecycleOwner(), cars -> {
            spnCar.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, cars) {
                @Override
                public long getItemId(int position) {
                    var item = getItem(position);
                    return item != null ? item.getId() : -1;
                }

                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    var view = (TextView) super.getView(position, convertView, parent);
                    var item = getItem(position);
                    if (item != null) {
                        view.setText(item.getName());
                    }
                    return view;
                }

                @NonNull
                @Override
                public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    var view = (TextView) super.getDropDownView(position, convertView, parent);
                    var item = getItem(position);
                    if (item != null) {
                        view.setText(item.getName());
                    }
                    return view;
                }
            });

            if (!isInEditMode()) {
                var currentPrefs = new Preferences(requireContext());
                mInitialCarId = getArguments() != null ? getArguments().getLong(EXTRA_CAR_ID) : 0;
                if (mInitialCarId == 0) {
                    mInitialCarId = currentPrefs.getDefaultCar();
                }
            }

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
    protected void saveAsync() {
        float price = (float) getDoubleFromEditText(edtPrice);
        if (!isExpenditure()) {
            price *= -1;
        }

        var recurrenceInterval = RecurrenceInterval.fromId(spnRepeat.getSelectedItemPosition());
        Date endDate = null;
        if (recurrenceInterval != RecurrenceInterval.ONCE && chkEndDate.isChecked()) {
            endDate = edtEndDate.getDate();
        }

        var otherCost = new OtherCost();
        if (isInEditMode()) {
            otherCost.setId(mId);
        }

        otherCost.setTitle(edtTitle.getText().toString().trim());
        otherCost.setDate(DateTimeInput.getDateTime(edtDate.getDate(), edtTime.getDate()));
        otherCost.setMileage(getIntegerFromEditText(edtMileage, -1));
        otherCost.setPrice(price);
        otherCost.setRecurrenceInterval(recurrenceInterval);
        otherCost.setRecurrenceMultiplier(1);
        otherCost.setEndDate(endDate);
        otherCost.setNote(edtNote.getText().toString().trim());
        otherCost.setCarId(spnCar.getSelectedItemId());

        viewModel.save(otherCost, () -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    ReminderWorker.enqueueUpdate(requireContext());
                    mOnItemActionListener.onItemSavedAsync(otherCost.getId());
                });
            }
        });
    }

    @Override
    protected void deleteAsync() {
        var otherCost = viewModel.getOtherCost().getValue();
        if (otherCost != null) {
            viewModel.delete(otherCost, () -> {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> mOnItemActionListener.onItemDeletedAsync());
                }
            });
        }
    }

    @Override
    protected long save() {
        return 0;
    }

    @Override
    protected void delete() {
    }

    @Override
    protected boolean validate() {
        var validator = new FormValidator();
        validator.add(new FormFieldGreaterZeroValidator(edtPrice));
        return validator.validate();
    }

    private boolean isExpenditure() {
        if (cachedIsExpenditure == null) {
            if (isInEditMode()) {
                var otherCost = viewModel.getOtherCost().getValue();
                if (otherCost != null) {
                    cachedIsExpenditure = otherCost.getPrice() > 0;
                } else {
                    cachedIsExpenditure = getArguments() != null && getArguments().getInt(EXTRA_OTHER_TYPE, EXTRA_OTHER_TYPE_EXPENDITURE) ==
                            EXTRA_OTHER_TYPE_EXPENDITURE;
                }
            } else {
                cachedIsExpenditure = getArguments() != null && getArguments().getInt(EXTRA_OTHER_TYPE, EXTRA_OTHER_TYPE_EXPENDITURE) ==
                        EXTRA_OTHER_TYPE_EXPENDITURE;
            }
        }
        return cachedIsExpenditure;
    }
}
