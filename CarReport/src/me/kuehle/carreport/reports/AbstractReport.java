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
import java.util.Date;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.util.gui.SectionListAdapter.Item;
import me.kuehle.carreport.util.gui.SectionListAdapter.Section;
import me.kuehle.chartlib.axis.AxisLabelFormatter;
import me.kuehle.chartlib.chart.Chart;
import android.content.Context;
import android.graphics.Color;
import android.text.format.DateFormat;
import android.util.TypedValue;

public abstract class AbstractReport {
	public class CalculationOption {
		private String name;
		private String hint1;

		public CalculationOption(int name, int hint1) {
			this.name = context.getString(name);
			this.hint1 = context.getString(hint1);
		}

		public CalculationOption(String name, String hint1) {
			this.name = name;
			this.hint1 = hint1;
		}

		public String getHint1() {
			return hint1;
		}

		public String getName() {
			return name;
		}

		public void setHint1(String hint1) {
			this.hint1 = hint1;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
	private ReportData data = new ReportData();
	private boolean showTrend = false;
	protected Context context;

	protected AxisLabelFormatter dateLabelFormatter = new AxisLabelFormatter() {
		@Override
		public String formatLabel(double value) {
			return DateFormat.getDateFormat(context).format(
					new Date((long) value));
		}
	};

	public AbstractReport(Context context) {
		this.context = context;
	}

	protected void addData(Item item) {
		data.getData().add(item);
	}

	protected Section addDataOverallSection() {
		String label = context.getString(R.string.report_overall);
		Preferences prefs = new Preferences(context);
		int position = prefs.getOverallSectionPos();
		Section section = new Section(label, Color.GRAY, position);
		data.getData().add(section);
		return section;
	}

	protected Section addDataSection(String label, int color) {
		return addDataSection(label, color, Section.DONT_STICK);
	}
	
	protected Section addDataSection(String label, int color, int stick) {
		Section section = new Section(label, color, stick);
		data.getData().add(section);
		return section;
	}

	protected void applyDefaultChartStyles(Chart chart) {
		chart.getDomainAxis().setFontSize(14, TypedValue.COMPLEX_UNIT_SP);
		chart.getDomainAxis().setShowGrid(false);
		chart.getRangeAxis().setFontSize(14, TypedValue.COMPLEX_UNIT_SP);
		chart.getRangeAxis().setZoomable(false);
		chart.getRangeAxis().setMovable(false);
		chart.getLegend().setFontSize(14, TypedValue.COMPLEX_UNIT_SP);
	}

	public abstract CalculationOption[] getCalculationOptions();

	public abstract Chart getChart(int option);

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

	public abstract int[] getGraphOptions();

	public boolean isShowTrend() {
		return showTrend;
	}

	public void setShowTrend(boolean showTrend) {
		this.showTrend = showTrend;
	}
}
