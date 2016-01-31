/*
 * Copyright 2013 Jan Kühle
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
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;

import java.util.Date;

import me.kuehle.carreport.R;
import me.kuehle.carreport.data.query.CarQueries;
import me.kuehle.carreport.gui.dialog.SupportColorPickerDialogFragment;
import me.kuehle.carreport.gui.dialog.SupportColorPickerDialogFragment.SupportColorPickerDialogFragmentListener;
import me.kuehle.carreport.gui.dialog.SupportDatePickerDialogFragment.SupportDatePickerDialogFragmentListener;
import me.kuehle.carreport.gui.util.DateTimeInput;
import me.kuehle.carreport.gui.util.FormFieldNotEmptyValidator;
import me.kuehle.carreport.gui.util.FormValidator;
import me.kuehle.carreport.gui.util.SimpleAnimator;
import me.kuehle.carreport.provider.car.CarContentValues;
import me.kuehle.carreport.provider.car.CarCursor;
import me.kuehle.carreport.provider.car.CarSelection;

public class DataDetailCarFragment extends AbstractDataDetailFragment implements
        SupportColorPickerDialogFragmentListener, SupportDatePickerDialogFragmentListener {
    private static final int REQUEST_PICK_COLOR = 1;
    private static final int REQUEST_PICK_SUSPEND_DATE = 2;

    private static final String STATE_COLOR = "color";

    private EditText edtName;
    private View colorIndicator;
    private int color;
    private EditText edtInitialMileage;
    private CheckBox chkSuspend;
    private DateTimeInput edtSuspendDate;
    private SimpleAnimator edtSuspendDateAnimator;
    private TextInputLayout edtSuspendDateInputLayout;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        if (CarQueries.getCount(getActivity()) == 1) {
            menu.removeItem(R.id.menu_delete);
        }
    }

    @Override
    public void onDialogNegativeClick(int requestCode) {
    }

    @Override
    public void onDialogPositiveClick(int requestCode, Date date) {
        if (requestCode == REQUEST_PICK_SUSPEND_DATE) {
            edtSuspendDate.setDate(date);
        }
    }

    @Override
    public void onDialogPositiveClick(int requestCode, int color) {
        if (requestCode == REQUEST_PICK_COLOR) {
            this.color = color;
            colorIndicator.getBackground().setColorFilter(color, PorterDuff.Mode.SRC);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_COLOR, color);
    }

    @Override
    protected void fillFields(Bundle savedInstanceState, View v) {
        if (savedInstanceState == null) {
            if (isInEditMode()) {
                CarCursor car = new CarSelection().id(mId).query(getActivity().getContentResolver());
                car.moveToNext();

                edtName.setText(car.getName());
                color = car.getColor();
                edtInitialMileage.setText(String.valueOf(car.getInitialMileage()));
                chkSuspend.setChecked(car.getSuspendedSince() != null);
                edtSuspendDate.setDate(car.getSuspendedSince() != null ? car.getSuspendedSince() : new Date());
            } else {
                color = getResources().getColor(R.color.accent);
                edtInitialMileage.setText("0");
                edtSuspendDate.setDate(new Date());
            }
        } else {
            color = savedInstanceState.getInt(STATE_COLOR);
        }

        colorIndicator.getBackground().setColorFilter(color, PorterDuff.Mode.SRC);
        if (!chkSuspend.isChecked()) {
            edtSuspendDateInputLayout.getLayoutParams().height = 0;
            edtSuspendDateInputLayout.setAlpha(0);
        }
    }

    @Override
    protected int getAlertDeleteMessage() {
        return R.string.alert_delete_car_message;
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_data_detail_car;
    }

    @Override
    protected int getTitleForEdit() {
        return R.string.title_edit_car;
    }

    @Override
    protected int getTitleForNew() {
        return R.string.title_add_car;
    }

    @Override
    protected void initFields(Bundle savedInstanceState, View v) {
        edtName = (EditText) v.findViewById(R.id.edt_name);
        colorIndicator = v.findViewById(R.id.btn_color);
        edtInitialMileage = (EditText) v.findViewById(R.id.edt_initial_mileage);
        chkSuspend = (CheckBox) v.findViewById(R.id.chk_suspend);
        edtSuspendDate = new DateTimeInput((EditText) v.findViewById(R.id.edt_suspend_date),
                DateTimeInput.Mode.DATE);
        edtSuspendDateInputLayout = (TextInputLayout) v.findViewById(R.id.edt_suspend_date_input_layout);
        edtSuspendDateAnimator = new SimpleAnimator(getActivity(), edtSuspendDateInputLayout,
                SimpleAnimator.Property.Height);

        View rowColor = (View) colorIndicator.getParent();
        rowColor.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SupportColorPickerDialogFragment.newInstance(
                        DataDetailCarFragment.this, REQUEST_PICK_COLOR,
                        R.string.alert_change_color_title, color).show(
                        getFragmentManager(), null);
            }
        });

        chkSuspend.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if (isChecked) {
                    edtSuspendDateAnimator.show();
                } else {
                    edtSuspendDateAnimator.hide();
                }
            }
        });

        edtSuspendDate.applyOnClickListener(DataDetailCarFragment.this, REQUEST_PICK_SUSPEND_DATE,
                getFragmentManager());
    }

    @Override
    protected long save() {
        CarContentValues values = new CarContentValues();
        values.putName(edtName.getText().toString());
        values.putColor(color);
        values.putInitialMileage(getIntegerFromEditText(edtInitialMileage, 0));
        values.putSuspendedSince(chkSuspend.isChecked() ? edtSuspendDate.getDate() : null);

        if (isInEditMode()) {
            CarSelection where = new CarSelection().id(mId);
            values.update(getActivity().getContentResolver(), where);
            return mId;
        } else {
            Uri uri = values.insert(getActivity().getContentResolver());
            return ContentUris.parseId(uri);
        }
    }

    @Override
    protected void delete() {
        new CarSelection().id(mId).delete(getActivity().getContentResolver());
    }

    @Override
    protected boolean validate() {
        FormValidator validator = new FormValidator();
        validator.add(new FormFieldNotEmptyValidator(edtName));

        return validator.validate();
    }
}
