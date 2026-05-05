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

package org.juanro.autumandu.gui.dialog;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.util.AbstractFormFieldValidator;
import org.juanro.autumandu.gui.util.FormFieldNotEmptyValidator;
import org.juanro.autumandu.gui.util.FormValidator;
import org.juanro.autumandu.model.entity.FuelType;
import org.juanro.autumandu.viewmodel.FuelTypesViewModel;

public class EditFuelTypeDialogFragment extends AbstractEditDialogFragment {
    public static final String REQUEST_KEY = "org.juanro.autumandu.EDIT_FUEL_TYPE_REQUEST";

    public static EditFuelTypeDialogFragment newInstance(int requestCode, long fuelTypeId) {
        EditFuelTypeDialogFragment f = new EditFuelTypeDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, fuelTypeId);
        args.putInt(ARG_REQUEST_CODE, requestCode);
        f.setArguments(args);
        return f;
    }

    private Set<String> mOtherFuelTypeNames;
    private FuelType mFuelType;
    private AutoCompleteTextView mEdtCategory;
    private FuelTypesViewModel mViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        long currentFuelTypeId = getArguments() != null ? getArguments().getLong(ARG_ID, 0) : 0;
        mOtherFuelTypeNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        mViewModel = new ViewModelProvider(this).get(FuelTypesViewModel.class);
        mViewModel.getFuelTypes().observe(this, fuelTypes -> processFuelTypes(fuelTypes, currentFuelTypeId));
    }

    private void processFuelTypes(List<FuelType> fuelTypes, long currentFuelTypeId) {
        mOtherFuelTypeNames.clear();
        Set<String> categories = new HashSet<>();
        for (FuelType fuelType : fuelTypes) {
            if (currentFuelTypeId == fuelType.getId()) {
                mFuelType = fuelType;
                updateFieldsFromFuelType();
            } else {
                mOtherFuelTypeNames.add(fuelType.getName());
            }
            categories.add(fuelType.getCategory());
        }

        updateCategoryAdapter(categories);
    }

    private void updateFieldsFromFuelType() {
        if (mEdtName != null && mEdtName.getText().length() == 0) {
            mEdtName.setText(mFuelType.getName());
        }
        if (mEdtCategory != null && mEdtCategory.getText().length() == 0) {
            mEdtCategory.setText(mFuelType.getCategory());
        }
    }

    private void updateCategoryAdapter(Set<String> categories) {
        if (mEdtCategory != null) {
            String[] categoryArray = categories.toArray(new String[0]);
            mEdtCategory.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    categoryArray));
        }
    }

    @Override
    protected int getLayout() { return R.layout.dialog_fuel_type; }

    @Override
    protected int getAddTitle() { return R.string.title_add_fuel_type; }

    @Override
    protected int getEditTitle() { return R.string.title_edit_fuel_type; }

    @Override
    protected void initFields(View view, Bundle savedInstanceState) {
        mEdtCategory = view.findViewById(R.id.edt_category);
        if (savedInstanceState != null) {
            mEdtName.setText(savedInstanceState.getString("name"));
            mEdtCategory.setText(savedInstanceState.getString("category"));
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mEdtName != null) {
            outState.putString("name", mEdtName.getText().toString());
        }
        if (mEdtCategory != null) {
            outState.putString("category", mEdtCategory.getText().toString());
        }
    }

    @Override
    protected String getRequestKey() { return REQUEST_KEY; }

    @Override
    protected boolean save() {
        FormValidator validator = new FormValidator();
        validator.add(new FormFieldNotEmptyValidator(mEdtName));
        validator.add(new AbstractFormFieldValidator(mEdtName) {
            @Override
            protected boolean isValid() {
                return !mOtherFuelTypeNames.contains(mEdtName.getText().toString());
            }

            @Override
            protected int getMessage() {
                return R.string.validate_error_fuel_type_exists;
            }
        });
        validator.add(new FormFieldNotEmptyValidator(mEdtCategory));

        if (validator.validate()) {
            performSave();
            return true;
        }
        return false;
    }

    private void performSave() {
        if (mFuelType == null) {
            mViewModel.saveFuelType(new FuelType(
                    mEdtName.getText().toString(),
                    mEdtCategory.getText().toString()
            ));
        } else {
            mFuelType.setName(mEdtName.getText().toString());
            mFuelType.setCategory(mEdtCategory.getText().toString());
            mViewModel.saveFuelType(mFuelType);
        }
    }
}
