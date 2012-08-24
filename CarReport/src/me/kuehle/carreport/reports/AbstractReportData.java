package me.kuehle.carreport.reports;

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

public abstract class AbstractReportData {
	protected Context context;
	protected String name;
	protected int color;

	protected Vector<Long> xValues = new Vector<Long>();
	protected Vector<Double> yValues = new Vector<Double>();

	public AbstractReportData(Context context, String name, int color) {
		this.context = context;
		this.name = name;
		this.color = color;
	}

	public AbstractReportData createRegressionData() {
		final AbstractReportData data = this;
		return new AbstractReportData(context, context.getString(
				R.string.report_trend_label, data.name), data.color) {
			{
				long avgX = Calculator.avg(data.xValues);
				double avgY = Calculator.avg(data.yValues);

				long sum1 = 0; // (x_i - avg(X)) ^ 2
				double sum2 = 0; // (x_i - avg(X)) * (y_i - avg(Y))
				for (int i = 0; i < data.size(); i++) {
					long xMinusAvgX = data.xValues.get(i) - avgX;
					double yMinusAvgY = data.yValues.get(i) - avgY;
					sum1 += xMinusAvgX * xMinusAvgX;
					sum2 += xMinusAvgX * yMinusAvgY;
				}

				double beta1 = sum2 / sum1;
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
