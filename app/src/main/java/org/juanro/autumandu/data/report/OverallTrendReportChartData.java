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
package org.juanro.autumandu.data.report;

import android.content.Context;
import android.graphics.DashPathEffect;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

import lecho.lib.hellocharts.model.Line;
import org.juanro.autumandu.R;

/**
 * Uses the method of least squares to calculate a straight regression line.
 */
public class OverallTrendReportChartData extends AbstractReportChartLineData {
    public OverallTrendReportChartData(Context context, String name, int color, List<AbstractReportChartData.DataPoint> dataPoints) {
        super(context, context.getString(R.string.report_overall_trend_label, name), color);

        // It doesn't make sense to display a trend line with just 2 points
        // or less because it would be the same as the original line.
        if (dataPoints == null || dataPoints.size() <= 2) {
            return;
        }

        double sumX = 0;
        double sumY = 0;
        for (AbstractReportChartData.DataPoint dp : dataPoints) {
            sumX += dp.x;
            sumY += dp.y;
        }

        float avgX = (float) (sumX / dataPoints.size());
        float avgY = (float) (sumY / dataPoints.size());

        BigDecimal sum1 = BigDecimal.ZERO; // (x_i - avg(X)) ^ 2
        BigDecimal sum2 = BigDecimal.ZERO; // (x_i - avg(X)) * (y_i - avg(Y))
        for (AbstractReportChartData.DataPoint dp : dataPoints) {
            BigDecimal xMinusAvgX = BigDecimal.valueOf(dp.x - avgX);
            BigDecimal yMinusAvgY = BigDecimal.valueOf(dp.y - avgY);
            sum1 = sum1.add(xMinusAvgX.multiply(xMinusAvgX));
            sum2 = sum2.add(yMinusAvgY.multiply(xMinusAvgX));
        }

        if (!sum1.equals(BigDecimal.ZERO)) {
            float beta1 = sum2.divide(sum1, MathContext.DECIMAL128).floatValue();
            float beta0 = avgY - (beta1 * avgX);

            float firstX = dataPoints.get(0).x;
            float lastX = dataPoints.get(dataPoints.size() - 1).x;

            add(firstX,
                    beta0 + (beta1 * firstX),
                    null, false);
            add(lastX,
                    beta0 + (beta1 * lastX),
                    null, false);
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
