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
import android.support.design.widget.TextInputLayout;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.Date;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.gui.dialog.SupportDatePickerDialogFragment.SupportDatePickerDialogFragmentListener;
import me.kuehle.carreport.gui.dialog.SupportTimePickerDialogFragment.SupportTimePickerDialogFragmentListener;
import me.kuehle.carreport.gui.util.DateTimeInput;
import me.kuehle.carreport.gui.util.FormFieldGreaterZeroValidator;
import me.kuehle.carreport.gui.util.FormValidator;
import me.kuehle.carreport.gui.util.SimpleAnimator;
import me.kuehle.carreport.provider.car.CarColumns;
import me.kuehle.carreport.provider.car.CarCursor;
import me.kuehle.carreport.provider.car.CarSelection;
import me.kuehle.carreport.provider.othercost.OtherCostColumns;
import me.kuehle.carreport.provider.othercost.OtherCostContentValues;
import me.kuehle.carreport.provider.othercost.OtherCostCursor;
import me.kuehle.carreport.provider.othercost.OtherCostSelection;
import me.kuehle.carreport.provider.othercost.RecurrenceInterval;

public class DataDetailOtherFragment extends AbstractDataDetailFragment
        implements SupportDatePickerDialogFragmentListener,
        SupportTimePickerDialogFragmentListener {
    public static final String EXTRA_OTHER_TYPE = "other_type";
    public static final int EXTRA_OTHER_TYPE_EXPENDITURE = 0;
    public static final int EXTRA_OTHER_TYPE_INCOME = 1;

    private static final int PICK_DATE_REQUEST_CODE = 0;
    private static final int PICK_TIME_REQUEST_CODE = 1;
    private static final int PICK_END_DATE_REQUEST_CODE = 2;

    /**
     * Creates a new fragment to edit an existing other cost entry.
     *
     * @param id The is of the item to edit.
     * @return A new edit fragment.
     */
    public static DataDetailOtherFragment newInstance(long id) {
        DataDetailOtherFragment f = new DataDetailOtherFragment();

        Bundle args = new Bundle();
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
    private TextInputLayout edtEndDateInputLayout;
    private EditText edtNote;
    private Spinner spnCar;

    private Boolean isExpenditureCached = null;

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

            long selectCarId = getArguments().getLong(EXTRA_CAR_ID);
            if (selectCarId == 0) {
                selectCarId = prefs.getDefaultCar();
            }

            for (int pos = 0; pos < spnCar.getCount(); pos++) {
                if (spnCar.getItemIdAtPosition(pos) == selectCarId) {
                    spnCar.setSelection(pos);
                }
            }
        } else {
            OtherCostCursor otherCost = new OtherCostSelection().id(mId).query(getActivity().getContentResolver());
            otherCost.moveToNext();

            edtDate.setDate(otherCost.getDate());
            edtTime.setDate(otherCost.getDate());
            edtTitle.setText(String.valueOf(otherCost.getTitle()));
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
                chkEndDate.setVisibility(View.VISIBLE);
                if (otherCost.getEndDate() != null) {
                    chkEndDate.setChecked(true);
                }
            }

            edtEndDate.setDate(otherCost.getEndDate() == null ? new Date() : otherCost.getEndDate());
            edtNote.setText(otherCost.getNote());

            for (int pos = 0; pos < spnCar.getCount(); pos++) {
                if (spnCar.getItemIdAtPosition(pos) == otherCost.getCarId()) {
                    spnCar.setSelection(pos);
                }
            }
        }

        if (spnRepeat.getSelectedItemPosition() == 0) {
            chkEndDate.getLayoutParams().height = 0;
            chkEndDate.setAlpha(0);
        }
        if (!chkEndDate.isChecked()) {
            edtEndDateInputLayout.getLayoutParams().height = 0;
            edtEndDateInputLayout.setAlpha(0);
        }
    }

    @Override
    protected int getAlertDeleteMessage() {
        if (isExpenditure()) {
            return R.string.alert_delete_other_expenditure_message;
        } else {
            return R.string.alert_delete_other_income_message;
        }
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_data_detail_other;
    }

    @Override
    protected int getTitleForEdit() {
        if (isExpenditure()) {
            return R.string.title_edit_other_expenditure;
        } else {
            return R.string.title_edit_other_income;
        }
    }

    @Override
    protected int getTitleForNew() {
        if (isExpenditure()) {
            return R.string.title_add_other_expenditure;
        } else {
            return R.string.title_add_other_income;
        }
    }

    @Override
    protected int getToastDeletedMessage() {
        if (isExpenditure()) {
            return R.string.toast_other_expenditure_deleted;
        } else {
            return R.string.toast_other_income_deleted;
        }
    }

    @Override
    protected int getToastSavedMessage() {
        if (isExpenditure()) {
            return R.string.toast_other_expenditure_saved;
        } else {
            return R.string.toast_other_income_saved;
        }
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
        edtEndDateInputLayout = (TextInputLayout) v.findViewById(R.id.edt_end_date_input_layout);
        edtEndDateAnimator = new SimpleAnimator(getActivity(), edtEndDateInputLayout,
                SimpleAnimator.Property.Height);
        edtNote = (EditText) v.findViewById(R.id.edt_note);
        spnCar = (Spinner) v.findViewById(R.id.spn_car);

        // Title
        OtherCostSelection otherCostTitleQuery = new OtherCostSelection();
        if (isExpenditure()) {
            otherCostTitleQuery.priceGt(0);
        } else {
            otherCostTitleQuery.priceLt(0);
        }

        OtherCostCursor otherCostTitles = otherCostTitleQuery.query(getActivity().getContentResolver(),
                new String[]{OtherCostColumns._ID, OtherCostColumns.TITLE},
                OtherCostColumns.TITLE + " COLLATE UNICODE ASC");
        edtTitle.setAdapter(new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_spinner_dropdown_item,
                otherCostTitles, new String[]{OtherCostColumns.TITLE}, new int[]{android.R.id.text1}, 0));

        // Date + Time
        edtDate.applyOnClickListener(DataDetailOtherFragment.this, PICK_DATE_REQUEST_CODE,
                getFragmentManager());
        edtTime.applyOnClickListener(DataDetailOtherFragment.this, PICK_TIME_REQUEST_CODE,
                getFragmentManager());

        // Units
        addUnitToHint(edtMileage, R.string.hint_mileage_optional, prefs.getUnitDistance());
        addUnitToHint(edtPrice, R.string.hint_price, prefs.getUnitCurrency());

        // Repeat
        spnRepeat.setOnItemSelectedListener(new OnItemSelectedListener() {
            private int mLastPosition = 0;

            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView,
                                       int position, long id) {
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
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    edtEndDateAnimator.show();
                } else {
                    edtEndDateAnimator.hide();
                }
            }
        });

        edtEndDate.applyOnClickListener(DataDetailOtherFragment.this, PICK_END_DATE_REQUEST_CODE,
                getFragmentManager());

        // Car
        CarCursor car = new CarSelection().query(getActivity().getContentResolver());
        spnCar.setAdapter(new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_spinner_dropdown_item,
                car, new String[]{CarColumns.NAME}, new int[]{android.R.id.text1}, 0));
    }

    @Override
    protected void save() {
        float price = (float) getDoubleFromEditText(edtPrice, 0);
        if (!isExpenditure()) {
            price *= -1;
        }

        RecurrenceInterval recurrenceInterval = RecurrenceInterval.values()[spnRepeat.getSelectedItemPosition()];
        Date endDate = null;
        if (recurrenceInterval != RecurrenceInterval.ONCE && chkEndDate.isChecked()) {
            endDate = edtEndDate.getDate();
        }

        OtherCostContentValues values = new OtherCostContentValues();
        values.putTitle(edtTitle.getText().toString().trim());
        values.putDate(DateTimeInput.getDateTime(edtDate.getDate(), edtTime.getDate()));
        values.putMileage(getIntegerFromEditText(edtMileage, -1));
        values.putPrice(price);
        values.putRecurrenceInterval(recurrenceInterval);
        values.putRecurrenceMultiplier(1);
        values.putEndDate(endDate);
        values.putNote(edtNote.getText().toString().trim());
        values.putCarId(spnCar.getSelectedItemId());

        if (isInEditMode()) {
            OtherCostSelection where = new OtherCostSelection().id(mId);
            values.update(getActivity().getContentResolver(), where);
        } else {
            values.insert(getActivity().getContentResolver());
        }
    }

    @Override
    protected void delete() {
        new OtherCostSelection().id(mId).delete(getActivity().getContentResolver());
    }

    @Override
    protected boolean validate() {
        FormValidator validator = new FormValidator();
        validator.add(new FormFieldGreaterZeroValidator(edtPrice));
        return validator.validate();
    }

    private boolean isExpenditure() {
        if (isExpenditureCached == null) {
            if (isInEditMode()) {

                OtherCostCursor otherCost = new OtherCostSelection().id(mId).query(getActivity().getContentResolver());
                otherCost.moveToNext();

                isExpenditureCached = otherCost.getPrice() > 0;
            } else {
                isExpenditureCached = getArguments().getInt(EXTRA_OTHER_TYPE, EXTRA_OTHER_TYPE_EXPENDITURE) ==
                        EXTRA_OTHER_TYPE_EXPENDITURE;
            }
        }

        return isExpenditureCached;
    }
}