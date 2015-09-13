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
import java.util.Collections;
import java.util.HashMap;

import me.kuehle.chartlib.chart.RectD;
import me.kuehle.chartlib.data.PointD;
import me.kuehle.chartlib.data.Series;
import me.kuehle.chartlib.util.Size;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.SparseArray;
import android.util.TypedValue;

/**
 * A renderer that draws bars for the specified {@link Series}.
 */
public class BarRenderer extends AbstractRenderer {
	/**
	 * This class holds y-coordinate and index of series, that belong to a
	 * x-coordinate.
	 */
	private class PointDataHolder {
		public float y;
		public int series;

		public PointDataHolder(float y, int series) {
			this.y = y;
			this.series = series;
		}
	}

	private final Size MAX_BAR_WIDTH = new Size(context, 50,
			TypedValue.COMPLEX_UNIT_DIP);

	public BarRenderer(Context context) {
		super(context);
	}

	@Override
	public void draw(Canvas canvas, RectF area, RectD axisBounds,
			SparseArray<Series> series) {
		// BarWidth (bw), BarGap (bg), PointGap (pg), SeriesCount (s)
		// bg = 0.5 * bw
		// pg = bw
		// XGap = (s/2)*bw + ((s-1)/2)*bg + pg + ((s-1)/2)*bg + (s/2)*bw
		// = s*bw + (s-1)*bg + pg
		// = s*bw + (s-1)*0.5*bw + bw
		// = (s+((s-1)*0.5)+1)*bw
		// = (1.5*s + 0.5) * bw
		// bw = XGap / (1.5*s + 0.5)
		//
		// XGaps can vary, so we do calculate it for the lowest xgap and
		// increase the point gap where necessary.

		// Group all points by their x value.
		HashMap<Float, ArrayList<PointDataHolder>> pointMap = new HashMap<Float, ArrayList<PointDataHolder>>();
		for (int s = 0; s < series.size(); s++) {
			for (int p = 0; p < series.valueAt(s).size(); p++) {
				PointF point = translate(series.valueAt(s).get(p), area,
						axisBounds);
				if (!pointMap.containsKey(point.x)) {
					pointMap.put(point.x, new ArrayList<PointDataHolder>());
				}
				pointMap.get(point.x).add(
						new PointDataHolder(point.y, series.keyAt(s)));
			}
		}

		// Calculate the lowest x gap.
		ArrayList<Float> xValues = new ArrayList<Float>(pointMap.keySet());
		Collections.sort(xValues);
		float minXGap = Float.MAX_VALUE;
		for (int i = 0; i < xValues.size() - 1; i++) {
			float xGap = Math.abs(xValues.get(i) - xValues.get(i + 1));
			minXGap = Math.min(minXGap, xGap);
		}

		// Calculate bar width and bar gap. The minimal point gap is not needed
		// for drawing.
		// Also calculate where y=0 is located, because all bars start from
		// there.
		float barWidth = Math.min(minXGap / (1.5f * series.size() + 0.5f),
				MAX_BAR_WIDTH.getSizeInPixel());
		float barGap = 0.5f * barWidth;
		float y0 = translate(new PointD(0, 0), area, axisBounds).y;

		// Draw
		canvas.save();
		canvas.clipRect(area);
		for (float x : xValues) {
			if (x < area.left || x > area.right) {
				continue;
			}

			ArrayList<PointDataHolder> pointDataHolders = pointMap.get(x);
			float left = x
					- (((pointDataHolders.size() * barWidth) + ((pointDataHolders
							.size() - 1) * barGap)) / 2);
			for (PointDataHolder pointDataHolder : pointDataHolders) {
				PointF point = new PointF(x, pointDataHolder.y);
				if ((point.y <= area.bottom && point.y >= area.top)
						|| (y0 <= area.bottom && y0 >= area.top)) {
					float top = Math.max(Math.min(point.y, y0), area.top);
					float bottom = Math.min(Math.max(point.y, y0), area.bottom);
					canvas.drawRect(left, top, left + barWidth, bottom,
							getSeriesPaint(pointDataHolder.series));
				}

				left += barWidth + barGap;
			}
		}
		canvas.restore();
	}

	@Override
	public boolean isEnoughData(SparseArray<Series> series) {
		for (int s = 0; s < series.size(); s++) {
			if (series.get(s).size() >= 1) {
				return true;
			}
		}
		return false;
	}
}
