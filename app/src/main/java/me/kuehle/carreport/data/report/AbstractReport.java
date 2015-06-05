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

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TypedValue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import me.kuehle.chartlib.axis.AxisLabelFormatter;
import me.kuehle.chartlib.chart.Chart;

public abstract class AbstractReport {
    private static final String TAG = "AbstractReport";

    public abstract static class AbstractListItem implements Comparable<AbstractListItem> {
        protected String mLabel;

        public AbstractListItem(String label) {
            mLabel = label;
        }

        public String getLabel() {
            return mLabel;
        }
    }

    public static class Item extends AbstractListItem {
        private String mValue;

        public Item(String label, String value) {
            super(label);
            mValue = value;
        }

        @Override
        public int compareTo(@NonNull AbstractListItem another) {
            if (another instanceof Section) {
                return -1;
            } else {
                return mLabel.compareTo(another.getLabel());
            }
        }

        public String getValue() {
            return mValue;
        }
    }

    public static class Section extends AbstractListItem {
        private int mColor;
        private int mOrder;
        private ArrayList<Item> mItems;

        public Section(String label, int color) {
            this(label, color, 0);
        }

        public Section(String label, int color, int order) {
            super(label);
            mColor = color;
            mOrder = order;
            mItems = new ArrayList<>();
        }

        public void addItem(Item item) {
            mItems.add(item);
        }

        @Override
        public int compareTo(@NonNull AbstractListItem another) {
            if (another instanceof Item) {
                return 1;
            } else {
                Section otherSection = (Section) another;
                if (mOrder != otherSection.getOrder()) {
                    return Integer.valueOf(mOrder).compareTo(otherSection.getOrder());
                } else {
                    return mLabel.compareTo(another.getLabel());
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
            if (mColor != other.mColor)
                return false;
            if (mItems == null) {
                if (other.mItems != null)
                    return false;
            } else if (!mItems.equals(other.mItems))
                return false;
            return mOrder == other.mOrder;
        }

        public int getColor() {
            return mColor;
        }

        public ArrayList<Item> getItems() {
            return mItems;
        }

        public int getOrder() {
            return mOrder;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + mColor;
            result = prime * result + ((mItems == null) ? 0 : mItems.hashCode());
            result = prime * result + mOrder;
            return result;
        }
    }

    public static AbstractReport newInstance(Class<? extends AbstractReport> reportClass,
                                             Context context) {
        try {
            Constructor<? extends AbstractReport> constructor = reportClass
                    .getConstructor(Context.class);
            return constructor.newInstance(context);
        } catch (NoSuchMethodException | IllegalArgumentException | InstantiationException |
                InvocationTargetException | IllegalAccessException e) {
            Log.e(TAG, "Error creating report.", e);
        }

        return null;
    }

    protected Context mContext;
    private ArrayList<AbstractListItem> mData = new ArrayList<>();
    private boolean mShowTrend = false;
    private boolean mShowOverallTrend = false;
    private int mChartOption = 0;

    private boolean mInitialized = false;

    protected AxisLabelFormatter mDateLabelFormatter = new AxisLabelFormatter() {
        @Override
        public String formatLabel(double value) {
            return DateFormat.getDateFormat(mContext).format(new Date((long) value));
        }
    };

    public AbstractReport(Context context) {
        this.mContext = context;
    }

    public abstract int[] getAvailableChartOptions();

    public Chart getChart(boolean zoomable, boolean moveable) {
        if (mInitialized) {
            return onGetChart(zoomable, moveable);
        } else {
            return null;
        }
    }

    public int getChartOption() {
        return mChartOption;
    }

    public List<AbstractListItem> getData() {
        return getData(false);
    }

    public List<AbstractListItem> getData(boolean flat) {
        Collections.sort(mData);

        if (flat) {
            ArrayList<AbstractListItem> items = new ArrayList<>();
            for (AbstractListItem item : mData) {
                items.add(item);
                if (item instanceof Section) {
                    items.addAll(((Section) item).getItems());
                }
            }

            return items;
        } else {
            return mData;
        }
    }

    public abstract String getTitle();

    public boolean isShowTrend() {
        return mShowTrend;
    }

    public boolean isShowOverallTrend() {
        return mShowOverallTrend;
    }

    public void setChartOption(int chartOption) {
        if (chartOption < getAvailableChartOptions().length) {
            this.mChartOption = chartOption;
        } else {
            this.mChartOption = 0;
        }
    }

    public void setShowTrend(boolean showTrend) {
        this.mShowTrend = showTrend;
    }

    public void setShowOverallTrend(boolean showOverallTrend) {
        this.mShowOverallTrend = showOverallTrend;
    }

    public void update() {
        mInitialized = false;
        mData.clear();
        onUpdate();
        mInitialized = true;
    }

    protected void addData(String label, String value) {
        mData.add(new Item(label, value));
    }

    protected Section addDataSection(String label, int color) {
        return addDataSection(label, color, 0);
    }

    protected Section addDataSection(String label, int color, int order) {
        Section section = new Section(label, color, order);
        mData.add(section);
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
