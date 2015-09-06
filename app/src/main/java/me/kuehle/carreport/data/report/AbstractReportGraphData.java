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
import android.graphics.DashPathEffect;
import android.util.TypedValue;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.kuehle.carreport.R;
import me.kuehle.chartlib.data.PointD;
import me.kuehle.chartlib.data.Series;
import me.kuehle.chartlib.renderer.AbstractRenderer;
import me.kuehle.chartlib.renderer.LineRenderer;

public abstract class AbstractReportGraphData {
    /**
     * Uses the method of least squares to calculate a straight regression line.
     */
    private class OverallTrendReportData extends AbstractReportGraphData {
        public OverallTrendReportData(AbstractReportGraphData data) {
            super(data.context, data.context.getString(
                    R.string.report_overall_trend_label, data.name), data
                    .getTrendColor());

            // It doesn't make sense to display a trend line with just 2 points
            // or less because it would be the same as the original line.
            if (data.size() <= 2) {
                return;
            }

            double sumX = 0;
            double sumY = 0;
            for (int i = 0; i < data.size(); i++) {
                sumX += data.points.get(i).x;
                sumY += data.points.get(i).y;
            }

            double avgX = sumX / data.size();
            double avgY = sumY / data.size();

            BigDecimal sum1 = BigDecimal.ZERO; // (x_i - avg(X)) ^ 2
            BigDecimal sum2 = BigDecimal.ZERO; // (x_i - avg(X)) * (y_i - avg(Y))
            for (int i = 0; i < data.size(); i++) {
                BigDecimal xMinusAvgX = BigDecimal.valueOf(data.points.get(i).x - avgX);
                BigDecimal yMinusAvgY = BigDecimal.valueOf(data.points.get(i).y - avgY);
                sum1 = sum1.add(xMinusAvgX.multiply(xMinusAvgX));
                sum2 = sum2.add(yMinusAvgY.multiply(xMinusAvgX));
            }

            if (!sum1.equals(BigDecimal.ZERO)) {
                double beta1 = sum2.divide(sum1, MathContext.DECIMAL128).doubleValue();
                double beta0 = avgY - (beta1 * avgX);

                PointD firstPoint = data.points.get(0);
                addPoint(firstPoint.x, beta0 + (beta1 * firstPoint.x));

                PointD lastPoint = data.points.get(data.points.size() - 1);
                addPoint(lastPoint.x, beta0 + (beta1 * lastPoint.x));
            }
        }

        @Override
        public void applySeriesStyle(int series, AbstractRenderer renderer) {
            super.applySeriesStyle(series, renderer);
            if (renderer instanceof LineRenderer) {
                ((LineRenderer) renderer).setSeriesLineWidth(series, 2,
                        TypedValue.COMPLEX_UNIT_DIP);
                ((LineRenderer) renderer).setSeriesDrawPoints(series, false);
                ((LineRenderer) renderer).setSeriesPathEffect(series,
                        new DashPathEffect(new float[]{5, 5}, 0));
            }
        }
    }

    /**
     * Uses simple moving average to calculate a accurate trend line.
     */
    private class TrendReportData extends AbstractReportGraphData {
        public TrendReportData(AbstractReportGraphData data) {
            super(data.context, data.context.getString(
                    R.string.report_trend_label, data.name), data.getTrendColor());

            // Use higher order when more entries are available to calculate a
            // more accurate trend.
            int order;
            if (data.points.size() > 7) {
                order = 5;
            } else if (data.points.size() > 3) {
                order = 3;
            } else {
                // It doesn't make sense to display a trend line with order 1
                // because it would be the same as the original line.
                return;
            }

            int k = (order - 1) / 2;

            for (int t = k; t < data.points.size() - k; t++) {
                double x = data.points.get(t).x;

                // y_t = (y_t-k + y_t-k+1 + ... + y_t + ... + y_t+k-1 + y_t+k) / order
                double y = 0;
                for (int i = t - k; i <= t + k; i++) {
                    y += data.points.get(i).y;
                }

                y /= order;

                addPoint(x, y);
            }
        }

        @Override
        public void applySeriesStyle(int series, AbstractRenderer renderer) {
            super.applySeriesStyle(series, renderer);
            if (renderer instanceof LineRenderer) {
                ((LineRenderer) renderer).setSeriesLineWidth(series, 2,
                        TypedValue.COMPLEX_UNIT_DIP);
                ((LineRenderer) renderer).setSeriesPathEffect(series,
                        new DashPathEffect(new float[]{5, 5}, 0));
            }
        }
    }

    protected Context context;
    protected String name;
    protected int color;

    private List<PointD> points = new ArrayList<>();

    private List<List<PointD>> markLines = new ArrayList<>();
    private List<PointD> markPoints = new ArrayList<>();

    public AbstractReportGraphData(Context context, String name, int color) {
        this.context = context;
        this.name = name;
        this.color = color;
    }

    public void applySeriesStyle(int series, AbstractRenderer renderer) {
        renderer.setSeriesColor(series, color);
        if (renderer instanceof LineRenderer) {
            ((LineRenderer) renderer).setSeriesLineWidth(series, 3,
                    TypedValue.COMPLEX_UNIT_DIP);
            ((LineRenderer) renderer).setSeriesPathEffect(series, null);

            // Styles for marked lines and points
            ((LineRenderer) renderer).setSeriesMarkColor(
                    series,
                    Color.argb(63, Color.red(color), Color.green(color),
                            Color.blue(color)));
            ((LineRenderer) renderer).setSeriesMarkPathEffect(series,
                    new DashPathEffect(new float[]{5, 5}, 0));

            for (PointD point : markPoints) {
                ((LineRenderer) renderer).addSeriesMarkPoint(series, point);
            }

            for (List<PointD> line : markLines) {
                ((LineRenderer) renderer).addSeriesMarkLine(series,
                        line.get(0), line.get(1));
            }
        }
    }

    public AbstractReportGraphData createOverallTrendData() {
        this.sort();
        return new OverallTrendReportData(this);
    }

    public AbstractReportGraphData createTrendData() {
        this.sort();
        return new TrendReportData(this);
    }

    public Series getSeries() {
        Series series = new Series(name);
        for (int i = 0; i < size(); i++) {
            PointD point = points.get(i);
            series.add(point.x, point.y);
        }

        return series;
    }

    public void addPoint(double x, double y) {
        points.add(new PointD(x, y));
    }

    public boolean isEmpty() {
        return points.size() == 0;
    }

    public int size() {
        return points.size();
    }

    public void sort() {
        Collections.sort(points);
    }

    /**
     * Creates a color for the trend lines based on the original color. Alters
     * the original colors saturation by 0.5.
     *
     * @return the color for trend lines
     */
    protected int getTrendColor() {
        float[] hsvColor = new float[3];
        Color.colorToHSV(this.color, hsvColor);

        if (hsvColor[1] > 0.5) {
            hsvColor[1] -= 0.5;
        } else {
            hsvColor[1] += 0.5;
        }

        return Color.HSVToColor(hsvColor);
    }

    protected void markLastLine() {
        if (points.size() < 2) {
            return;
        }

        List<PointD> line = new ArrayList<>();
        line.add(points.get(points.size() - 2));
        line.add(points.get(points.size() - 1));
        markLines.add(line);
    }

    protected void markLastPoint() {
        markPoints.add(points.get(points.size() - 1));
    }
}
