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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.util.Calculator;

import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.renderer.BasicStroke;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.util.MathHelper;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.text.format.DateFormat;

public abstract class AbstractReport {
	private HashMap<Section, ArrayList<AbstractReport.Item>> data = new HashMap<Section, ArrayList<AbstractReport.Item>>();
	protected Context context;

	public AbstractReport(Context context) {
		this.context = context;
	}

	protected void addData(String label, String value) {
		addData(label, value, getOverallSection());
	}

	protected void addData(String label, String value, Car car) {
		addData(label, value, car == null ? getOverallSection() : new Section(
				car.getName(), car.getColor()));
	}

	protected void addData(String label, String value, Section section) {
		if (!data.containsKey(section)) {
			data.put(section, new ArrayList<AbstractReport.Item>());
		}
		ArrayList<AbstractReport.Item> items = data.get(section);
		items.add(new Item(label, value));
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

	public HashMap<Section, ArrayList<AbstractReport.Item>> getData() {
		return data;
	}

	public Section getOverallSection() {
		String label = context.getString(R.string.report_overall);
		Preferences prefs = new Preferences(context);
		int position = prefs.getOverallSectionPos();
		return new Section(label, Color.GRAY, position);
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

	protected void applyDefaultStyle(XYSeriesRenderer renderer, int color,
			boolean fill) {
		Calculator calc = new Calculator(context);

		renderer.setColor(color);
		renderer.setPointStyle(PointStyle.CIRCLE);
		renderer.setFillPoints(true);
		renderer.setLineWidth(calc.dpToPx(3));

		renderer.setFillBelowLine(fill);
		renderer.setFillBelowLineColor(Color.rgb(20, 40, 60));
	}
	
	protected void applyTrendStyle(XYSeriesRenderer renderer, int color) {
		Calculator calc = new Calculator(context);

		renderer.setColor(color);
		renderer.setLineWidth(calc.dpToPx(2));
		renderer.setStroke(BasicStroke.DASHED);
	}

	public static class Section implements Comparable<Section> {
		public static final int DONT_STICK = -1;
		public static final int STICK_TOP = 0;
		public static final int STICK_BOTTOM = 1;

		private String label;
		private int color;
		private int stickToPos;

		public Section(String label, int color) {
			this(label, color, DONT_STICK);
		}

		public Section(String label, int color, int stickToBottom) {
			this.label = label;
			this.color = color;
			this.stickToPos = stickToBottom;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public int getColor() {
			return color;
		}

		public void setColor(int color) {
			this.color = color;
		}

		public int getStickToPos() {
			return stickToPos;
		}

		public void setStickToPos(int stickToPos) {
			this.stickToPos = stickToPos;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + color;
			result = prime * result + ((label == null) ? 0 : label.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Section other = (Section) obj;
			if (color != other.color)
				return false;
			if (label == null) {
				if (other.label != null)
					return false;
			} else if (!label.equals(other.label))
				return false;
			return true;
		}

		@Override
		public int compareTo(Section another) {
			if (stickToPos == STICK_TOP && another.getStickToPos() != STICK_TOP) {
				return -1;
			} else if (stickToPos == STICK_BOTTOM
					&& another.getStickToPos() != STICK_BOTTOM) {
				return 1;
			} else {
				return label.compareTo(another.getLabel());
			}
		}
	}

	public static class Item {
		private String label;
		private String value;

		public Item(String label, String value) {
			this.label = label;
			this.value = value;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}
}
