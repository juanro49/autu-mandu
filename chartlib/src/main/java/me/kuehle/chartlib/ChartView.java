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

package me.kuehle.chartlib;

import java.util.Timer;
import java.util.TimerTask;

import me.kuehle.chartlib.chart.Chart;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * A view, that can hold and display a {@link Chart}.
 * <p>
 * Use {@link #setNotEnoughDataView} to specify a view, that should be drawn
 * instead of the chart, if there is not enough data to draw anything.
 * <p>
 * To implement it, use the following in your layouts file:
 * 
 * <pre>
 * {@code
 * <me.kuehle.chartlib.ChartView
 *     android:id="@+id/chart_view"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent" />
 * }
 * </pre>
 * 
 * Then receive the chart view in your code, and set a chart using
 * {@link #setChart}:
 * 
 * <pre><code>
 * Dataset dataset = new Dataset();
 * Series s1 = new Series("Prices");
 * s1.add(0, 131.32);
 * s1.add(1, 128.75);
 * s1.add(2, 145.31);
 * dataset.add(s1);
 * 
 * RendererList renderers = new RendererList();
 * LineRenderer renderer = new LineRenderer(context);
 * renderers.addRenderer(renderer);
 * 
 * Chart chart = new Chart(context, dataset, renderers);
 * chart.getDomainAxis().setLabels(new double[] { 0, 1, 2 });
 * chart.getDomainAxis().setLabelFormatter(new AxisLabelFormatter() {
 *     private String[] months = { "Jan", "Feb", "Mar" };
 * 
 *     public String formatLabel(double value) {
 *         return months[(int) value];
 *     }
 * });
 * chart.getRangeAxis().setLabelFormatter(new DecimalAxisLabelFormatter(2));
 * 
 * ChartView chartView = (ChartView) findViewById(R.id.chart_view);
 * chartView.setChart(chart);
 * </code></pre>
 */
public class ChartView extends View {
	private static final int MIN_MOVE_DISTANCE = 0;
	private static final int MAX_DOUBLE_CLICK_INTERVAL = 200;
	private static final int MAX_CLICK_TIME = 400;

	private Chart chart = null;
	private View notEnoughDataView = null;

	// Move
	private PointF singleTouchStartPoint = null;

	// Zoom
	private double multiTouchStartDistance = -1;

	// Click and double click
	private long timeLastUp = 0;

	private long timeLastDown = 0;

	private Timer clickTimer = new Timer();
	private TimerTask clickTask = null;
	private Handler handler = new Handler();

	public ChartView(Context context) {
		super(context);
	}

	public ChartView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ChartView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public Chart getChart() {
		return chart;
	}

	public View getNotEnoughDataView() {
		return notEnoughDataView;
	}

	private void moveAdjustment(MotionEvent ev) {
		boolean moveTriggerX = Math.abs(ev.getX(0) - singleTouchStartPoint.x) >= MIN_MOVE_DISTANCE;
		boolean moveTriggerY = Math.abs(ev.getY(0) - singleTouchStartPoint.y) >= MIN_MOVE_DISTANCE;
		if (moveTriggerX || moveTriggerY) {
			float distanceX = singleTouchStartPoint.x - ev.getX(0);
			float distanceY = singleTouchStartPoint.y - ev.getY(0);

			if (chart != null && chart.hasEnoughData()) {
				chart.move(distanceX, distanceY);
				invalidate();
			}
		}

		singleTouchStartPoint.set(ev.getX(), ev.getY());
	}

	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (chart != null) {
			if (chart.hasEnoughData()) {
				chart.draw(canvas);
			} else if (notEnoughDataView != null) {
				notEnoughDataView.draw(canvas);
			}
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (chart != null) {
			chart.changeSize(w, h);
		}
		if (notEnoughDataView != null) {
			notEnoughDataView.measure(w, h);
			notEnoughDataView.layout(0, 0, w, h);
		}
	}

	public boolean onTouchEvent(MotionEvent ev) {
		int action = ev.getAction();
		int count = ev.getPointerCount();

		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_POINTER_DOWN:
			timeLastDown = System.currentTimeMillis();
			if (count == 2 && multiTouchStartDistance == -1) {
				multiTouchStartDistance = Math.sqrt(Math.pow(
						ev.getX(0) - ev.getX(1), 2)
						+ Math.pow(ev.getY(0) - ev.getY(1), 2));
			} else if (count == 1 && singleTouchStartPoint == null) {
				singleTouchStartPoint = new PointF(ev.getX(), ev.getY());
			}

			return true;
		case MotionEvent.ACTION_MOVE:
			if (count == 2 && multiTouchStartDistance != -1) {
				zoomAdjustment(ev);
			} else if (count == 1 && singleTouchStartPoint != null) {
				moveAdjustment(ev);
			}

			return true;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			if (count == 1 && singleTouchStartPoint != null) {
				long timeNow = System.currentTimeMillis();
				if (timeNow - timeLastDown < MAX_CLICK_TIME && chart != null
						&& chart.hasEnoughData()) {
					if (timeNow - timeLastUp < MAX_DOUBLE_CLICK_INTERVAL) {
						if (clickTask != null) {
							clickTask.cancel();
							clickTask = null;
						}
						chart.doubleClick(singleTouchStartPoint);
						invalidate();
					} else {
						timeLastUp = timeNow;
						clickTask = new TimerTask() {
							private PointF point = singleTouchStartPoint;

							@Override
							public void run() {
								handler.post(new Runnable() {
									@Override
									public void run() {
										if (!performClick()) {
											chart.click(point);
										}
									}
								});
							}
						};
						clickTimer.schedule(clickTask,
								MAX_DOUBLE_CLICK_INTERVAL);
					}
				}
			}

			if (count == 2) {
				multiTouchStartDistance = -1;
			} else if (count == 1) {
				singleTouchStartPoint = null;
			}

			return true;
		default:
			return super.onTouchEvent(ev);
		}
	}

	public void setChart(Chart chart) {
		this.chart = chart;
		if (chart != null) {
			chart.changeSize(getWidth(), getHeight());
		}
		invalidate();
	}

	/**
	 * Sets a view, that should be drawn instead of the chart, if there is not
	 * enough data to draw anything. This view will only be drawn if a chart is
	 * specified.
	 * 
	 * @param notEnoughDataView
	 */
	public void setNotEnoughDataView(View notEnoughDataView) {
		this.notEnoughDataView = notEnoughDataView;
		if (notEnoughDataView != null) {
			notEnoughDataView.measure(getWidth(), getHeight());
			notEnoughDataView.layout(0, 0, getWidth(), getHeight());
		}
		invalidate();
	}

	private void zoomAdjustment(MotionEvent ev) {
		PointF zoomCenter = new PointF((ev.getX(0) + ev.getX(1)) / 2,
				(ev.getY(0) + ev.getY(1)) / 2);
		double endDistance = Math.sqrt(Math.pow(ev.getX(0) - ev.getX(1), 2)
				+ Math.pow(ev.getY(0) - ev.getY(1), 2));

		// Zoom
		float scaleDistance = (float) (multiTouchStartDistance / endDistance);
		if (chart != null && chart.hasEnoughData()) {
			chart.zoom(zoomCenter, scaleDistance);
			invalidate();
		}

		multiTouchStartDistance = endDistance;
	}
}
