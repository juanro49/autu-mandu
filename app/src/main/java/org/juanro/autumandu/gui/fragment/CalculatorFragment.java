/*
 * Copyright 2014 Jan Kühle
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
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.juanro.autumandu.R;
import org.juanro.autumandu.data.calculation.AbstractCalculation;
import org.juanro.autumandu.data.calculation.CalculationItem;
import org.juanro.autumandu.gui.chart.kubit.KubitChartBridge;
import org.juanro.autumandu.viewmodel.CalculatorViewModel;

public class CalculatorFragment extends Fragment {
    private static final String STATE_CURRENT_OPTION = "current_option";

    private Spinner spnOptions;
    private EditText edtInput;
    private TextView txtUnit;
    private View chartHolder;
    private FrameLayout chartContainer;

    private CalculatorViewModel viewModel;
    private AbstractCalculation selectedCalculation;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CalculatorViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calculator, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(view.findViewById(R.id.toolbar));

        spnOptions = view.findViewById(R.id.spn_options);
        edtInput = view.findViewById(R.id.edt_input);
        txtUnit = view.findViewById(R.id.txt_unit);
        chartHolder = view.findViewById(R.id.chart_holder);
        chartContainer = view.findViewById(R.id.chart_container);

        var options = new ArrayAdapter<String>(requireActivity(),
                android.R.layout.simple_spinner_dropdown_item);
        spnOptions.setAdapter(options);
        for (var calculation : viewModel.getCalculations()) {
            options.add(calculation.getName());
        }

        if (savedInstanceState != null) {
            spnOptions.setSelection(savedInstanceState.getInt(STATE_CURRENT_OPTION, 0));
        }

        spnOptions.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView,
                                       int position, long id) {
                selectedCalculation = viewModel.getCalculations()[position];
                txtUnit.setText(selectedCalculation.getInputUnit());
                calculate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                selectedCalculation = null;
            }
        });

        edtInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                calculate();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        viewModel.getResults().observe(getViewLifecycleOwner(), this::updateChart);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (spnOptions != null) {
            outState.putInt(STATE_CURRENT_OPTION, spnOptions.getSelectedItemPosition());
        }
    }

    private void calculate() {
        if (selectedCalculation == null || edtInput == null) {
            return;
        }

        var inputText = edtInput.getText().toString();
        if (TextUtils.isEmpty(inputText)) {
            chartHolder.setVisibility(View.INVISIBLE);
            return;
        }

        final double input;
        try {
            input = Double.parseDouble(inputText);
        } catch (NumberFormatException e) {
            chartHolder.setVisibility(View.INVISIBLE);
            return;
        }

        viewModel.calculate(selectedCalculation, input);
    }

    private void updateChart(CalculationItem[] items) {
        if (items == null || items.length == 0 || !isAdded() || selectedCalculation == null) {
            chartHolder.setVisibility(View.INVISIBLE);
            return;
        }

        chartHolder.setVisibility(View.VISIBLE);
        chartContainer.removeAllViews();
        chartContainer.addView(KubitChartBridge.createCalculatorChart(
                requireContext(),
                items,
                selectedCalculation.getOutputUnit()
        ));
    }
}
