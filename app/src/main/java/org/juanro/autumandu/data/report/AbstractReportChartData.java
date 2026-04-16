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
import android.graphics.Color;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractReportChartData {
    public static class DataPoint implements Comparable<DataPoint> {
        public float x;
        public float y;
        public String tooltip;

        public DataPoint(float x, float y, String tooltip) {
            this.x = x;
            this.y = y;
            this.tooltip = tooltip;
        }

        @Override
        public int compareTo(@NonNull DataPoint other) {
            return Float.compare(this.x, other.x);
        }
    }

    protected final Context mContext;
    protected final String mName;
    protected final int mColor;
    protected final List<DataPoint> mDataPoints = new ArrayList<>();

    public AbstractReportChartData(Context context, String name, int color) {
        mContext = context.getApplicationContext();
        mName = name;
        mColor = color;
    }

    public final OverallTrendReportChartData createOverallTrendData() {
        return new OverallTrendReportChartData(mContext, mName, getTrendColor(), mDataPoints);
    }

    public final TrendReportChartData createTrendData() {
        return new TrendReportChartData(mContext, mName, getTrendColor(), mDataPoints);
    }

    public List<DataPoint> getDataPoints() {
        return mDataPoints;
    }

    public int getColor() {
        return mColor;
    }

    public String getName() {
        return mName;
    }

    public List<Float> getYValues() {
        return mDataPoints.stream().map(p -> p.y).collect(Collectors.toList());
    }

    public final boolean isEmpty() {
        return mDataPoints.isEmpty();
    }

    public final int size() {
        return mDataPoints.size();
    }

    protected void add(Float x, Float y, String tooltip) {
        mDataPoints.add(new DataPoint(x, y, tooltip));
    }

    protected int indexOf(Float x) {
        for (int i = 0; i < mDataPoints.size(); i++) {
            if (mDataPoints.get(i).x == x) {
                return i;
            }
        }
        return -1;
    }

    protected void set(int index, Float x, Float y, String tooltip) {
        mDataPoints.set(index, new DataPoint(x, y, tooltip));
    }

    /**
     * Creates a color for the trend lines based on the original color. Alters
     * the original colors saturation by 0.5.
     *
     * @return the color for trend lines
     */
    private int getTrendColor() {
        float[] hsv = new float[3];
        Color.colorToHSV(this.mColor, hsv);
        if (hsv[1] > 0.5f) {
            hsv[1] -= 0.5f;
        } else {
            hsv[1] += 0.5f;
        }
        return Color.HSVToColor(hsv);
    }
}
