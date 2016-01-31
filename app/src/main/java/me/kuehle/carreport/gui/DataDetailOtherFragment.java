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

import android.content.ContentUris;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.widget.SimpleCursorAdapter;
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

import java.util.Date;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.data.query.OtherCostQueries;
import me.kuehle.carreport.gui.dialog.SupportDatePickerDialogFragment.SupportDatePickerDialogFragmentListener;
import me.kuehle.carreport.gui.dialog.SupportTimePickerDialogFragment.SupportTimePickerDialogFragmentListener;
import me.kuehle.carreport.gui.util.DateTimeInput;
import me.kuehle.carreport.gui.util.FormFieldGreaterZeroValidator;
import me.kuehle.carreport.gui.util.FormValidator;
import me.kuehle.carreport.gui.util.SimpleAnimator;
import me.kuehle.carreport.provider.car.CarColumns;
import me.kuehle.carreport.provider.car.CarCursor;
import me.kuehle.carreport.provider.car.CarSelection;
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

    private AutoCompleteTextView mEdtTitle;
    private DateTimeInput mEdtDate;
    private DateTimeInput mEdtTime;
    private EditText mEdtMileage;
    private EditText mEdtPrice;
    private Spinner mSpnRepeat;
    private CheckBox mChkEndDate;
    private SimpleAnimator mChkEndDateAnimator;
    private SimpleAnimator mEdtEndDateAnimator;
    private DateTimeInput mEdtEndDate;
    private TextInputLayout mEdtEndDateInputLayout;
    private EditText mEdtNote;
    private Spinner mSpnCar;

    private Boolean mCachedIsExpenditure = null;

    @Override
    public void onDialogPositiveClick(int requestCode, Date date) {
        switch (requestCode) {
            case PICK_DATE_REQUEST_CODE:
                mEdtDate.setDate(date);
                break;
            case PICK_TIME_REQUEST_CODE:
                mEdtTime.setDate(date);
                break;
            case PICK_END_DATE_REQUEST_CODE:
                mEdtEndDate.setDate(date);
                break;
        }
    }

    @Override
    protected void fillFields(Bundle savedInstanceState, View v) {
        if (!isInEditMode()) {
            Preferences prefs = new Preferences(getActivity());

            mEdtDate.setDate(new Date());
            mEdtTime.setDate(new Date());

            mEdtEndDate.setDate(new Date());

            long selectCarId = getArguments().getLong(EXTRA_CAR_ID);
            if (selectCarId == 0) {
                selectCarId = prefs.getDefaultCar();
            }

            for (int pos = 0; pos < mSpnCar.getCount(); pos++) {
                if (mSpnCar.getItemIdAtPosition(pos) == selectCarId) {
                    mSpnCar.setSelection(pos);
                }
            }
        } else {
            OtherCostCursor otherCost = new OtherCostSelection().id(mId).query(getActivity().getContentResolver());
            otherCost.moveToNext();

            mEdtDate.setDate(otherCost.getDate());
            mEdtTime.setDate(otherCost.getDate());
            mEdtTitle.setText(String.valueOf(otherCost.getTitle()));
            if (otherCost.getMileage() != null && otherCost.getMileage() > -1) {
                mEdtMileage.setText(String.valueOf(otherCost.getMileage()));
            }

            if (isExpenditure()) {
                mEdtPrice.setText(String.valueOf(otherCost.getPrice()));
            } else {
                mEdtPrice.setText(String.valueOf(-otherCost.getPrice()));
            }

            mSpnRepeat.setSelection(otherCost.getRecurrenceInterval().ordinal());
            if (otherCost.getRecurrenceInterval() != RecurrenceInterval.ONCE) {
                mChkEndDate.setVisibility(View.VISIBLE);
                if (otherCost.getEndDate() != null) {
                    mChkEndDate.setChecked(true);
                }
            }

            mEdtEndDate.setDate(otherCost.getEndDate() == null ? new Date() : otherCost.getEndDate());
            mEdtNote.setText(otherCost.getNote());

            for (int pos = 0; pos < mSpnCar.getCount(); pos++) {
                if (mSpnCar.getItemIdAtPosition(pos) == otherCost.getCarId()) {
                    mSpnCar.setSelection(pos);
                }
            }
        }

        if (mSpnRepeat.getSelectedItemPosition() == 0) {
            mChkEndDate.getLayoutParams().height = 0;
            mChkEndDate.setAlpha(0);
        }
        if (!mChkEndDate.isChecked()) {
            mEdtEndDateInputLayout.getLayoutParams().height = 0;
            mEdtEndDateInputLayout.setAlpha(0);
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
    protected void initFields(Bundle savedInstanceState, View v) {
        Preferences prefs = new Preferences(getActivity());

        mEdtTitle = (AutoCompleteTextView) v.findViewById(R.id.edt_title);
        mEdtDate = new DateTimeInput((EditText) v.findViewById(R.id.edt_date),
                DateTimeInput.Mode.DATE);
        mEdtTime = new DateTimeInput((EditText) v.findViewById(R.id.edt_time),
                DateTimeInput.Mode.TIME);
        mEdtMileage = (EditText) v.findViewById(R.id.edt_mileage);
        mEdtPrice = (EditText) v.findViewById(R.id.edt_price);
        mSpnRepeat = (Spinner) v.findViewById(R.id.spn_repeat);
        mChkEndDate = (CheckBox) v.findViewById(R.id.chk_end_date);
        mChkEndDateAnimator = new SimpleAnimator(getActivity(), mChkEndDate,
                SimpleAnimator.Property.Height);
        mEdtEndDate = new DateTimeInput((EditText) v.findViewById(R.id.edt_end_date),
                DateTimeInput.Mode.DATE);
        mEdtEndDateInputLayout = (TextInputLayout) v.findViewById(R.id.edt_end_date_input_layout);
        mEdtEndDateAnimator = new SimpleAnimator(getActivity(), mEdtEndDateInputLayout,
                SimpleAnimator.Property.Height);
        mEdtNote = (EditText) v.findViewById(R.id.edt_note);
        mSpnCar = (Spinner) v.findViewById(R.id.spn_car);

        // Title
        mEdtTitle.setAdapter(new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item, android.R.id.text1,
                OtherCostQueries.getTitles(getActivity(), isExpenditure())));

        // Date + Time
        mEdtDate.applyOnClickListener(DataDetailOtherFragment.this, PICK_DATE_REQUEST_CODE,
                getFragmentManager());
        mEdtTime.applyOnClickListener(DataDetailOtherFragment.this, PICK_TIME_REQUEST_CODE,
                getFragmentManager());

        // Units
        addUnitToHint(mEdtMileage, R.string.hint_mileage_optional, prefs.getUnitDistance());
        addUnitToHint(mEdtPrice, R.string.hint_price, prefs.getUnitCurrency());

        // Repeat
        mSpnRepeat.setOnItemSelectedListener(new OnItemSelectedListener() {
            private int mLastPosition = 0;

            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView,
                                       int position, long id) {
                if (position > 0 && mLastPosition == 0) {
                    mChkEndDateAnimator.show();
                } else if (position == 0 && mLastPosition > 0) {
                    mChkEndDate.setChecked(false);
                    mChkEndDateAnimator.hide();
                }

                mLastPosition = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        mChkEndDate.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mEdtEndDateAnimator.show();
                } else {
                    mEdtEndDateAnimator.hide();
                }
            }
        });

        mEdtEndDate.applyOnClickListener(DataDetailOtherFragment.this, PICK_END_DATE_REQUEST_CODE,
                getFragmentManager());

        // Car
        CarCursor car = new CarSelection().query(getActivity().getContentResolver(), null,
                CarColumns.NAME + " COLLATE UNICODE");
        mSpnCar.setAdapter(new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_spinner_dropdown_item,
                car, new String[]{CarColumns.NAME}, new int[]{android.R.id.text1}, 0));
    }

    @Override
    protected long save() {
        float price = (float) getDoubleFromEditText(mEdtPrice, 0);
        if (!isExpenditure()) {
            price *= -1;
        }

        RecurrenceInterval recurrenceInterval = RecurrenceInterval.values()[mSpnRepeat.getSelectedItemPosition()];
        Date endDate = null;
        if (recurrenceInterval != RecurrenceInterval.ONCE && mChkEndDate.isChecked()) {
            endDate = mEdtEndDate.getDate();
        }

        OtherCostContentValues values = new OtherCostContentValues();
        values.putTitle(mEdtTitle.getText().toString().trim());
        values.putDate(DateTimeInput.getDateTime(mEdtDate.getDate(), mEdtTime.getDate()));
        values.putMileage(getIntegerFromEditText(mEdtMileage, -1));
        values.putPrice(price);
        values.putRecurrenceInterval(recurrenceInterval);
        values.putRecurrenceMultiplier(1);
        values.putEndDate(endDate);
        values.putNote(mEdtNote.getText().toString().trim());
        values.putCarId(mSpnCar.getSelectedItemId());

        if (isInEditMode()) {
            OtherCostSelection where = new OtherCostSelection().id(mId);
            values.update(getActivity().getContentResolver(), where);
            return mId;
        } else {
            Uri uri = values.insert(getActivity().getContentResolver());
            return ContentUris.parseId(uri);
        }
    }

    @Override
    protected void delete() {
        new OtherCostSelection().id(mId).delete(getActivity().getContentResolver());
    }

    @Override
    protected boolean validate() {
        FormValidator validator = new FormValidator();
        validator.add(new FormFieldGreaterZeroValidator(mEdtPrice));
        return validator.validate();
    }

    private boolean isExpenditure() {
        if (mCachedIsExpenditure == null) {
            if (isInEditMode()) {

                OtherCostCursor otherCost = new OtherCostSelection().id(mId).query(getActivity().getContentResolver());
                otherCost.moveToNext();

                mCachedIsExpenditure = otherCost.getPrice() > 0;
            } else {
                mCachedIsExpenditure = getArguments().getInt(EXTRA_OTHER_TYPE, EXTRA_OTHER_TYPE_EXPENDITURE) ==
                        EXTRA_OTHER_TYPE_EXPENDITURE;
            }
        }

        return mCachedIsExpenditure;
    }
}