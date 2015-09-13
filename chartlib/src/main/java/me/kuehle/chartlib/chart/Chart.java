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

package me.kuehle.chartlib.chart;

import me.kuehle.chartlib.axis.AbstractAxis;
import me.kuehle.chartlib.axis.DomainAxis;
import me.kuehle.chartlib.axis.RangeAxis;
import me.kuehle.chartlib.data.Dataset;
import me.kuehle.chartlib.renderer.OnClickListener;
import me.kuehle.chartlib.renderer.RendererList;
import me.kuehle.chartlib.util.Size;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.TypedValue;

/**
 * The chart.
 * <p>
 * Use {@link #setShowLegend} to specify whether a legend should be drawn.
 */
public class Chart implements Dataset.DataChangedListener {
	private Dataset dataset;
	private RendererList renderers = new RendererList();

	private Rect clip = new Rect();
	private RectF domainAxisArea = new RectF();
	private RectF rangeAxisArea = new RectF();
	private RectF rendererArea = new RectF();
	private RectF legendArea = new RectF();

	private AbstractAxis domainAxis;
	private AbstractAxis rangeAxis;

	private Legend legend;
	private boolean showLegend = true;

	/**
	 * Creates a new chart with the given {@link Dataset} and
	 * {@link RendererList}.
	 * 
	 * @param context
	 * @param dataset
	 * @param renderers
	 */
	public Chart(Context context, Dataset dataset, RendererList renderers) {
		this.dataset = dataset;
		this.dataset.setDataChangedListener(this);
		this.renderers = renderers;

		double maxX = dataset.maxX();
		double minX = dataset.minX();
		if (maxX == minX) {
			maxX *= 1.001;
			minX *= 0.999;
		}
		domainAxis = new DomainAxis(maxX, minX, new Size(context, 12,
				TypedValue.COMPLEX_UNIT_SP));
		double maxY = dataset.maxY();
		double minY = dataset.minY();
		if (maxY == minY) {
			maxY *= 1.001;
			minY *= 0.999;
		}
		rangeAxis = new RangeAxis(maxY, minY, new Size(context, 12,
				TypedValue.COMPLEX_UNIT_SP));

		legend = new Legend(dataset, renderers, new Size(context, 12,
				TypedValue.COMPLEX_UNIT_SP));
	}

	/**
	 * Changes the size of the chart. You should not call this method manually.
	 * 
	 * @param width
	 *            the new width.
	 * @param height
	 *            the new height.
	 */
	public void changeSize(int width, int height) {
		domainAxis.changeSize(width, height);
		rangeAxis.changeSize(width, height);
	}

	/**
	 * Performs a click at the specified point. This will trigger the
	 * {@link OnClickListener} if a point in the graph is hit.
	 * 
	 * @param point
	 *            the position of the click.
	 */
	public void click(PointF point) {
		renderers.click(point);
	}

	/**
	 * Performs a double click at the specified point. This will restore the
	 * default position and zoom level.
	 * 
	 * @param point
	 */
	public void doubleClick(PointF point) {
		domainAxis.restoreDefaultBounds();
		rangeAxis.restoreDefaultBounds();
	}

	/**
	 * Draws the chart on the specified canvas.
	 * 
	 * @param canvas
	 */
	public void draw(Canvas canvas) {
		canvas.getClipBounds(clip);

		domainAxisArea.set(clip.left + rangeAxis.measureSize(), clip.top,
				clip.right, clip.bottom);
		domainAxis.draw(canvas, domainAxisArea);
		rangeAxisArea.set(clip.left, clip.top, clip.right, clip.bottom
				- domainAxis.measureSize());
		rangeAxis.draw(canvas, rangeAxisArea);

		RectD axisBounds = new RectD(rangeAxis.getTopBound(),
				domainAxis.getTopBound(), rangeAxis.getBottomBound(),
				domainAxis.getBottomBound());
		rendererArea.set(clip.left + rangeAxis.measureSize(), clip.top,
				clip.right, clip.bottom - domainAxis.measureSize());
		renderers.draw(canvas, rendererArea, axisBounds, dataset.getAll());

		if (showLegend) {
			legendArea.set(clip.left + rangeAxis.measureSize(), clip.top,
					clip.right, clip.bottom - domainAxis.measureSize());
			legend.draw(canvas, legendArea);
		}
	}

	public Dataset getDataset() {
		return dataset;
	}

	public AbstractAxis getDomainAxis() {
		return domainAxis;
	}

	public Legend getLegend() {
		return legend;
	}

	public AbstractAxis getRangeAxis() {
		return rangeAxis;
	}

	public RendererList getRenderers() {
		return renderers;
	}

	/**
	 * Checks, if there is enough data to draw anything.
	 * 
	 * @return true, if something would be drawn.
	 */
	public boolean hasEnoughData() {
		return renderers.isEnoughData(dataset.getAll());
	}

	/**
	 * Gets whether the legend is visible.
	 * 
	 * @return true, if the legend is visible.
	 */
	public boolean isShowLegend() {
		return showLegend;
	}

	/**
	 * Moves the chart by the specified distances.
	 * 
	 * @param distanceX
	 * @param distanceY
	 */
	public void move(float distanceX, float distanceY) {
		domainAxis.move(distanceX);
		rangeAxis.move(distanceY);
	}

	/**
	 * Applies new default bottom bounds for the axes.
	 */
	@Override
	public void onGraphDataChanged() {
		domainAxis.setDefaultBottomBound(dataset.minX());
		domainAxis.setDefaultTopBound(dataset.maxX());
		rangeAxis.setDefaultBottomBound(dataset.minY());
		rangeAxis.setDefaultTopBound(dataset.maxY());
	}

	/**
	 * Sets whether the legend should be visible.
	 * 
	 * @param showLegend
	 *            true, if the legend should be visible.
	 */
	public void setShowLegend(boolean showLegend) {
		this.showLegend = showLegend;
	}

	/**
	 * Zooms the chart by the specified distance around the specified point.
	 * 
	 * @param center
	 *            the center of the zoom.
	 * @param scaleDistance
	 *            the scale distance.
	 */
	public void zoom(PointF center, float scaleDistance) {
		domainAxis.zoom(center, scaleDistance);
		rangeAxis.zoom(center, scaleDistance);
	}
}
