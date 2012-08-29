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

package me.kuehle.carreport.reports;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Date;
import java.util.Vector;

import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.renderer.BasicStroke;
import org.achartengine.renderer.XYSeriesRenderer;

import me.kuehle.carreport.R;
import me.kuehle.carreport.util.Calculator;
import android.content.Context;
import android.graphics.Color;

public abstract class AbstractReportGraphData {
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

	public AbstractReportGraphData createRegressionData() {
		final AbstractReportGraphData data = this;
		return new AbstractReportGraphData(context, context.getString(
				R.string.report_trend_label, data.name), data.color) {
			{
				long avgX = Calculator.avg(data.xValues);
				double avgY = Calculator.avg(data.yValues);

				BigInteger sum1 = BigInteger.ZERO; // (x_i - avg(X)) ^ 2
				BigDecimal sum2 = BigDecimal.ZERO; // (x_i - avg(X)) * (y_i -
													// avg(Y))
				for (int i = 0; i < data.size(); i++) {
					BigInteger xMinusAvgX = BigInteger.valueOf(data.xValues
							.get(i) - avgX);
					BigDecimal yMinusAvgY = BigDecimal.valueOf(data.yValues
							.get(i) - avgY);
					sum1 = sum1.add(xMinusAvgX.multiply(xMinusAvgX));
					sum2 = sum2.add(yMinusAvgY.multiply(new BigDecimal(
							xMinusAvgX)));
				}

				double beta1 = sum2.divide(new BigDecimal(sum1),
						MathContext.DECIMAL128).doubleValue();
				double beta0 = avgY - (beta1 * avgX);

				xValues.add(data.xValues.firstElement());
				yValues.add(beta0 + (beta1 * data.xValues.firstElement()));
				xValues.add(data.xValues.lastElement());
				yValues.add(beta0 + (beta1 * data.xValues.lastElement()));
			}

			@Override
			public XYSeriesRenderer getRenderer() {
				XYSeriesRenderer renderer = new XYSeriesRenderer();
				applyTrendStyle(renderer, color);
				return renderer;
			}
		};
	}

	public abstract XYSeriesRenderer getRenderer();

	public TimeSeries getSeries() {
		TimeSeries series = new TimeSeries(name);
		for (int i = 0; i < size(); i++) {
			series.add(new Date(xValues.get(i)), yValues.get(i));
		}
		return series;
	}

	public int size() {
		return xValues.size();
	}

	protected void applyDefaultStyle(XYSeriesRenderer renderer, int color,
			boolean fill) {
		Calculator calc = new Calculator(context);

		renderer.setColor(color);
		renderer.setPointStyle(PointStyle.CIRCLE);
		renderer.setFillPoints(true);
		renderer.setLineWidth(calc.dpToPx(3));

		renderer.setFillBelowLine(fill);
		renderer.setFillBelowLineColor(Color.rgb(20, 40, 60));
	}

	protected void applyTrendStyle(XYSeriesRenderer renderer, int color) {
		Calculator calc = new Calculator(context);

		renderer.setColor(color);
		renderer.setLineWidth(calc.dpToPx(2));
		renderer.setStroke(BasicStroke.DASHED);
	}
}
