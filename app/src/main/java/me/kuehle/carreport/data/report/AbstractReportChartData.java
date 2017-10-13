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
import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractReportChartData {
    private Context mContext;
    private String mName;
    private int mColor;

    private ArrayList<Float> mXValues = new ArrayList<>();
    private ArrayList<Float> mYValues = new ArrayList<>();
    private ArrayList<String> mTooltips = new ArrayList<>();

    private float[] mColorTransformationCache = new float[3];

    public AbstractReportChartData(Context context, String name, int color) {
        mContext = context;
        mName = name;
        mColor = color;
    }

    public final OverallTrendReportChartData createOverallTrendData() {
        Float[] xArray = mXValues.toArray(new Float[mXValues.size()]);
        Float[] yArray = mYValues.toArray(new Float[mYValues.size()]);
        return new OverallTrendReportChartData(mContext, mName, getTrendColor(), xArray, yArray);
    }

    public final TrendReportChartData createTrendData() {
        Float[] xArray = mXValues.toArray(new Float[mXValues.size()]);
        Float[] yArray = mYValues.toArray(new Float[mYValues.size()]);
        return new TrendReportChartData(mContext, mName, getTrendColor(), xArray, yArray);
    }

    public int getColor() {
        return mColor;
    }

    public String getName() {
        return mName;
    }

    public ArrayList<String> getTooltips() {
        return mTooltips;
    }

    public final List<Float> getXValues() {
        return mXValues;
    }

    public ArrayList<Float> getYValues() {
        return mYValues;
    }

    public final boolean isEmpty() {
        return mXValues.size() == 0;
    }

    public final int size() {
        return mXValues.size();
    }

    public final void sort() {
        int lenD = size();
        int inc = lenD / 2;
        while (inc > 0) {
            for (int i = inc; i < lenD; i++) {
                Float tmp = mXValues.get(i);
                Float tmpY = mYValues.get(i);
                String tmpT = mTooltips.get(i);

                int j = i;
                while (j >= inc && mXValues.get(j - inc) > tmp) {
                    mXValues.set(j, mXValues.get(j - inc));
                    mYValues.set(j, mYValues.get(j - inc));
                    mTooltips.set(j, mTooltips.get(j - inc));
                    j = j - inc;
                }

                mXValues.set(j, tmp);
                mYValues.set(j, tmpY);
                mTooltips.set(j, tmpT);
            }

            inc = inc / 2;
        }
    }

    protected void add(Float x, Float y, String tooltip) {
        mXValues.add(x);
        mYValues.add(y);
        mTooltips.add(tooltip);
    }

    protected int indexOf(Float x) {
        return mXValues.indexOf(x);
    }

    protected void set(int index, Float x, Float y, String tooltip) {
        mXValues.set(index, x);
        mYValues.set(index, y);
        mTooltips.set(index, tooltip);
    }

    /**
     * Creates a color for the trend lines based on the original color. Alters
     * the original colors saturation by 0.5.
     *
     * @return the color for trend lines
     */
    private int getTrendColor() {
        Color.colorToHSV(this.mColor, mColorTransformationCache);
        if (mColorTransformationCache[1] > 0.5) {
            mColorTransformationCache[1] -= 0.5;
        } else {
            mColorTransformationCache[1] += 0.5;
        }

        return Color.HSVToColor(mColorTransformationCache);
    }
}
