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
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.github.dhaval2404.colorpicker.MaterialColorPickerDialog;
import com.github.dhaval2404.colorpicker.model.ColorShape;
import com.github.dhaval2404.colorpicker.model.ColorSwatch;

import java.util.Date;

import org.juanro.autumandu.R;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.viewmodel.CarDetailViewModel;

/**
 * Fragment to edit car details.
 */
public class DataDetailCarFragment extends AbstractDataDetailFragment {
    private EditText edtName;
    private EditText edtInitialMileage;
    private EditText edtNumTires;
    private EditText edtBuyingPrice;
    private View colorPreview;
    private CheckBox chkSuspended;

    private CarDetailViewModel viewModel;
    private Car car;

    @Override
    protected void initFields(Bundle savedInstanceState, View view) {
        edtName = view.findViewById(R.id.edt_name);
        edtInitialMileage = view.findViewById(R.id.edt_initial_mileage);
        edtNumTires = view.findViewById(R.id.edt_num_tires);
        edtBuyingPrice = view.findViewById(R.id.edt_buying_price);
        colorPreview = view.findViewById(R.id.btn_color);
        chkSuspended = view.findViewById(R.id.chk_suspend);

        colorPreview.setOnClickListener(v -> new MaterialColorPickerDialog.Builder(requireContext())
                .setTitle(R.string.title_select_color)
                .setColorShape(ColorShape.CIRCLE)
                .setColorSwatch(ColorSwatch._500)
                .setDefaultColor(car != null ? car.getColor() : R.color.primary)
                .setColorListener((color, colorHex) -> {
                    if (car != null) {
                        car.setColor(color);
                    }
                    androidx.core.view.ViewCompat.setBackgroundTintList(colorPreview, android.content.res.ColorStateList.valueOf(color));
                })
                .show());
    }

    @Override
    protected void fillFields(Bundle savedInstanceState, View view) {
        viewModel = new ViewModelProvider(this).get(CarDetailViewModel.class);
        viewModel.setCarId(mId);
        viewModel.getCar().observe(getViewLifecycleOwner(), car -> {
            this.car = car;
            if (car != null) {
                edtName.setText(car.getName());
                edtInitialMileage.setText(String.valueOf(car.getInitialMileage()));
                edtNumTires.setText(String.valueOf(car.getNumTires()));
                edtBuyingPrice.setText(String.valueOf(car.getBuyingPrice()));
                androidx.core.view.ViewCompat.setBackgroundTintList(colorPreview, android.content.res.ColorStateList.valueOf(car.getColor()));
                chkSuspended.setChecked(car.getSuspendedSince() != null);
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_data_detail_car;
    }

    @Override
    protected int getTitleForNew() {
        return R.string.title_add_car;
    }

    @Override
    protected int getTitleForEdit() {
        return R.string.title_edit_car;
    }

    @Override
    protected int getAlertDeleteMessage() {
        return R.string.alert_delete_car_message;
    }

    @Override
    protected void delete() {
        if (car != null) {
            viewModel.delete(car.getId(), () -> mOnItemActionListener.onItemDeletedAsync());
        }
    }

    @Override
    protected long save() {
        if (car == null) {
            car = new Car();
            car.setColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary));
        }

        car.setName(edtName.getText().toString());
        try {
            car.setInitialMileage(Integer.parseInt(edtInitialMileage.getText().toString()));
        } catch (NumberFormatException e) {
            car.setInitialMileage(0);
        }

        try {
            car.setNumTires(Integer.parseInt(edtNumTires.getText().toString()));
        } catch (NumberFormatException e) {
            car.setNumTires(4);
        }

        try {
            car.setBuyingPrice(Double.parseDouble(edtBuyingPrice.getText().toString()));
        } catch (NumberFormatException e) {
            car.setBuyingPrice(0);
        }

        if (chkSuspended.isChecked()) {
            if (car.getSuspendedSince() == null) {
                car.setSuspendedSince(new Date());
            }
        } else {
            car.setSuspendedSince(null);
        }

        viewModel.save(car, () -> mOnItemActionListener.onItemSavedAsync(car.getId() != null ? car.getId() : 0));
        return 0; // Not used
    }

    @Override
    protected boolean validate() {
        var name = edtName.getText().toString().trim();
        if (name.isEmpty()) {
            edtName.setError(getString(R.string.validate_error_empty));
            return false;
        }
        return true;
    }
}
