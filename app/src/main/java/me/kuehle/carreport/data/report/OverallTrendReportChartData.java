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

import java.math.BigDecimal;
import java.math.MathContext;

import lecho.lib.hellocharts.model.Line;
import me.kuehle.carreport.R;
import me.kuehle.carreport.util.Calculator;

/**
 * Uses the method of least squares to calculate a straight regression line.
 */
public class OverallTrendReportChartData extends AbstractReportChartLineData {
    public OverallTrendReportChartData(Context context, String name, int color, Float[] parentXValues, Float[] parentYValues) {
        super(context, context.getString(R.string.report_overall_trend_label, name), color);

        // It doesn't make sense to display a trend line with just 2 points
        // or less because it would be the same as the original line.
        if (parentXValues.length <= 2) {
            return;
        }

        float avgX = Calculator.avg(parentXValues);
        float avgY = Calculator.avg(parentYValues);

        BigDecimal sum1 = BigDecimal.ZERO; // (x_i - avg(X)) ^ 2
        BigDecimal sum2 = BigDecimal.ZERO; // (x_i - avg(X)) * (y_i - avg(Y))
        for (int i = 0; i < parentXValues.length; i++) {
            BigDecimal xMinusAvgX = BigDecimal.valueOf(parentXValues[i] - avgX);
            BigDecimal yMinusAvgY = BigDecimal.valueOf(parentYValues[i] - avgY);
            sum1 = sum1.add(xMinusAvgX.multiply(xMinusAvgX));
            sum2 = sum2.add(yMinusAvgY.multiply(xMinusAvgX));
        }

        if (!sum1.equals(BigDecimal.ZERO)) {
            float beta1 = sum2.divide(sum1, MathContext.DECIMAL128).floatValue();
            float beta0 = avgY - (beta1 * avgX);

            add(parentXValues[0],
                    beta0 + (beta1 * parentXValues[0]),
                    null, false);
            add(parentXValues[parentXValues.length - 1],
                    beta0 + (beta1 * parentXValues[parentXValues.length - 1]),
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
