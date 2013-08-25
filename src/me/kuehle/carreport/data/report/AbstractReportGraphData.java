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
import java.util.Vector;

import me.kuehle.carreport.R;
import me.kuehle.carreport.util.Calculator;
import me.kuehle.chartlib.data.PointD;
import me.kuehle.chartlib.data.Series;
import me.kuehle.chartlib.renderer.AbstractRenderer;
import me.kuehle.chartlib.renderer.LineRenderer;
import android.content.Context;
import android.graphics.DashPathEffect;
import android.util.TypedValue;

public abstract class AbstractReportGraphData {
	private class RegressionReportData extends AbstractReportGraphData {
		public RegressionReportData(AbstractReportGraphData data) {
			super(data.context, data.context.getString(
					R.string.report_trend_label, data.name), data.color);

			if (data.xValues.size() == 0 || data.yValues.size() == 0) {
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

	protected Context context;
	protected String name;
	protected int color;

	protected Vector<Long> xValues = new Vector<Long>();
	protected Vector<Double> yValues = new Vector<Double>();

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
		}
	}

	public AbstractReportGraphData createRegressionData() {
		this.sort();
		return new RegressionReportData(this);
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
}
