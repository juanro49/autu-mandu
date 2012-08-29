/*
 * Copyright 2012 Jan Kühle
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

package me.kuehle.carreport.reports;

import java.util.Collections;
import java.util.List;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.gui.SectionListAdapter.Item;
import me.kuehle.carreport.gui.SectionListAdapter.Section;
import me.kuehle.carreport.util.Calculator;

import org.achartengine.GraphicalView;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.util.MathHelper;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.text.format.DateFormat;

public abstract class AbstractReport {
	private ReportData data = new ReportData();
	private boolean showTrend = false;
	protected Context context;

	public AbstractReport(Context context) {
		this.context = context;
	}

	protected void addData(String label, String value) {
		data.getData().add(new Item(label, value));
	}

	protected Section addDataSection(String label, int color) {
		Section section = new Section(label, color);
		data.getData().add(section);
		return section;
	}

	protected Section addDataOverallSection() {
		String label = context.getString(R.string.report_overall);
		Preferences prefs = new Preferences(context);
		int position = prefs.getOverallSectionPos();
		Section section = new Section(label, Color.GRAY, position);
		data.getData().add(section);
		return section;
	}

	protected void applyDefaultStyle(XYMultipleSeriesRenderer renderer,
			double[] axesMinMax, boolean clickable, String xLabelFormat,
			String yLabelFormat) {
		Calculator calc = new Calculator(context);

		renderer.setLabelsTextSize(calc.spToPx(14));
		renderer.setLegendTextSize(calc.spToPx(14));
		renderer.setFitLegend(true);
		renderer.setPointSize(calc.dpToPx(4));
		renderer.setMargins(new int[] { 0, calc.spToPx(35), 0, 0 });
		renderer.setAxesColor(Color.DKGRAY);
		renderer.setLabelsColor(Color.LTGRAY);
		renderer.setShowGridX(true);
		renderer.setYLabelsAlign(Align.RIGHT);

		// When the background on the device is not completely black,
		// the margin background bad, because it is black. Setting it
		// to Color.TRANSPARENT does not work, but the below does.
		renderer.setApplyBackgroundColor(true);
		renderer.setBackgroundColor(Color.TRANSPARENT);
		renderer.setMarginsColor(Color.argb(0, 255, 0, 0));

		// When scaling the font, the amount of x labels is not being
		// adjusted. This results in labels, which lay one above the other. So
		// we need to adjust the amount manually.
		int xLabelCount = renderer.getXLabels();
		renderer.setXLabels((int) calc.pxToSp(xLabelCount) + 1);

		// Add 5% padding to top, left and right. Points at the edges should be
		// reachable simply.
		double padX = (axesMinMax[1] - axesMinMax[0]) * 0.05;
		double padY = (axesMinMax[3] - axesMinMax[2]) * 0.05;
		double[] limits = { axesMinMax[0] - padX, axesMinMax[1] + padX,
				axesMinMax[2], axesMinMax[3] + padY };

		renderer.setYAxisMin(limits[2]);
		renderer.setYAxisMax(limits[3]);
		renderer.setInitialRange(axesMinMax);
		renderer.setZoomEnabled(true, false);
		renderer.setPanEnabled(true, false);
		renderer.setPanLimits(limits);
		renderer.setZoomLimits(limits);

		renderer.setClickEnabled(clickable);
		renderer.setSelectableBuffer(calc.dpToPx(20));

		if (xLabelFormat != null) {
			List<Double> xLabels = MathHelper.getLabels(axesMinMax[0],
					axesMinMax[1], renderer.getXLabels());
			for (double label : xLabels) {
				renderer.addXTextLabel(label,
						String.format(xLabelFormat, label));
			}
		}
		if (yLabelFormat != null) {
			List<Double> yLabels = MathHelper.getLabels(axesMinMax[2],
					axesMinMax[3], renderer.getYLabels());
			for (double label : yLabels) {
				renderer.addYTextLabel(label,
						String.format(yLabelFormat, label));
			}
		}
	}
	
	public abstract int[] getCalculationOptions();

	public ReportData getData() {
		Collections.sort(data.getData());
		return data;
	}

	protected String getDateFormatPattern() {
		java.text.DateFormat dateFormat = DateFormat.getDateFormat(context);
		if (dateFormat instanceof java.text.SimpleDateFormat) {
			return ((java.text.SimpleDateFormat) dateFormat)
					.toLocalizedPattern();
		} else {
			return null;
		}
	}

	public abstract GraphicalView getGraphView();

	public boolean isShowTrend() {
		return showTrend;
	}

	public void setShowTrend(boolean showTrend) {
		this.showTrend = showTrend;
	}
}
