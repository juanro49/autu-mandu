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

import me.kuehle.chartlib.data.Dataset;
import me.kuehle.chartlib.renderer.AbstractRenderer;
import me.kuehle.chartlib.renderer.RendererList;
import me.kuehle.chartlib.util.Size;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.SparseBooleanArray;
import android.util.TypedValue;

/**
 * A legend, that shows the which series (title) belongs to which color.
 * <p>
 * Use {@link #setSeriesVisible} to specifiy if a series should show up in the
 * legend.
 */
public class Legend {
	private static final int PADDING = 5;

	private Dataset dataset;
	private RendererList renderers;
	private Size fontSize;
	private SparseBooleanArray seriesVisible = new SparseBooleanArray();

	private Paint paint;

	/**
	 * Creates a new legend.
	 * 
	 * @param dataset
	 * @param renderers
	 * @param fontSize
	 */
	public Legend(Dataset dataset, RendererList renderers, Size fontSize) {
		this.dataset = dataset;
		this.renderers = renderers;
		this.fontSize = fontSize;

		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setStyle(Paint.Style.FILL_AND_STROKE);
		paint.setTextSize(fontSize.getSizeInPixel());
	}

	/**
	 * Draws the legend on the canvas within the specified area.
	 * 
	 * @param canvas
	 * @param area
	 */
	public void draw(Canvas canvas, RectF area) {
		float y = area.bottom - PADDING;
		for (int s = dataset.size() - 1; s >= 0; s--) {
			if (isSeriesVisible(s) && dataset.get(s).getTitle() != null) {
				AbstractRenderer renderer = renderers.getRendererForSeries(s);
				paint.setColor(renderer.getSeriesColor(s));
				canvas.drawRect(area.right - getFontSize() - PADDING, y
						- getFontSize(), area.right - PADDING, y, paint);

				String title = dataset.get(s).getTitle();
				float textWidth = paint.measureText(title);
				paint.setColor(Color.LTGRAY);
				canvas.drawText(title, area.right - PADDING - getFontSize()
						- PADDING - textWidth, y, paint);

				y -= getFontSize() + PADDING;
			}
		}
	}

	/**
	 * Gets the font size for the series titles in pixel.
	 * 
	 * @return the font size in pixel.
	 */
	public int getFontSize() {
		return fontSize.getSizeInPixel();
	}

	/**
	 * Gets whether the specified series should show up in the legend.
	 * 
	 * @param series
	 *            the index of the series.
	 * @return true, if the series is visible in the legend.
	 */
	public boolean isSeriesVisible(int series) {
		return seriesVisible.get(series, true);
	}

	/**
	 * Sets the font size for the series titles in pixel.
	 * 
	 * @param fontSize
	 *            the font size in pixel.
	 */
	public void setFontSize(int fontSize) {
		setFontSize(fontSize, TypedValue.COMPLEX_UNIT_PT);
	}

	/**
	 * Sets the font size for the series titles in the specified unit type.
	 * 
	 * @param fontSize
	 *            the font size.
	 * @param type
	 *            the unit type as in {@link http
	 *            ://developer.android.com/reference
	 *            /android/util/TypedValue.html#TYPE_DIMENSION}
	 */
	public void setFontSize(int fontSize, int type) {
		this.fontSize.setSize(fontSize);
		this.fontSize.setType(type);
		paint.setTextSize(this.fontSize.getSizeInPixel());
	}

	/**
	 * Sets whether the specified series should show up in the legend. By
	 * default all series show up.
	 * 
	 * @param series
	 *            the index of the series.
	 * @param visible
	 *            true, of the series should be visible in the legend.
	 */
	public void setSeriesVisible(int series, boolean visible) {
		seriesVisible.put(series, visible);
	}
}
