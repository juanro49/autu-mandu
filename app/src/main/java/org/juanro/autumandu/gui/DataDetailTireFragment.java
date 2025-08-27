/*
 * Copyright 2025 Juanro49
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

package org.juanro.autumandu.gui;

import android.content.ContentUris;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.cursoradapter.widget.SimpleCursorAdapter;

import com.google.android.material.textfield.TextInputLayout;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.dialog.SupportDatePickerDialogFragment.SupportDatePickerDialogFragmentListener;
import org.juanro.autumandu.gui.dialog.SupportTimePickerDialogFragment.SupportTimePickerDialogFragmentListener;
import org.juanro.autumandu.gui.util.DateTimeInput;
import org.juanro.autumandu.gui.util.FormFieldGreaterZeroValidator;
import org.juanro.autumandu.gui.util.FormValidator;
import org.juanro.autumandu.gui.util.SimpleAnimator;
import org.juanro.autumandu.presentation.CarPresenter;
import org.juanro.autumandu.presentation.TirePresenter;
import org.juanro.autumandu.provider.car.CarColumns;
import org.juanro.autumandu.provider.car.CarCursor;
import org.juanro.autumandu.provider.car.CarSelection;
import org.juanro.autumandu.provider.tirelist.TireListContentValues;
import org.juanro.autumandu.provider.tirelist.TireListCursor;
import org.juanro.autumandu.provider.tirelist.TireListSelection;
import org.juanro.autumandu.provider.tireusage.TireUsageContentValues;
import org.juanro.autumandu.provider.tireusage.TireUsageSelection;

import java.util.Date;

public class DataDetailTireFragment extends AbstractDataDetailFragment
        implements SupportDatePickerDialogFragmentListener,
        SupportTimePickerDialogFragmentListener {
    private static final int PICK_DATE_REQUEST_CODE = 0;
    private static final int PICK_TIME_REQUEST_CODE = 1;
    private static final int PICK_TRASH_DATE_REQUEST_CODE = 2;
    private static final int PICK_MOUNT_DATE_REQUEST_CODE = 3;
    private static final int PICK_UMOUNT_DATE_REQUEST_CODE = 4;

    /**
     * Creates a new fragment to edit an existing tire entry.
     *
     * @param id The is of the item to edit.
     * @return A new edit fragment.
     */
    public static DataDetailTireFragment newInstance(long id) {
        DataDetailTireFragment f = new DataDetailTireFragment();

        Bundle args = new Bundle();
        args.putLong(AbstractDataDetailFragment.EXTRA_ID, id);
        f.setArguments(args);

        return f;
    }

    private EditText mEdtManufacturer;
    private EditText mEdtModel;
    private EditText mEdtQuantity;
    private EditText mEdtPrice;
    private EditText mEdtNote;
    private DateTimeInput mEdtDate;
    private DateTimeInput mEdtTime;
    private CheckBox mChkTrashDate;
    private SimpleAnimator mEdtTrashDateAnimator;
    private SimpleAnimator mChkTrashDateAnimator;
    private DateTimeInput mEdtTrashDate;
    private TextInputLayout mEdtTrashDateInputLayout;
    private CheckBox mChkMount;
    private EditText mEdtMountDistance;
    private DateTimeInput mEdtMountDate;
    private SimpleAnimator mEdtMountDistanceAnimator;
    private SimpleAnimator mEdtMountDateAnimator;
    private SimpleAnimator mChkMountAnimator;
    private TextInputLayout mEdtMountDistanceInputLayout;
    private TextInputLayout mEdtMountDateInputLayout;
    private CheckBox mChkUmount;
    private EditText mEdtUmountDistance;
    private DateTimeInput mEdtUmountDate;
    private SimpleAnimator mEdtUmountDistanceAnimator;
    private SimpleAnimator mEdtUmountDateAnimator;
    private SimpleAnimator mChkUmountAnimator;
    private TextInputLayout mEdtUmountDistanceInputLayout;
    private TextInputLayout mEdtUmountDateInputLayout;
    private Spinner mSpnCar;
    private int carNumTires;
    private int numTiresToMount;

    @Override
    public void onDialogPositiveClick(int requestCode, Date date) {
        switch (requestCode) {
            case PICK_DATE_REQUEST_CODE:
                mEdtDate.setDate(date);
                break;
            case PICK_TIME_REQUEST_CODE:
                mEdtTime.setDate(date);
                break;
            case PICK_TRASH_DATE_REQUEST_CODE:
                mEdtTrashDate.setDate(date);
            case PICK_MOUNT_DATE_REQUEST_CODE:
                mEdtMountDate.setDate(date);
            case PICK_UMOUNT_DATE_REQUEST_CODE:
                mEdtUmountDate.setDate(date);
        }
    }

    @Override
    protected void fillFields(Bundle savedInstanceState, View v) {
        if (!isInEditMode()) {
            Preferences prefs = new Preferences(getActivity());

            mEdtDate.setDate(new Date());
            mEdtTime.setDate(new Date());
            mEdtTrashDate.setDate(new Date());
            mEdtMountDate.setDate(new Date());
            mEdtUmountDate.setDate(new Date());
            mChkMount.setVisibility(View.INVISIBLE);
            mChkMountAnimator.hide();
            mChkUmount.setVisibility(View.INVISIBLE);
            mChkUmountAnimator.hide();
            mChkTrashDate.setVisibility(View.INVISIBLE);
            mChkTrashDateAnimator.hide();

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
            TireListCursor tireList = new TireListSelection().id(mId).query(getActivity().getContentResolver());
            TirePresenter mTire = TirePresenter.getInstance(getActivity());
            CarPresenter mCar = CarPresenter.getInstance(getActivity());
            tireList.moveToNext();

            carNumTires = mCar.getCarNumTires(tireList.getCarId());
            numTiresToMount = mCar.getCarNumMountedTires(tireList.getCarId()) + tireList.getQuantity();

            mEdtDate.setDate(tireList.getBuyDate());
            mEdtTime.setDate(tireList.getBuyDate());
            mEdtManufacturer.setText(tireList.getManufacturer());
            mEdtModel.setText(tireList.getModel());
            mEdtQuantity.setText(String.valueOf(tireList.getQuantity()));
            mEdtPrice.setText(String.valueOf(tireList.getPrice()));
            mEdtNote.setText(tireList.getNote());

            if (mTire.getState(mId) == 1) {
                mChkMount.setVisibility(View.INVISIBLE);
                mChkTrashDate.setVisibility(View.INVISIBLE);
                mChkMountAnimator.hide();
                mChkTrashDateAnimator.hide();
            }
            mEdtMountDistance.setText(String.valueOf(CarPresenter.getInstance(this.getContext()).getLatestMileage(tireList.getCarId())));
            mEdtMountDate.setDate(new Date());

            if (mTire.getState(mId) == 0) {
                mChkUmount.setVisibility(View.INVISIBLE);
                mChkUmountAnimator.hide();
            }
            mEdtUmountDistance.setText(String.valueOf(CarPresenter.getInstance(this.getContext()).getLatestMileage(tireList.getCarId())));
            mEdtUmountDate.setDate(new Date());


            if (tireList.getTrashDate() != null) {
                mChkMount.setVisibility(View.INVISIBLE);
                mChkUmount.setVisibility(View.INVISIBLE);
                mChkMountAnimator.hide();
                mChkUmountAnimator.hide();
                mChkTrashDate.setChecked(true);
            }
            mEdtTrashDate.setDate(tireList.getTrashDate() == null ? new Date() : tireList.getTrashDate());

            for (int pos = 0; pos < mSpnCar.getCount(); pos++) {
                if (mSpnCar.getItemIdAtPosition(pos) == tireList.getCarId()) {
                    mSpnCar.setSelection(pos);
                }
            }
        }

        if (!mChkMount.isChecked()) {
            mEdtMountDistanceInputLayout.getLayoutParams().height = 0;
            mEdtMountDistanceInputLayout.setAlpha(0);
            mEdtMountDateInputLayout.getLayoutParams().height = 0;
            mEdtMountDateInputLayout.setAlpha(0);
        }

        if (!mChkUmount.isChecked()) {
            mEdtUmountDistanceInputLayout.getLayoutParams().height = 0;
            mEdtUmountDistanceInputLayout.setAlpha(0);
            mEdtUmountDateInputLayout.getLayoutParams().height = 0;
            mEdtUmountDateInputLayout.setAlpha(0);
        }

        if (!mChkTrashDate.isChecked()) {
            mEdtTrashDateInputLayout.getLayoutParams().height = 0;
            mEdtTrashDateInputLayout.setAlpha(0);
        }
    }

    @Override
    protected int getAlertDeleteMessage() {
        return R.string.alert_delete_tire_message;
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_data_detail_tire;
    }

    @Override
    protected int getTitleForEdit() {
        return R.string.title_edit_tire;
    }

    @Override
    protected int getTitleForNew() {
        return R.string.title_add_tire;
    }

    @Override
    protected void initFields(Bundle savedInstanceState, View v) {
        Preferences prefs = new Preferences(getActivity());

        mEdtManufacturer = (EditText) v.findViewById(R.id.edt_manufacturer);
        mEdtModel = (EditText) v.findViewById(R.id.edt_model);
        mEdtDate = new DateTimeInput((EditText) v.findViewById(R.id.edt_date),
                DateTimeInput.Mode.DATE);
        mEdtTime = new DateTimeInput((EditText) v.findViewById(R.id.edt_time),
                DateTimeInput.Mode.TIME);
        mEdtQuantity = (EditText) v.findViewById(R.id.edt_quantity);
        mEdtPrice = (EditText) v.findViewById(R.id.edt_price);
        mEdtNote = (EditText) v.findViewById(R.id.edt_note);
        mSpnCar = (Spinner) v.findViewById(R.id.spn_car);
        mChkMount = (CheckBox) v.findViewById(R.id.chk_mount);
        mEdtMountDistance = (EditText) v.findViewById(R.id.edt_mount_distance);
        mEdtMountDate = new DateTimeInput((EditText) v.findViewById(R.id.edt_mount_date), DateTimeInput.Mode.DATE);
        mEdtMountDistanceInputLayout = (TextInputLayout) v.findViewById(R.id.edt_mount_distance_input_layout);
        mEdtMountDateInputLayout = (TextInputLayout) v.findViewById(R.id.edt_mount_date_input_layout);
        mEdtMountDistanceAnimator = new SimpleAnimator(getActivity(), mEdtMountDistanceInputLayout,
            SimpleAnimator.Property.Height);
        mEdtMountDateAnimator = new SimpleAnimator(getActivity(), mEdtMountDateInputLayout,
            SimpleAnimator.Property.Height);
        mChkUmount = (CheckBox) v.findViewById(R.id.chk_umount);
        mEdtUmountDistance = (EditText) v.findViewById(R.id.edt_umount_distance);
        mEdtUmountDate = new DateTimeInput((EditText) v.findViewById(R.id.edt_umount_date), DateTimeInput.Mode.DATE);
        mEdtUmountDistanceInputLayout = (TextInputLayout) v.findViewById(R.id.edt_umount_distance_input_layout);
        mEdtUmountDateInputLayout = (TextInputLayout) v.findViewById(R.id.edt_umount_date_input_layout);
        mEdtUmountDistanceAnimator = new SimpleAnimator(getActivity(), mEdtUmountDistanceInputLayout,
            SimpleAnimator.Property.Height);
        mChkMountAnimator = new SimpleAnimator(getActivity(), mChkMount,
            SimpleAnimator.Property.Height);
        mEdtUmountDateAnimator = new SimpleAnimator(getActivity(), mEdtUmountDateInputLayout,
            SimpleAnimator.Property.Height);
        mChkUmountAnimator = new SimpleAnimator(getActivity(), mChkUmount,
            SimpleAnimator.Property.Height);
        mChkTrashDate = (CheckBox) v.findViewById(R.id.chk_trash_date);
        mEdtTrashDate = new DateTimeInput((EditText) v.findViewById(R.id.edt_trash_date), DateTimeInput.Mode.DATE);
        mEdtTrashDateInputLayout = (TextInputLayout) v.findViewById(R.id.edt_trash_date_input_layout);
        mEdtTrashDateAnimator = new SimpleAnimator(getActivity(), mEdtTrashDateInputLayout,
            SimpleAnimator.Property.Height);
        mChkTrashDateAnimator = new SimpleAnimator(getActivity(), mChkTrashDate,
            SimpleAnimator.Property.Height);

        // Date + Time
        mEdtDate.applyOnClickListener(DataDetailTireFragment.this, PICK_DATE_REQUEST_CODE, getFragmentManager());
        mEdtTime.applyOnClickListener(DataDetailTireFragment.this, PICK_TIME_REQUEST_CODE, getFragmentManager());

        // Units
        addUnitToHint(mEdtPrice, R.string.hint_price, prefs.getUnitCurrency());

        mChkMount.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (carNumTires < numTiresToMount)
                    {
                        mChkMount.setChecked(false);
                        Toast.makeText(requireContext(), requireContext().getString(R.string.toast_all_tires_mounted), Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        mChkTrashDate.setChecked(false);
                        mEdtTrashDateAnimator.hide();
                        mEdtTrashDateAnimator.hide();
                        mChkTrashDateAnimator.hide();
                        mEdtMountDistanceAnimator.show();
                        mEdtMountDateAnimator.show();
                    }
                } else {
                    mEdtMountDistanceAnimator.hide();
                    mEdtMountDateAnimator.hide();
                    mChkTrashDateAnimator.show();
                }
            }
        });

        mChkUmount.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mEdtUmountDistanceAnimator.show();
                    mEdtUmountDateAnimator.show();
                } else {
                    mEdtUmountDistanceAnimator.hide();
                    mEdtUmountDateAnimator.hide();
                }
            }
        });

        mChkTrashDate.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mEdtTrashDateAnimator.show();
                } else {
                    mEdtTrashDateAnimator.hide();
                }
            }
        });

        mEdtMountDate.applyOnClickListener(DataDetailTireFragment.this, PICK_MOUNT_DATE_REQUEST_CODE, getFragmentManager());
        mEdtUmountDate.applyOnClickListener(DataDetailTireFragment.this, PICK_UMOUNT_DATE_REQUEST_CODE, getFragmentManager());
        mEdtTrashDate.applyOnClickListener(DataDetailTireFragment.this, PICK_TRASH_DATE_REQUEST_CODE, getFragmentManager());

        // Car
        CarCursor car = new CarSelection().suspendedSince((Date) null).query(getActivity().getContentResolver(), null,
                CarColumns.NAME + " COLLATE UNICODE");
        mSpnCar.setAdapter(new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_spinner_dropdown_item,
                car, new String[]{CarColumns.NAME}, new int[]{android.R.id.text1}, 0));
    }

    @Override
    protected long save() {
        Date trashDate = null;
        if (mChkTrashDate.isChecked()) {
            trashDate = mEdtTrashDate.getDate();
        }

        TireListContentValues values = new TireListContentValues();
        values.putManufacturer(mEdtManufacturer.getText().toString().trim());
        values.putModel(mEdtModel.getText().toString().trim());
        values.putBuyDate(DateTimeInput.getDateTime(mEdtDate.getDate(), mEdtTime.getDate()));
        values.putQuantity(getIntegerFromEditText(mEdtQuantity, 4));
        values.putPrice((float) getDoubleFromEditText(mEdtPrice, 0));
        values.putTrashDate(trashDate);
        values.putNote(mEdtNote.getText().toString().trim());
        values.putCarId(mSpnCar.getSelectedItemId());

        if (isInEditMode()) {
            TireUsageContentValues valuesUsage = new TireUsageContentValues();
            TireListSelection where = new TireListSelection().id(mId);
            values.update(getActivity().getContentResolver(), where);

            // Mount and Umount section
            if(mChkMount.isChecked())
            {
                valuesUsage.putDateMount(mEdtMountDate.getDate());
                valuesUsage.putDistanceMount(getIntegerFromEditText(mEdtMountDistance, 0));
                valuesUsage.putDateUmount(null);
                valuesUsage.putDistanceUmount(getIntegerFromEditText(mEdtMountDistance, 0));
                valuesUsage.putTireId(mId);

                Uri uri = valuesUsage.insert(getActivity().getContentResolver());

                return ContentUris.parseId(uri);
            }
            else if (mChkUmount.isChecked())
            {
                TireUsageSelection whereUsage = new TireUsageSelection().tireIdNotUmount(mId);
                valuesUsage.putDateUmount(mEdtUmountDate.getDate());
                valuesUsage.putDistanceUmount(getIntegerFromEditText(mEdtUmountDistance, 0));

                valuesUsage.update(getActivity().getContentResolver(), whereUsage);
            }

            return mId;
        } else {
            Uri uri = values.insert(getActivity().getContentResolver());
            return ContentUris.parseId(uri);
        }
    }

    @Override
    protected void delete() {
        new TireListSelection().id(mId).delete(getActivity().getContentResolver());
    }

    @Override
    protected boolean validate() {
        FormValidator validator = new FormValidator();
        validator.add(new FormFieldGreaterZeroValidator(mEdtQuantity));
        return validator.validate();
    }
}
