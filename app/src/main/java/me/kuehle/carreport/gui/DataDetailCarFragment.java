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

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;

import com.activeandroid.Model;

import java.util.Date;

import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.gui.dialog.SupportColorPickerDialogFragment;
import me.kuehle.carreport.gui.dialog.SupportColorPickerDialogFragment.SupportColorPickerDialogFragmentListener;
import me.kuehle.carreport.gui.dialog.SupportDatePickerDialogFragment.SupportDatePickerDialogFragmentListener;
import me.kuehle.carreport.gui.util.DateTimeInput;
import me.kuehle.carreport.gui.util.FormFieldNotEmptyValidator;
import me.kuehle.carreport.gui.util.FormValidator;
import me.kuehle.carreport.gui.util.SimpleAnimator;

public class DataDetailCarFragment extends AbstractDataDetailFragment implements
        SupportColorPickerDialogFragmentListener, SupportDatePickerDialogFragmentListener {
    private static final int REQUEST_PICK_COLOR = 1;
    private static final int REQUEST_PICK_SUSPEND_DATE = 2;

    private static final String STATE_COLOR = "color";

    private EditText edtName;
    private View colorIndicator;
    private int color;
    private CheckBox chkSuspend;
    private DateTimeInput edtSuspendDate;
    private SimpleAnimator edtSuspendDateAnimator;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (Car.getCount() == 1) {
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
                Car car = (Car) editItem;

                edtName.setText(car.name);
                color = car.color;
                chkSuspend.setChecked(car.isSuspended());
                edtSuspendDate.setDate(car.isSuspended() ? car.suspendedSince : new Date());
            } else {
                color = getResources().getColor(R.color.accent );
                edtSuspendDate.setDate(new Date());
            }
        } else {
            color = savedInstanceState.getInt(STATE_COLOR);
        }

        colorIndicator.getBackground().setColorFilter(color, PorterDuff.Mode.SRC);
        if (!chkSuspend.isChecked()) {
            edtSuspendDate.getEditText().getLayoutParams().height = 0;
            edtSuspendDate.getEditText().setAlpha(0);
        }
    }

    @Override
    protected int getAlertDeleteMessage() {
        return R.string.alert_delete_car_message;
    }

    @Override
    protected Model getEditItem(long id) {
        return Car.load(Car.class, id);
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
    protected int getToastDeletedMessage() {
        return R.string.toast_car_deleted;
    }

    @Override
    protected int getToastSavedMessage() {
        return R.string.toast_car_saved;
    }

    @Override
    protected void initFields(Bundle savedInstanceState, View v) {
        edtName = (EditText) v.findViewById(R.id.edt_name);
        colorIndicator = v.findViewById(R.id.btn_color);
        chkSuspend = (CheckBox) v.findViewById(R.id.chk_suspend);
        edtSuspendDate = new DateTimeInput((EditText) v.findViewById(R.id.edt_suspend_date),
                DateTimeInput.Mode.DATE);
        edtSuspendDateAnimator = new SimpleAnimator(getActivity(), edtSuspendDate.getEditText(),
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
    protected void save() {
        String name = edtName.getText().toString();
        Date suspended = chkSuspend.isChecked() ? edtSuspendDate.getDate() : null;

        if (!isInEditMode()) {
            new Car(name, color, suspended).save();
        } else {
            Car car = (Car) editItem;
            car.name = name;
            car.color = color;
            car.suspendedSince = suspended;
            car.save();
        }
    }

    @Override
    protected boolean validate() {
        FormValidator validator = new FormValidator();
        validator.add(new FormFieldNotEmptyValidator(edtName));

        return validator.validate();
    }
}
