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
import org.juanro.autumandu.gui.adapter.CarArrayAdapter;
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
        setupResultListeners();
        configureMode();
    }

    private void configureMode() {
        if (isInEditMode()) {
            loadExistingData();
        } else {
            setupNewItemDefaults();
        }
    }

    private void setupResultListeners() {
        getParentFragmentManager().setFragmentResultListener(DatePickerDialogFragment.REQUEST_KEY, getViewLifecycleOwner(), (requestKey, result) -> {
            int requestCode = result.getInt(DatePickerDialogFragment.RESULT_REQUEST_CODE);
            var date = new Date(result.getLong(DatePickerDialogFragment.RESULT_DATE));
            if (requestCode == PICK_DATE_REQUEST_CODE) {
                edtDate.setDate(date);
            } else if (requestCode == PICK_END_DATE_REQUEST_CODE) {
                edtEndDate.setDate(date);
            }
        });

        getParentFragmentManager().setFragmentResultListener(TimePickerDialogFragment.REQUEST_KEY, getViewLifecycleOwner(), (requestKey, result) -> {
            int requestCode = result.getInt(TimePickerDialogFragment.RESULT_REQUEST_CODE);
            if (requestCode == PICK_TIME_REQUEST_CODE) {
                edtTime.setDate(new Date(result.getLong(TimePickerDialogFragment.RESULT_TIME)));
            }
        });
    }

    private void loadExistingData() {
        viewModel.getOtherCost().observe(getViewLifecycleOwner(), otherCost -> {
            if (otherCost == null) return;

            cachedIsExpenditure = otherCost.getPrice() > 0;
            updateFieldsFromOtherCost(otherCost);

            mInitialCarId = otherCost.getCarId();
            selectSpinnerItemById(spnCar, mInitialCarId);

            updateInitialAnimatorStates();
        });
    }

    private void updateFieldsFromOtherCost(OtherCost otherCost) {
        edtDate.setDate(otherCost.getDate());
        edtTime.setDate(otherCost.getDate());
        edtTitle.setText(otherCost.getTitle());
        if (otherCost.getMileage() != null && otherCost.getMileage() > -1) {
            edtMileage.setText(String.valueOf(otherCost.getMileage()));
        }

        float displayPrice = isExpenditure() ? otherCost.getPrice() : -otherCost.getPrice();
        edtPrice.setText(String.valueOf(displayPrice));

        spnRepeat.setSelection(otherCost.getRecurrenceInterval().ordinal());
        updateRecurrenceFieldsVisibility(false);

        edtEndDate.setDate(otherCost.getEndDate() == null ? new Date() : otherCost.getEndDate());
        edtNote.setText(otherCost.getNote());
    }

    private void updateInitialAnimatorStates() {
        if (spnRepeat.getSelectedItemPosition() == 0) {
            chkEndDateAnimator.hide();
        }
        if (!chkEndDate.isChecked()) {
            edtEndDateAnimator.hide();
        }
    }

    private void setupNewItemDefaults() {
        edtDate.setDate(new Date());
        edtTime.setDate(new Date());
        edtEndDate.setDate(new Date());
        updateRecurrenceFieldsVisibility(false);
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
        initViewReferences(v);
        setupTitleAutoCompletion();
        setupDateTimePickers();
        setupUnits(prefs);
        setupRecurrenceListeners();
        setupCarSpinner(prefs);
    }

    private void initViewReferences(View v) {
        edtTitle = v.findViewById(R.id.edt_title);
        edtDate = new DateTimeInput(v.findViewById(R.id.edt_date), DateTimeInput.Mode.DATE);
        edtTime = new DateTimeInput(v.findViewById(R.id.edt_time), DateTimeInput.Mode.TIME);
        edtMileage = v.findViewById(R.id.edt_mileage);
        edtPrice = v.findViewById(R.id.edt_price);
        spnRepeat = v.findViewById(R.id.spn_repeat);
        chkEndDate = v.findViewById(R.id.chk_end_date);
        chkEndDateAnimator = new SimpleAnimator(getActivity(), chkEndDate, SimpleAnimator.Property.HEIGHT);
        edtEndDate = new DateTimeInput(v.findViewById(R.id.edt_end_date), DateTimeInput.Mode.DATE);
        TextInputLayout edtEndDateInputLayout = v.findViewById(R.id.edt_end_date_input_layout);
        edtEndDateAnimator = new SimpleAnimator(getActivity(), edtEndDateInputLayout, SimpleAnimator.Property.HEIGHT);
        edtNote = v.findViewById(R.id.edt_note);
        spnCar = v.findViewById(R.id.spn_car);
    }

    private void setupTitleAutoCompletion() {
        viewModel.getTitles().observe(getViewLifecycleOwner(), titles ->
                edtTitle.setAdapter(new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_dropdown_item, titles.toArray(new String[0]))));
    }

    private void setupDateTimePickers() {
        edtDate.applyOnClickListener(PICK_DATE_REQUEST_CODE, getParentFragmentManager());
        edtTime.applyOnClickListener(PICK_TIME_REQUEST_CODE, getParentFragmentManager());
        edtEndDate.applyOnClickListener(PICK_END_DATE_REQUEST_CODE, getParentFragmentManager());
    }

    private void setupUnits(Preferences prefs) {
        addUnitToHint(edtMileage, R.string.hint_mileage_optional, prefs.getUnitDistance());
        addUnitToHint(edtPrice, R.string.hint_price, prefs.getUnitCurrency());
    }

    private void setupRecurrenceListeners() {
        spnRepeat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateRecurrenceFieldsVisibility(true);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Not used
            }
        });

        chkEndDate.setOnCheckedChangeListener((buttonView, isChecked) -> updateRecurrenceFieldsVisibility(true));
    }

    private void setupCarSpinner(Preferences prefs) {
        viewModel.getCars().observe(getViewLifecycleOwner(), cars -> {
            spnCar.setAdapter(new CarArrayAdapter(requireContext(), cars));
            updateInitialCarSelection(prefs);
        });

        spnCar.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Car selection doesn't affect defaults in OtherCost
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Not used
            }
        });
    }

    private void updateInitialCarSelection(Preferences prefs) {
        if (!isInEditMode()) {
            mInitialCarId = getArguments() != null ? getArguments().getLong(EXTRA_CAR_ID) : 0;
            if (mInitialCarId == 0) {
                mInitialCarId = prefs.getDefaultCar();
            }
        }

        if (mInitialCarId != -1) {
            selectSpinnerItemById(spnCar, mInitialCarId);
        }
    }

    private void updateRecurrenceFieldsVisibility(boolean animated) {
        boolean repeat = spnRepeat.getSelectedItemPosition() > 0;
        updateEndDateCheckboxVisibility(repeat, animated);
        updateEndDateInputVisibility(repeat, animated);
    }

    private void updateEndDateCheckboxVisibility(boolean repeat, boolean animated) {
        if (repeat) {
            if (animated) chkEndDateAnimator.show();
            else chkEndDate.setVisibility(View.VISIBLE);
        } else {
            chkEndDate.setChecked(false);
            if (animated) chkEndDateAnimator.hide();
            else chkEndDate.setVisibility(View.GONE);
        }
    }

    private void updateEndDateInputVisibility(boolean repeat, boolean animated) {
        if (repeat && chkEndDate.isChecked()) {
            if (animated) edtEndDateAnimator.show();
            else {
                View view = findView(R.id.edt_end_date_input_layout);
                if (view != null) view.setVisibility(View.VISIBLE);
            }
        } else {
            if (animated) edtEndDateAnimator.hide();
            else {
                View view = findView(R.id.edt_end_date_input_layout);
                if (view != null) view.setVisibility(View.GONE);
            }
        }
    }

    private View findView(int id) {
        return getView() != null ? getView().findViewById(id) : null;
    }

    @Override
    protected void saveAsync() {
        OtherCost otherCost = createOtherCostFromFields();
        viewModel.save(otherCost, () -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    ReminderWorker.enqueueUpdate(requireContext());
                    mOnItemActionListener.onItemSavedAsync(otherCost.getId());
                });
            }
        });
    }

    private OtherCost createOtherCostFromFields() {
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
        return otherCost;
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
        return 0; // Using saveAsync
    }

    @Override
    protected void delete() {
        // Using deleteAsync
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
