/*
 * Copyright 2015 Jan Kühle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.kuehle.carreport.data.report;

import android.content.Context;
import android.graphics.DashPathEffect;

import lecho.lib.hellocharts.model.Line;
import me.kuehle.carreport.R;

/**
 * Uses simple moving average to calculate a accurate trend line.
 */
class TrendReportChartData extends AbstractReportChartLineData {
    public TrendReportChartData(Context context, String name, int color, Float[] parentXValues, Float[] parentYValues) {
        super(context, context.getString(R.string.report_trend_label, name), color);

        // Use higher order when more entries are available to calculate a
        // more accurate trend.
        int order;
        if (parentXValues.length > 7) {
            order = 5;
        } else if (parentXValues.length > 3) {
            order = 3;
        } else {
            // It doesn't make sense to display a trend line with order 1
            // because it would be the same as the original line.
            return;
        }

        int k = (order - 1) / 2;

        for (int t = k; t < parentXValues.length - k; t++) {
            float x = parentXValues[t];

            // y_t = (y_t-k + y_t-k+1 + ... + y_t + ... + y_t+k-1 + y_t+k) / order
            float y = 0;
            for (int i = t - k; i <= t + k; i++) {
                y += parentYValues[i];
            }

            y /= order;

            add(x, y, null, false);
        }
    }

    @Override
    public Line getLine() {
        Line line = super.getLine();
        line.setHasPoints(false);
        line.setStrokeWidth(1);
        line.setPathEffect(new DashPathEffect(new float[]{20, 20}, 0));
        return line;
    }
}
