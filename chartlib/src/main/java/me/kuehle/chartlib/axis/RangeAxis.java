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

package me.kuehle.chartlib.axis;

import me.kuehle.chartlib.util.Size;
import android.graphics.Canvas;
import android.graphics.RectF;

/**
 * The range axis.
 */
public class RangeAxis extends AbstractAxis {
	public RangeAxis(double topBound, double bottomBound, Size fontSize) {
		super(topBound, bottomBound, fontSize);
	}

	@Override
	public void draw(Canvas canvas, RectF area) {
		paint.setColor(getGridColor());
		float lineX = area.left + measureSize();
		canvas.drawLine(lineX, area.top, lineX, area.bottom, paint);

		for (double label : labels) {
			double scale = (area.bottom - area.top) / (topBound - bottomBound);
			float middle = (float) ((topBound - label) * scale) + area.top;
			if (middle < area.top || middle > area.bottom) {
				continue;
			}

			paint.setColor(getGridColor());
			canvas.drawLine(lineX, middle, lineX - LABEL_TICK_LENGTH, middle,
					paint);
			if (isShowGrid()) {
				canvas.drawLine(lineX + LINE_WIDTH, middle, area.right, middle,
						paint);
			}

			paint.setColor(getFontColor());
			String text = labelFormatter.formatLabel(label);
			float textWidth = paint.measureText(text);
			canvas.drawText(text, lineX - LABEL_TICK_LENGTH - LABEL_TICK_GAP
					- textWidth, middle - (paint.ascent() / 2), paint);
		}
	}

	@Override
	protected float getMaxLabelSize() {
		return getFontSize();
	}

	@Override
	protected boolean isDomain() {
		return false;
	}

	@Override
	public int measureSize() {
		return (int) (Math.max(
				paint.measureText(labelFormatter.formatLabel(topBound)),
				paint.measureText(labelFormatter.formatLabel(bottomBound))))
				+ LABEL_TICK_GAP + LABEL_TICK_LENGTH + LINE_WIDTH;
	}
}
