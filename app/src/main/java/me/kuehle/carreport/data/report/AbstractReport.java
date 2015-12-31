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
import android.database.ContentObserver;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import lecho.lib.hellocharts.formatter.AxisValueFormatter;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.ComboLineColumnChartData;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.SubcolumnValue;
import me.kuehle.carreport.R;

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
    private Cursor[] mUsedCursors;

    private boolean mInitialized = false;

    public AbstractReport(Context context) {
        mContext = context;
    }

    public abstract int[] getAvailableChartOptions();

    public abstract String getTitle();

    public ComboLineColumnChartData getChartData(final ReportChartOptions options) {
        if (!mInitialized) {
            return null;
        }

        List<AbstractReportChartData> rawData = getRawChartData(options.getChartOption());
        List<AbstractReportChartColumnData> rawColumnData = new ArrayList<>();
        AxisValueFormatter xAxisFormatter = new ReportAxisValueFormatter() {
            @Override
            public String format(float value) {
                return formatXValue(value, options.getChartOption());
            }
        };
        AxisValueFormatter yAxisFormatter = new ReportAxisValueFormatter() {
            @Override
            public String format(float value) {
                return formatYValue(value, options.getChartOption());
            }
        };

        List<Column> columns = new ArrayList<>();
        List<Line> lines = new ArrayList<>();

        for (AbstractReportChartData data : rawData) {
            data.sort();
        }

        if (options.isShowTrend()) {
            for (AbstractReportChartData data : rawData) {
                lines.add(data.createTrendData().getLine());
            }
        }

        if (options.isShowOverallTrend()) {
            for (AbstractReportChartData data : rawData) {
                lines.add(data.createOverallTrendData().getLine());
            }
        }

        for (AbstractReportChartData data : rawData) {
            if (data instanceof AbstractReportChartColumnData) {
                rawColumnData.add((AbstractReportChartColumnData) data);
            } else if (data instanceof AbstractReportChartLineData) {
                lines.add(((AbstractReportChartLineData) data).getLine());
            }
        }

        if (!rawColumnData.isEmpty()) {
            // Collect X values.
            SortedSet<Float> xValues = new TreeSet<>();
            for (AbstractReportChartData data : rawData) {
                xValues.addAll(data.getXValues());
            }

            Map<Float, Integer> xValueMap = new HashMap<>(xValues.size());
            int xValueIndex = 0;
            for (Float x : xValues) {
                // Build a map of X values to their indexes on the axis.
                xValueMap.put(x, xValueIndex);
                xValueIndex++;

                // Create column values.
                List<SubcolumnValue> subcolumnValues = new ArrayList<>();
                for (AbstractReportChartColumnData data : rawColumnData) {
                    SubcolumnValue subcolumnValue = data.getSubcolumnValue(x);
                    if (subcolumnValue != null) {
                        subcolumnValues.add(subcolumnValue);
                    }
                }

                columns.add(new Column(subcolumnValues));
            }

            // Convert X values of lines to indexes, so they match the columns.
            for (Line line : lines) {
                List<PointValue> points = line.getValues();
                for (PointValue point : points) {
                    point.set(xValueMap.get(point.getX()), point.getY());
                }
            }

            // Change X axis value formatter to first convert index back to real value.
            final Float[] xValueReverseMap = xValues.toArray(new Float[xValues.size()]);
            xAxisFormatter = new ReportAxisValueFormatter() {
                @Override
                public String format(float value) {
                    return formatXValue(xValueReverseMap[(int) value], options.getChartOption());
                }
            };
        }

        ColumnChartData columnChartData = new ColumnChartData(columns);
        LineChartData lineChartData = new LineChartData(lines);

        ComboLineColumnChartData data = new ComboLineColumnChartData(columnChartData, lineChartData);
        data.setAxisXBottom(new Axis()
                .setTextColor(mContext.getResources().getColor(R.color.secondary_text))
                .setFormatter(xAxisFormatter)
                .setMaxLabelChars(8));
        data.setAxisYLeft(new Axis()
                .setLineColor(mContext.getResources().getColor(R.color.divider))
                .setTextColor(mContext.getResources().getColor(R.color.secondary_text))
                .setHasLines(true)
                .setFormatter(yAxisFormatter)
                .setMaxLabelChars(4));

        return data;
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

    public void registerContentObserver(ContentObserver observer) {
        for (Cursor c : mUsedCursors) {
            c.registerContentObserver(observer);
        }
    }

    public void update() {
        mInitialized = false;
        mData.clear();
        mUsedCursors = onUpdate();
        mInitialized = true;
    }

    protected Section addDataSection(String label, int color) {
        return addDataSection(label, color, 0);
    }

    protected Section addDataSection(String label, int color, int order) {
        Section section = new Section(label, color, order);
        mData.add(section);
        return section;
    }

    protected abstract String formatXValue(float value, int chartOption);

    protected abstract String formatYValue(float value, int chartOption);

    protected abstract List<AbstractReportChartData> getRawChartData(int chartOption);

    /**
     * Is called as a result of {@link #update()} and should perform all operations to
     * update the report with the newest data.
     *
     * @return The cursors used in the process. These will be used to register a content
     * observer on them, so the caller can get noticed of any changes.
     */
    protected abstract Cursor[] onUpdate();
}
