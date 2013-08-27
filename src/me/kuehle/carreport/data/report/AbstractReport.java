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

package me.kuehle.carreport.data.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import me.kuehle.chartlib.axis.AxisLabelFormatter;
import me.kuehle.chartlib.chart.Chart;
import android.content.Context;
import android.text.format.DateFormat;
import android.util.TypedValue;

public abstract class AbstractReport {
	public abstract static class AbstractListItem implements
			Comparable<AbstractListItem> {
		protected String label;

		public AbstractListItem(String label) {
			this.label = label;
		}

		public String getLabel() {
			return label;
		}
	}

	public static class Item extends AbstractListItem {
		private String value;

		public Item(String label, String value) {
			super(label);
			this.value = value;
		}

		@Override
		public int compareTo(AbstractListItem another) {
			if (another instanceof Section) {
				return -1;
			} else {
				return label.compareTo(another.getLabel());
			}
		}

		public String getValue() {
			return value;
		}
	}

	public static class Section extends AbstractListItem {
		private int color;
		private int order;
		private ArrayList<Item> items;

		public Section(String label, int color) {
			this(label, color, 0);
		}

		public Section(String label, int color, int order) {
			super(label);
			this.color = color;
			this.order = order;
			this.items = new ArrayList<Item>();
		}

		public void addItem(Item item) {
			items.add(item);
		}

		@Override
		public int compareTo(AbstractListItem another) {
			if (another instanceof Item) {
				return 1;
			} else {
				Section otherSection = (Section) another;
				if (order != otherSection.getOrder()) {
					return Integer.valueOf(order).compareTo(
							otherSection.getOrder());
				} else {
					return label.compareTo(another.getLabel());
				}
			}
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
			if (items == null) {
				if (other.items != null)
					return false;
			} else if (!items.equals(other.items))
				return false;
			if (order != other.order)
				return false;
			return true;
		}

		public int getColor() {
			return color;
		}

		public ArrayList<Item> getItems() {
			return items;
		}

		public int getOrder() {
			return order;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + color;
			result = prime * result + ((items == null) ? 0 : items.hashCode());
			result = prime * result + order;
			return result;
		}

		public void removeItem(Item item) {
			items.remove(item);
		}
	}

	protected Context context;

	private ArrayList<AbstractListItem> data = new ArrayList<AbstractListItem>();
	private boolean showTrend = false;
	private int chartOption = 0;
	private boolean initialized = false;

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

	public abstract int[] getAvailableChartOptions();

	public Chart getChart(boolean zoomable, boolean moveable) {
		if (initialized) {
			return onGetChart(zoomable, moveable);
		} else {
			return null;
		}
	}

	public int getChartOption() {
		return chartOption;
	}

	public List<AbstractListItem> getData() {
		return getData(false);
	}

	public List<AbstractListItem> getData(boolean flat) {
		Collections.sort(data);

		if (flat) {
			ArrayList<AbstractListItem> items = new ArrayList<AbstractListItem>();
			for (AbstractListItem item : data) {
				items.add(item);
				if (item instanceof Section) {
					items.addAll(((Section) item).getItems());
				}
			}

			return items;
		} else {
			return data;
		}
	}

	public abstract String getTitle();

	public boolean isShowTrend() {
		return showTrend;
	}

	public void setChartOption(int chartOption) {
		if (chartOption < getAvailableChartOptions().length) {
			this.chartOption = chartOption;
		} else {
			this.chartOption = 0;
		}
	}

	public void setShowTrend(boolean showTrend) {
		this.showTrend = showTrend;
	}

	public void update() {
		initialized = false;
		data.clear();
		onUpdate();
		initialized = true;
	}

	protected void addData(String label, String value) {
		data.add(new Item(label, value));
	}

	protected Section addDataSection(String label, int color) {
		return addDataSection(label, color, 0);
	}

	protected Section addDataSection(String label, int color, int order) {
		Section section = new Section(label, color, order);
		data.add(section);
		return section;
	}

	protected void applyDefaultChartStyles(Chart chart) {
		chart.getDomainAxis().setFontSize(14, TypedValue.COMPLEX_UNIT_SP);
		chart.getDomainAxis().setShowGrid(false);
		chart.getRangeAxis().setFontSize(14, TypedValue.COMPLEX_UNIT_SP);
		chart.getLegend().setFontSize(14, TypedValue.COMPLEX_UNIT_SP);
	}

	protected abstract Chart onGetChart(boolean zoomable, boolean moveable);

	protected abstract void onUpdate();
}
