package me.kuehle.carreport.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.kuehle.carreport.R;
import me.kuehle.carreport.data.calculation.AbstractCalculation;
import me.kuehle.carreport.data.calculation.CalculationItem;
import me.kuehle.carreport.data.calculation.DistanceToPriceCalculation;
import me.kuehle.carreport.data.calculation.DistanceToVolumeCalculation;
import me.kuehle.carreport.data.calculation.PriceToDistanceCalculation;
import me.kuehle.carreport.data.calculation.PriceToVolumeCalculation;
import me.kuehle.carreport.data.calculation.VolumeToDistanceCalculation;
import me.kuehle.carreport.data.calculation.VolumeToPriceCalculation;
import me.kuehle.carreport.gui.MainActivity.DataChangeListener;
import me.kuehle.chartlib.ChartView;
import me.kuehle.chartlib.axis.AxisLabelFormatter;
import me.kuehle.chartlib.axis.DecimalAxisLabelFormatter;
import me.kuehle.chartlib.chart.Chart;
import me.kuehle.chartlib.data.Dataset;
import me.kuehle.chartlib.data.Series;
import me.kuehle.chartlib.renderer.BarRenderer;
import me.kuehle.chartlib.renderer.RendererList;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class CalculatorFragment extends Fragment implements DataChangeListener {
	private static final String STATE_CURRENT_OPTION = "current_option";

	private Spinner mSpnOptions;
	private EditText mEdtInput;
	private TextView mTxtUnit;
	private View mGraphHolder;
	private ChartView mGraph;
	private ListView mTable;

	private AbstractCalculation[] mCalculations;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mCalculations = new AbstractCalculation[] {
				new VolumeToDistanceCalculation(activity),
				new DistanceToVolumeCalculation(activity),
				new VolumeToPriceCalculation(activity),
				new PriceToVolumeCalculation(activity),
				new DistanceToPriceCalculation(activity),
				new PriceToDistanceCalculation(activity) };
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_calculator, container,
				false);
		mSpnOptions = (Spinner) v.findViewById(R.id.spn_options);
		mEdtInput = (EditText) v.findViewById(R.id.edt_input);
		mTxtUnit = (TextView) v.findViewById(R.id.txt_unit);
		mGraphHolder = v.findViewById(R.id.chart_holder);
		mGraph = (ChartView) v.findViewById(R.id.chart);
		mTable = (ListView) v.findViewById(R.id.table);

		ArrayAdapter<String> options = new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_spinner_dropdown_item);
		mSpnOptions.setAdapter(options);
		for (AbstractCalculation calculation : mCalculations) {
			options.add(calculation.getName());
		}

		if (savedInstanceState != null) {
			mSpnOptions.setSelection(savedInstanceState.getInt(
					STATE_CURRENT_OPTION, 0));
		}

		mSpnOptions.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView,
					View selectedItemView, int position, long id) {
				AbstractCalculation calculation = mCalculations[position];
				mTxtUnit.setText(calculation.getInputUnit());

				calculate();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView) {
			}
		});

		mEdtInput.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				calculate();
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}
		});

		return v;
	}

	@Override
	public void onDataChanged() {
		calculate();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_CURRENT_OPTION,
				mSpnOptions.getSelectedItemPosition());
	}

	private void calculate() {
		AbstractCalculation calculation = mCalculations[mSpnOptions
				.getSelectedItemPosition()];
		double input = 1;
		try {
			input = Double.parseDouble(mEdtInput.getText().toString());
		} catch (NumberFormatException e) {
			mGraphHolder.setVisibility(View.INVISIBLE);
			mTable.setVisibility(View.INVISIBLE);
			return;
		}

		final CalculationItem[] items = calculation.calculate(input);

		// Update graph
		Dataset dataset = new Dataset();
		Series series = new Series(calculation.getOutputUnit());
		dataset.add(series);
		for (int i = 0; i < items.length; i++) {
			series.add(i, items[i].getValue());
		}

		RendererList renderers = new RendererList();
		renderers.addRenderer(new BarRenderer(getActivity()));

		Chart chart = new Chart(getActivity(), dataset, renderers);
		chart.getDomainAxis().setFontSize(14, TypedValue.COMPLEX_UNIT_SP);
		chart.getDomainAxis().setShowGrid(false);
		chart.getDomainAxis().setZoomable(false);
		chart.getDomainAxis().setMovable(false);
		chart.getDomainAxis().setLabels(getXValues(series));
		chart.getDomainAxis().setLabelFormatter(new AxisLabelFormatter() {
			@Override
			public String formatLabel(double value) {
				return items[(int) value].getName();
			}
		});
		chart.getDomainAxis().setDefaultBottomBound(dataset.minX() - 0.5);
		chart.getDomainAxis().setDefaultTopBound(dataset.maxX() + 0.5);
		chart.getRangeAxis().setFontSize(14, TypedValue.COMPLEX_UNIT_SP);
		chart.getRangeAxis().setZoomable(false);
		chart.getRangeAxis().setMovable(false);
		chart.getRangeAxis()
				.setLabelFormatter(new DecimalAxisLabelFormatter(2));
		chart.getRangeAxis().setDefaultBottomBound(0);
		chart.setShowLegend(false);

		mGraph.setChart(chart);
		mGraphHolder.setVisibility(View.VISIBLE);

		// Update table
		List<Map<String, String>> tableData = new ArrayList<Map<String, String>>();
		for (CalculationItem item : items) {
			Map<String, String> row = new HashMap<String, String>();
			row.put("label", item.getName());
			row.put("value",
					String.format("%.2f %s", item.getValue(),
							calculation.getOutputUnit()));
			tableData.add(row);
		}

		mTable.setAdapter(new SimpleAdapter(getActivity(), tableData,
				R.layout.row_report_data, new String[] { "label", "value" },
				new int[] { android.R.id.text1, android.R.id.text2 }));
		mTable.setVisibility(View.VISIBLE);
	}

	private double[] getXValues(Series series) {
		double[] xValues = new double[series.size()];
		for (int i = 0; i < series.size(); i++) {
			xValues[i] = series.get(i).x;
		}

		return xValues;
	}
}
