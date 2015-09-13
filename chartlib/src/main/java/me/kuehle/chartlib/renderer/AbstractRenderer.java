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

package me.kuehle.chartlib.renderer;

import me.kuehle.chartlib.chart.RectD;
import me.kuehle.chartlib.data.PointD;
import me.kuehle.chartlib.data.Series;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.SparseArray;
import android.util.SparseIntArray;

/**
 * Abstract class for all renderers.
 */
public abstract class AbstractRenderer {
	protected static final int DEFAULT_COLOR = Color.BLUE;

	protected Context context;
	protected SparseIntArray colors = new SparseIntArray();

	private Paint paint;

	public AbstractRenderer(Context context) {
		this.context = context;
		this.paint = new Paint();
		this.paint.setAntiAlias(true);
	}

	/**
	 * Draws the specified series on the canvas.
	 * 
	 * @param canvas
	 * @param area
	 * @param axisBounds
	 * @param series
	 */
	public abstract void draw(Canvas canvas, RectF area, RectD axisBounds,
			SparseArray<Series> series);

	/**
	 * Gets the color associated with the specified series.
	 * 
	 * @param series
	 *            the index of the series.
	 * @return
	 */
	public int getSeriesColor(int series) {
		return colors.get(series, DEFAULT_COLOR);
	}

	/**
	 * Gets the paint filled with the data for the specified series.
	 * 
	 * @param series
	 *            the index of the series.
	 * @return
	 */
	protected Paint getSeriesPaint(int series) {
		paint.setColor(getSeriesColor(series));
		return paint;
	}

	/**
	 * Checks if the renderer would draw something with the specified series.
	 * 
	 * @param series
	 * @return true, if the renderer would draw something.
	 */
	public abstract boolean isEnoughData(SparseArray<Series> series);

	/**
	 * Sets the associated color for the specified series.
	 * 
	 * @param series
	 *            the index of the series.
	 * @param color
	 *            a color as in {@link http
	 *            ://developer.android.com/reference/android
	 *            /graphics/Color.html}.
	 */
	public void setSeriesColor(int series, int color) {
		colors.put(series, color);
	}

	/**
	 * Translates the coordinates of a point in the dataset to coordinates on
	 * the canvas.
	 * 
	 * @param point
	 *            the point in the dataset.
	 * @param area
	 *            the area, where the renderer can draw in.
	 * @param axisBounds
	 *            the axisBounds that have to be taken into account.
	 * @return the coordinates on the canvas.
	 */
	protected PointF translate(PointD point, RectF area, RectD axisBounds) {
		double scaleX = (area.right - area.left)
				/ (axisBounds.getRight() - axisBounds.getLeft());
		double scaleY = (area.bottom - area.top)
				/ (axisBounds.getTop() - axisBounds.getBottom());

		float x = (float) ((point.x - axisBounds.getLeft()) * scaleX);
		float y = (float) ((axisBounds.getTop() - point.y) * scaleY);

		return new PointF(x + area.left, y + area.top);
	}
}
