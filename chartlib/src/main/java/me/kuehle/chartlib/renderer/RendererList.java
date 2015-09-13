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

import java.util.ArrayList;

import me.kuehle.chartlib.chart.RectD;
import me.kuehle.chartlib.data.Series;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.SparseArray;

/**
 * This class holds all registered renderers and maps them to the {@link Series}
 * in the {@link Dataset}. If series are not explicitly mapped to a renderer
 * they are drawn by the first registered renderer.
 * <p>
 * Use {@link #addRenderer} to register a new renderer.<br>
 * Use {@link #mapSeriesToRenderer} to map a renderer to a series in the
 * dataset.
 */
public class RendererList {
	private ArrayList<AbstractRenderer> renderers = new ArrayList<AbstractRenderer>();
	private SparseArray<AbstractRenderer> mapping = new SparseArray<AbstractRenderer>();

	public RendererList() {
	}

	/**
	 * Registers a new renderer.
	 * 
	 * @param renderer
	 */
	public void addRenderer(AbstractRenderer renderer) {
		renderers.add(renderer);
	}

	/**
	 * Performes a click on the specified points. This will trigger the onClick
	 * event for all renderers, if a point is hit.
	 * 
	 * @param point
	 */
	public void click(PointF point) {
		for (AbstractRenderer renderer : renderers) {
			if (renderer instanceof Clickable) {
				((Clickable) renderer).click(point);
			}
		}
	}

	/**
	 * Draws all specified series on the canvas, taking in account the margins
	 * and axisBounds.
	 * 
	 * @param canvas
	 * @param margins
	 * @param axisBounds
	 * @param series
	 */
	public void draw(Canvas canvas, RectF margins, RectD axisBounds,
			Series[] series) {
		// Map renderers for series, which do not currently have one.
		for (int i = 0; i < series.length; i++) {
			if (mapping.get(i) == null) {
				mapping.put(i, renderers.get(0));
			}
		}

		// Draw series
		for (AbstractRenderer renderer : renderers) {
			SparseArray<Series> renderSeries = new SparseArray<Series>();
			for (int i = 0; i < series.length; i++) {
				if (mapping.get(i) == renderer) {
					renderSeries.append(i, series[i]);
				}
			}

			if (renderSeries.size() > 0) {
				renderer.draw(canvas, margins, axisBounds, renderSeries);
			}
		}
	}

	/**
	 * Returns the renderer, that is associated with the given series. If no
	 * renderer explicitly mapped, null will be returned.
	 * 
	 * @param series
	 *            the index of the series.
	 * @return the mapped renderer or null.
	 */
	public AbstractRenderer getRendererForSeries(int series) {
		return mapping.get(series, renderers.get(0));
	}

	/**
	 * Checks if all registered renderers, if they would draw something with the
	 * specified series.
	 * 
	 * @param series
	 * @return true, if at least one o the renderers would draw something.
	 */
	public boolean isEnoughData(Series[] series) {
		// Map renderers for series, which do not currently have one.
		for (int i = 0; i < series.length; i++) {
			if (mapping.get(i) == null) {
				mapping.put(i, renderers.get(0));
			}
		}

		// Ask renderers, if they would draw something.
		for (AbstractRenderer renderer : renderers) {
			SparseArray<Series> renderSeries = new SparseArray<Series>();
			for (int i = 0; i < series.length; i++) {
				if (mapping.get(i) == renderer) {
					renderSeries.append(i, series[i]);
				}
			}

			if (renderSeries.size() > 0 && renderer.isEnoughData(renderSeries)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Explicitly maps a renderer to a series.
	 * 
	 * @param series
	 *            the index of the series.
	 * @param renderer
	 *            the renderer. If it is not already registered, it will then
	 *            be.
	 */
	public void mapSeriesToRenderer(int series, AbstractRenderer renderer) {
		if (!renderers.contains(renderer)) {
			addRenderer(renderer);
		}

		mapping.put(series, renderer);
	}

	/**
	 * Removes a renderer.
	 * 
	 * @param renderer
	 */
	public void removeRenderer(AbstractRenderer renderer) {
		renderers.remove(renderer);
	}
}
