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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import me.kuehle.carreport.R;
import me.kuehle.carreport.util.Calculator;
import me.kuehle.chartlib.data.PointD;
import me.kuehle.chartlib.data.Series;
import me.kuehle.chartlib.renderer.AbstractRenderer;
import me.kuehle.chartlib.renderer.LineRenderer;
import android.content.Context;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.util.TypedValue;

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
			if (data.xValues.size() <= 2 || data.yValues.size() <= 2) {
				return;
			}

			long avgX = Calculator.avg(data.xValues);
			double avgY = Calculator.avg(data.yValues);

			BigInteger sum1 = BigInteger.ZERO; // (x_i - avg(X)) ^ 2
			BigDecimal sum2 = BigDecimal.ZERO; // (x_i - avg(X)) * (y_i -
												// avg(Y))
			for (int i = 0; i < data.size(); i++) {
				BigInteger xMinusAvgX = BigInteger.valueOf(data.xValues.get(i)
						- avgX);
				BigDecimal yMinusAvgY = BigDecimal.valueOf(data.yValues.get(i)
						- avgY);
				sum1 = sum1.add(xMinusAvgX.multiply(xMinusAvgX));
				sum2 = sum2
						.add(yMinusAvgY.multiply(new BigDecimal(xMinusAvgX)));
			}

			if (!sum1.equals(BigInteger.ZERO)) {
				double beta1 = sum2.divide(new BigDecimal(sum1),
						MathContext.DECIMAL128).doubleValue();
				double beta0 = avgY - (beta1 * avgX);

				xValues.add(data.xValues.firstElement());
				yValues.add(beta0 + (beta1 * data.xValues.firstElement()));
				xValues.add(data.xValues.lastElement());
				yValues.add(beta0 + (beta1 * data.xValues.lastElement()));
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
						new DashPathEffect(new float[] { 5, 5 }, 0));
			}
		}
	}

	/**
	 * Uses simple moving average to calculate a accurate trend line.
	 */
	private class TrendReportData extends AbstractReportGraphData {
		public TrendReportData(AbstractReportGraphData data) {
			super(data.context, data.context.getString(
					R.string.report_trend_label, data.name), data
					.getTrendColor());

			// Use higher order when more entries are available to calculate a
			// more accurate trend.
			int order = 1;
			if (data.xValues.size() > 7) {
				order = 5;
			} else if (data.xValues.size() > 3) {
				order = 3;
			} else {
				// It doesn't make sense to display a trend line with order 1
				// because it would be the same as the original line.
				return;
			}

			int k = (order - 1) / 2;

			for (int t = k; t < data.xValues.size() - k; t++) {
				long x = data.xValues.get(t);

				// y_t = (y_t-k + y_t-k+1 + ... + y_t + ... + y_t+k-1 + y_t+k) /
				// order
				double y = 0;
				for (int i = t - k; i <= t + k; i++) {
					y += data.yValues.get(i);
				}

				y /= order;

				xValues.add(x);
				yValues.add(y);
			}
		}

		@Override
		public void applySeriesStyle(int series, AbstractRenderer renderer) {
			super.applySeriesStyle(series, renderer);
			if (renderer instanceof LineRenderer) {
				((LineRenderer) renderer).setSeriesLineWidth(series, 2,
						TypedValue.COMPLEX_UNIT_DIP);
				((LineRenderer) renderer).setSeriesPathEffect(series,
						new DashPathEffect(new float[] { 5, 5 }, 0));
			}
		}
	}

	protected Context context;
	protected String name;
	protected int color;

	protected Vector<Long> xValues = new Vector<Long>();
	protected Vector<Double> yValues = new Vector<Double>();

	private List<List<PointD>> markLines = new ArrayList<List<PointD>>();
	private List<PointD> markPoints = new ArrayList<PointD>();

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
					new DashPathEffect(new float[] { 5, 5 }, 0));

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
			series.add(xValues.get(i), yValues.get(i));
		}
		return series;
	}

	public boolean isEmpty() {
		return xValues.size() == 0 || yValues.size() == 0;
	}

	public int size() {
		return xValues.size();
	}

	public void sort() {
		ArrayList<PointD> points = new ArrayList<PointD>();
		for (int i = 0; i < xValues.size(); i++) {
			points.add(new PointD(xValues.get(i), yValues.get(i)));
		}

		Collections.sort(points);
		xValues.clear();
		yValues.clear();
		for (PointD point : points) {
			xValues.add((long) point.x);
			yValues.add(point.y);
		}
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
		if (xValues.size() < 2 || yValues.size() < 2) {
			return;
		}

		List<PointD> line = new ArrayList<PointD>();
		line.add(new PointD(xValues.get(xValues.size() - 2), yValues
				.get(yValues.size() - 2)));
		line.add(new PointD(xValues.lastElement(), yValues.lastElement()));
		markLines.add(line);
	}

	protected void markLastPoint() {
		markPoints
				.add(new PointD(xValues.lastElement(), yValues.lastElement()));
	}
}
