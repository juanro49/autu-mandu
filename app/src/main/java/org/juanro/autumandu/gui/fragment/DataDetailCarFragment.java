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

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.juanro.autumandu.R;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.FuelCategory;
import org.juanro.autumandu.model.entity.Tank;
import org.juanro.autumandu.viewmodel.CarDetailViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Fragment to edit car details.
 */
@AndroidEntryPoint
public class DataDetailCarFragment extends AbstractDataDetailFragment {
    private EditText edtName;
    private EditText edtInitialMileage;
    private EditText edtNumTires;
    private EditText edtBuyingPrice;
    private View colorPreview;
    private CheckBox chkSuspended;

    private RecyclerView lstTanks;
    private MaterialButton btnAddTank;

    private CarDetailViewModel viewModel;
    private Car car;
    private final List<Tank> mTanks = new ArrayList<>();
    private final List<Tank> mDeletedTanks = new ArrayList<>();
    private TankAdapter tankAdapter;

    @Override
    protected void initFields(Bundle savedInstanceState, View view) {
        edtName = view.findViewById(R.id.edt_name);
        edtInitialMileage = view.findViewById(R.id.edt_initial_mileage);
        edtNumTires = view.findViewById(R.id.edt_num_tires);
        edtBuyingPrice = view.findViewById(R.id.edt_buying_price);
        colorPreview = view.findViewById(R.id.btn_color);
        chkSuspended = view.findViewById(R.id.chk_suspend);

        lstTanks = view.findViewById(R.id.lst_tanks);
        btnAddTank = view.findViewById(R.id.btn_add_tank);

        lstTanks.setLayoutManager(new LinearLayoutManager(requireContext()));
        tankAdapter = new TankAdapter();
        lstTanks.setAdapter(tankAdapter);

        btnAddTank.setOnClickListener(v -> showEditTankDialog(null));

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

            LinearLayoutCompat container = new LinearLayoutCompat(requireContext());
            container.setOrientation(LinearLayoutCompat.VERTICAL);
            container.setPadding(32, 32, 32, 32);

            ScrollView scrollView = new ScrollView(requireContext());
            scrollView.addView(container);

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

        viewModel.getTanks().observe(getViewLifecycleOwner(), tanks -> {
            if (mTanks.isEmpty()) {
                mTanks.addAll(tanks);
                tankAdapter.notifyDataSetChanged();
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

    private void showEditTankDialog(@Nullable Tank tank) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_tank, null);
        Spinner spnCategory = view.findViewById(R.id.spn_fuel_category);
        EditText edtName = view.findViewById(R.id.edt_tank_name);
        EditText edtCapacity = view.findViewById(R.id.edt_capacity);
        CheckBox chkManual = view.findViewById(R.id.chk_is_manual);

        spnCategory.setAdapter(new FuelCategoryArrayAdapter(requireContext()));

        if (tank != null) {
            selectSpinnerItemByValue(spnCategory, tank.getFuelCategory());
            edtName.setText(tank.getName());
            edtCapacity.setText(String.valueOf(tank.getCapacity()));
            chkManual.setChecked(tank.isManuallySet());
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(tank == null ? R.string.menu_add_tank : R.string.title_tanks)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String category = (String) spnCategory.getSelectedItem();
                    String name = edtName.getText().toString();
                    float capacity = (float) getDoubleFromEditText(edtCapacity);
                    boolean isManual = chkManual.isChecked();

                    if (tank == null) {
                        Tank newTank = new Tank(mId, category, name, capacity, isManual);
                        mTanks.add(newTank);
                    } else {
                        tank.setFuelCategory(category);
                        tank.setName(name);
                        tank.setCapacity(capacity);
                        tank.setManuallySet(isManual);
                    }
                    tankAdapter.notifyDataSetChanged();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void selectSpinnerItemByValue(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).equals(value)) {
                spinner.setSelection(i);
                break;
            }
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

        viewModel.save(car, mTanks, mDeletedTanks, () -> mOnItemActionListener.onItemSavedAsync(car.getId() != null ? car.getId() : 0));
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

    private class TankAdapter extends RecyclerView.Adapter<TankAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Tank tank = mTanks.get(position);
            String title = tank.getName() != null && !tank.getName().isEmpty() ? tank.getName() : tank.getFuelCategory();
            holder.text1.setText(title);
            holder.text2.setText(String.format(Locale.getDefault(), "%.2f", tank.getCapacity()));

            holder.itemView.setOnClickListener(v -> showEditTankDialog(tank));
            holder.itemView.setOnLongClickListener(v -> {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.alert_delete_title)
                        .setMessage(R.string.menu_delete)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            mTanks.remove(position);
                            if (tank.getId() != null) {
                                mDeletedTanks.add(tank);
                            }
                            notifyDataSetChanged();
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return mTanks.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1;
            TextView text2;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }

    private static class FuelCategoryArrayAdapter extends ArrayAdapter<String> {
        public FuelCategoryArrayAdapter(android.content.Context context) {
            super(context, android.R.layout.simple_spinner_dropdown_item, FuelCategory.getAllKeys());
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            TextView v = (TextView) super.getView(position, convertView, parent);
            v.setText(FuelCategory.fromKey(getItem(position)).getName(getContext()));
            return v;
        }

        @NonNull
        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            TextView v = (TextView) super.getDropDownView(position, convertView, parent);
            v.setText(FuelCategory.fromKey(getItem(position)).getName(getContext()));
            return v;
        }
    }
}
