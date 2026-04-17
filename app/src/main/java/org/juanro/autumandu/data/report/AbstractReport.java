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

package org.juanro.autumandu.data.report;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.entity.Refueling;

public abstract class AbstractReport {
    private static final String TAG = "AbstractReport";

    public abstract static class AbstractListItem implements Comparable<AbstractListItem> {
        protected final String mLabel;

        public AbstractListItem(String label) {
            mLabel = label;
        }

        public String getLabel() {
            return mLabel;
        }
    }

    public static class Item extends AbstractListItem {
        private final String mValue;

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
        private final int mColor;
        private final int mOrder;
        private final List<Item> mItems;

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
                    return Integer.compare(mOrder, otherSection.getOrder());
                } else {
                    return mLabel.compareTo(another.getLabel());
                }
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Section other = (Section) obj;
            return mColor == other.mColor &&
                    mOrder == other.mOrder &&
                    mItems.equals(other.mItems);
        }

        public int getColor() {
            return mColor;
        }

        public List<Item> getItems() {
            return mItems;
        }

        public int getOrder() {
            return mOrder;
        }

        @Override
        public int hashCode() {
            int result = mColor;
            result = 31 * result + mOrder;
            result = 31 * result + mItems.hashCode();
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

    protected final Context mContext;
    private final List<AbstractListItem> mData = new ArrayList<>();
    protected final Map<Integer, List<AbstractReportChartData>> mCachedChartData = new java.util.HashMap<>();
    private boolean mUpdated = false;

    public AbstractReport(Context context) {
        mContext = context.getApplicationContext();
    }

    public void invalidate() {
        synchronized (mData) {
            mUpdated = false;
            mCachedChartData.clear();
        }
    }

    public abstract int[] getAvailableChartOptions();

    public abstract String getTitle();

    public List<AbstractListItem> getData() {
        return getData(false);
    }

    public List<AbstractListItem> getData(boolean flat) {
        List<AbstractListItem> dataCopy;
        synchronized (mData) {
            Collections.sort(mData);
            dataCopy = new ArrayList<>(mData);
        }

        if (flat) {
            List<AbstractListItem> items = new ArrayList<>();
            for (AbstractListItem item : dataCopy) {
                items.add(item);
                if (item instanceof Section) {
                    items.addAll(((Section) item).getItems());
                }
            }

            return items;
        } else {
            return dataCopy;
        }
    }

    /**
     * Updates the report with the newest data.
     * Note: This performs database operations, so it should be called from a background thread.
     */
    public void update() {
        synchronized (mData) {
            if (mUpdated) return;
            mData.clear();

            // Set base date for precision improvement (Issue #83).
            // Use the date of the very first refueling as Day 0.
            AutuManduDatabase db = AutuManduDatabase.getInstance(mContext);
            Refueling firstRefueling = db.getRefuelingDao().getFirst();
            if (firstRefueling != null) {
                ReportDateHelper.setBaseDate(firstRefueling.getDate());
            } else {
                ReportDateHelper.setBaseDate(null);
            }

            onUpdate();
            mUpdated = true;
        }
    }

    protected Section addDataSection(String label, int color) {
        return addDataSection(label, color, 0);
    }

    protected Section addDataSection(String label, int color, int order) {
        synchronized (mData) {
            Section section = new Section(label, color, order);
            mData.add(section);
            return section;
        }
    }

    public abstract String formatXValue(float value, int chartOption);

    public abstract String formatYValue(float value, int chartOption);

    public abstract List<AbstractReportChartData> getRawChartData(int chartOption);

    /**
     * Is called as a result of {@link #update()} and should perform all operations to
     * update the report with the newest data from the database.
     */
    protected abstract void onUpdate();
}
