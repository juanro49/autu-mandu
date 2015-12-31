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

package me.kuehle.carreport.gui;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.ColumnChartView;
import me.kuehle.carreport.R;
import me.kuehle.carreport.data.calculation.AbstractCalculation;
import me.kuehle.carreport.data.calculation.CalculationItem;
import me.kuehle.carreport.data.calculation.DistanceToPriceCalculation;
import me.kuehle.carreport.data.calculation.DistanceToVolumeCalculation;
import me.kuehle.carreport.data.calculation.PriceToDistanceCalculation;
import me.kuehle.carreport.data.calculation.PriceToVolumeCalculation;
import me.kuehle.carreport.data.calculation.VolumeToDistanceCalculation;
import me.kuehle.carreport.data.calculation.VolumeToPriceCalculation;

public class CalculatorFragment extends Fragment {
    public final class ForceLoadContentObserver extends ContentObserver {
        public ForceLoadContentObserver() {
            super(new Handler());
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            calculate();
        }
    }

    private static final String STATE_CURRENT_OPTION = "current_option";

    private Spinner mSpnOptions;
    private EditText mEdtInput;
    private TextView mTxtUnit;
    private View mChartHolder;
    private ColumnChartView mChart;

    private ForceLoadContentObserver mObserver;

    private AbstractCalculation[] mCalculations;
    private AbstractCalculation mSelectedCalculation;

    @Override
    public void onAttach(@Nullable Context context) {
        super.onAttach(context);
        mCalculations = new AbstractCalculation[]{
                new VolumeToDistanceCalculation(context),
                new DistanceToVolumeCalculation(context),
                new VolumeToPriceCalculation(context),
                new PriceToVolumeCalculation(context),
                new DistanceToPriceCalculation(context),
                new PriceToDistanceCalculation(context)};

        mObserver = new ForceLoadContentObserver();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_calculator, container, false);
        mSpnOptions = (Spinner) v.findViewById(R.id.spn_options);
        mEdtInput = (EditText) v.findViewById(R.id.edt_input);
        mTxtUnit = (TextView) v.findViewById(R.id.txt_unit);
        mChartHolder = v.findViewById(R.id.chart_holder);
        mChart = (ColumnChartView) v.findViewById(R.id.chart);

        ArrayAdapter<String> options = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item);
        mSpnOptions.setAdapter(options);
        for (AbstractCalculation calculation : mCalculations) {
            options.add(calculation.getName());
        }

        if (savedInstanceState != null) {
            mSpnOptions.setSelection(savedInstanceState.getInt(STATE_CURRENT_OPTION, 0));
        }

        mSpnOptions.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView,
                                       int position, long id) {
                if (mSelectedCalculation != null) {
                    mSelectedCalculation.unregisterContentObserver(mObserver);
                }

                mSelectedCalculation = mCalculations[position];
                mSelectedCalculation.registerContentObserver(mObserver);

                mTxtUnit.setText(mSelectedCalculation.getInputUnit());

                calculate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                if (mSelectedCalculation != null) {
                    mSelectedCalculation.unregisterContentObserver(mObserver);
                }

                mSelectedCalculation = null;
            }
        });

        mEdtInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                calculate();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        mChart.setInteractive(false);

        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        MainActivity.setSupportActionBar(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_OPTION, mSpnOptions.getSelectedItemPosition());
    }

    private void calculate() {
        if (mSelectedCalculation == null) {
            return;
        }

        double input;
        try {
            input = Double.parseDouble(mEdtInput.getText().toString());

            mChartHolder.setVisibility(View.VISIBLE);
        } catch (NumberFormatException e) {
            mChartHolder.setVisibility(View.INVISIBLE);
            return;
        }

        final CalculationItem[] items = mSelectedCalculation.calculate(input);

        List<Column> columns = new ArrayList<>(items.length);
        List<AxisValue> axisValues = new ArrayList<>(items.length);
        int[] colors = ChartUtils.COLORS;
        int colorIndex = -1;
        for (int i = 0; i < items.length; i++) {
            float value = (float) items[i].getValue();
            int color = mSelectedCalculation.hasColors()
                    ? items[i].getColor()
                    : colors[++colorIndex % colors.length];
            String label = String.format("%.2f %s", value, mSelectedCalculation.getOutputUnit());

            List<SubcolumnValue> subcolumnValues = new ArrayList<>(1);
            subcolumnValues.add(new SubcolumnValue(value, color).setLabel(label));
            columns.add(new Column(subcolumnValues).setHasLabels(true));

            axisValues.add(new AxisValue(i).setLabel(items[i].getName()));
        }

        ColumnChartData data = new ColumnChartData(columns);
        data.setAxisXBottom(new Axis()
                .setTextColor(getResources().getColor(R.color.secondary_text))
                .setValues(axisValues));
        data.setAxisYLeft(new Axis()
                .setLineColor(getResources().getColor(R.color.divider))
                .setTextColor(getResources().getColor(R.color.secondary_text))
                .setHasLines(true)
                .setMaxLabelChars(4));

        mChart.setColumnChartData(data);
    }
}
