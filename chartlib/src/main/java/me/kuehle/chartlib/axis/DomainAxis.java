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
 * The domain axis.
 */
public class DomainAxis extends AbstractAxis {
	public DomainAxis(double topBound, double bottomBound, Size fontSize) {
		super(topBound, bottomBound, fontSize);
	}

	@Override
	public void draw(Canvas canvas, RectF area) {
		paint.setColor(getGridColor());
		float lineY = area.bottom - measureSize();
		canvas.drawLine(area.left, lineY, area.right, lineY, paint);

		for (double label : labels) {
			double scale = (area.right - area.left) / (topBound - bottomBound);
			float middle = (float) ((label - bottomBound) * scale) + area.left;
			if (middle < area.left || middle > area.right) {
				continue;
			}

			paint.setColor(getGridColor());
			canvas.drawLine(middle, lineY + LINE_WIDTH, middle, lineY
					+ LINE_WIDTH + LABEL_TICK_LENGTH, paint);
			if (isShowGrid()) {
				canvas.drawLine(middle, area.top, middle, lineY, paint);
			}

			paint.setColor(getFontColor());
			String text = labelFormatter.formatLabel(label);
			float textWidth = paint.measureText(text);
			canvas.drawText(text, middle - (textWidth / 2),
					area.bottom - paint.descent(), paint);
		}
	}

	@Override
	protected float getMaxLabelSize() {
		return Math.max(
				paint.measureText(labelFormatter.formatLabel(topBound)),
				paint.measureText(labelFormatter.formatLabel(bottomBound)));
	}

	@Override
	protected boolean isDomain() {
		return true;
	}

	@Override
	public int measureSize() {
		return getFontSize() + LABEL_TICK_GAP + LABEL_TICK_LENGTH + LINE_WIDTH;
	}
}
