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
import android.view.Gravity;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;
import android.content.res.ColorStateList;
import android.widget.ScrollView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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

        colorPreview.setOnClickListener(v -> {
            int[] colors = {
                getResources().getColor(R.color.red, null), getResources().getColor(R.color.pink, null),
                getResources().getColor(R.color.purple, null), getResources().getColor(R.color.deep_purple, null),
                getResources().getColor(R.color.indigo, null), getResources().getColor(R.color.blue, null),
                getResources().getColor(R.color.light_blue, null), getResources().getColor(R.color.cyan, null),
                getResources().getColor(R.color.teal, null), getResources().getColor(R.color.green, null),
                getResources().getColor(R.color.light_green, null), getResources().getColor(R.color.lime, null),
                getResources().getColor(R.color.yellow, null), getResources().getColor(R.color.amber, null),
                getResources().getColor(R.color.orange, null), getResources().getColor(R.color.deep_orange, null),
                getResources().getColor(R.color.brown, null), getResources().getColor(R.color.grey, null),
                getResources().getColor(R.color.dark_grey, null), getResources().getColor(R.color.blue_grey, null),
                getResources().getColor(R.color.white, null), getResources().getColor(R.color.black, null)
            };

            // 1. Crear el contenedor primero
            LinearLayoutCompat container = new LinearLayoutCompat(requireContext());
            container.setOrientation(LinearLayoutCompat.VERTICAL);
            container.setPadding(32, 32, 32, 32);

            // 2. Usar un ScrollView para manejar el scroll
            ScrollView scrollView = new ScrollView(requireContext());
            scrollView.addView(container);

            // 3. Crear el diálogo
            final AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.title_select_color)
                .setView(scrollView)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

            int itemsPerRow = getResources().getInteger(R.integer.color_picker_colors_per_row);
            LinearLayoutCompat currentRow = null;

            for (int i = 0; i < colors.length; i++) {
                if (i % itemsPerRow == 0) {
                    currentRow = new LinearLayoutCompat(requireContext());
                    currentRow.setOrientation(LinearLayoutCompat.HORIZONTAL);
                    currentRow.setGravity(Gravity.CENTER);
                    container.addView(currentRow);
                }

                MaterialCardView card = new MaterialCardView(requireContext());
                int size = (int) getResources().getDimension(R.dimen.color_picker_circle_size);
                int margin = (int) getResources().getDimension(R.dimen.color_picker_circle_margin);
                LinearLayoutCompat.LayoutParams cardParams = new LinearLayoutCompat.LayoutParams(size, size);
                cardParams.setMargins(margin, margin, margin, margin);
                card.setLayoutParams(cardParams);
                card.setRadius(size / 2f);
                card.setCardElevation(4f);
                card.setCardBackgroundColor(colors[i]);

                final int color = colors[i];
                card.setOnClickListener(v1 -> {
                    if (car != null) car.setColor(color);
                    ViewCompat.setBackgroundTintList(colorPreview, ColorStateList.valueOf(color));
                    dialog.dismiss();
                });
                currentRow.addView(card);
            }
            dialog.show();
        });
    }

    @Override
    protected void fillFields(Bundle savedInstanceState, View view) {
        viewModel = new ViewModelProvider(this).get(CarDetailViewModel.class);
        viewModel.setCarId(mId);
        viewModel.getCar().observe(getViewLifecycleOwner(), carEntity -> {
            this.car = carEntity;
            if (carEntity != null) {
                edtName.setText(carEntity.getName());
                edtInitialMileage.setText(String.valueOf(carEntity.getInitialMileage()));
                edtNumTires.setText(String.valueOf(carEntity.getNumTires()));
                edtBuyingPrice.setText(String.valueOf(carEntity.getBuyingPrice()));
                ViewCompat.setBackgroundTintList(colorPreview, ColorStateList.valueOf(carEntity.getColor()));
                chkSuspended.setChecked(carEntity.getSuspendedSince() != null);
            }
        });

        // Hide suspension UI if creating new car
        if (mId == -1) {
            view.findViewById(R.id.txt_section_suspend).setVisibility(View.GONE);
            view.findViewById(R.id.txt_description_suspend).setVisibility(View.GONE);
            chkSuspended.setVisibility(View.GONE);
            view.findViewById(R.id.edt_suspend_date_input_layout).setVisibility(View.GONE);
        }
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
            car.setColor(MaterialColors.getColor(requireContext(), R.attr.colorPrimary, 0));
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
