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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.TypedValue;

/**
 * Abstract class for both domain and range axis.
 * <p>
 * Use {@link #setDefaultBottomBound} and {@link #setDefaultBottomBound} to
 * provide a default position and zoom level.<br>
 * Use {@link #setFontColor}, {@link #setFontSize}, {@link #setShowGrid} and
 * {@link #setGridColor} to customize the design of the axis.<br>
 * Use {@link #setLabels} to provide a custom set of labels and
 * {@link #setLabelFormatter} to change the format of the labels.<br>
 * Use {@link #setMovable} and {@link #setZoomable} to specify whether moving
 * and zooming along the axis should be possible.
 */
public abstract class AbstractAxis {
	private static final int FONT_SIZE_RELATIVE_LABEL_PADDING = 3;
	protected static final int LINE_WIDTH = 1;
	protected static final int LABEL_TICK_LENGTH = 3;
	protected static final int LABEL_TICK_GAP = 3;

	private double defaultTopBound;
	private double defaultBottomBound;
	private Size fontSize;
	private int fontColor = Color.LTGRAY;
	private int gridColor = Color.DKGRAY;
	private boolean showGrid = true;
	private boolean movable = true;
	private boolean zoomable = true;
	private boolean autoGenerateLabels = true;

	protected double topBound;
	protected double bottomBound;
	protected int width;
	protected int height;
	protected AxisLabelFormatter labelFormatter;
	protected double[] labels;

	protected Paint paint;

	public AbstractAxis(double topBound, double bottomBound, Size fontSize) {
		this.defaultTopBound = topBound;
		this.defaultBottomBound = bottomBound;
		this.topBound = topBound;
		this.bottomBound = bottomBound;
		this.fontSize = fontSize;

		labelFormatter = new IntegerAxisLabelFormatter();
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setStyle(Paint.Style.STROKE);
		paint.setTextSize(fontSize.getSizeInPixel());
	}

	/**
	 * Changes the size of the axis and calculates new labels. You should not
	 * call this method manually.
	 * 
	 * @param width
	 * @param height
	 */
	public void changeSize(int width, int height) {
		this.width = width;
		this.height = height;
		generateLabels();
	}

	/**
	 * Draws the axis on the canvas within the specified area.
	 * 
	 * @param canvas
	 * @param area
	 */
	public abstract void draw(Canvas canvas, RectF area);

	/**
	 * Generates a new set of labels, if labels aren't specified manually using
	 * {@link #setLabels}.
	 */
	private void generateLabels() {
		if (!autoGenerateLabels) {
			return;
		}

		double boundDistance = topBound - bottomBound;
		int prefLabelCount = (int) ((isDomain() ? width : height) / (getMaxLabelSize() + getPreferredLabelPadding()));
		if (prefLabelCount <= 0) {
			labels = new double[0];
			return;
		}

		double labelDataPadding = boundDistance / prefLabelCount;
		if (labelDataPadding == 0) {
			labels = new double[0];
			return;
		} else if (labelDataPadding < 1 && labelDataPadding > -1) {
			int i;
			for (i = 0; labelDataPadding < 1 && labelDataPadding > -1; i++) {
				labelDataPadding *= 10;
			}
			labelDataPadding = Math.rint(labelDataPadding);
			labelDataPadding /= Math.pow(10, i);
		} else if (labelDataPadding > 10 || labelDataPadding < -10) {
			int i;
			for (i = 0; labelDataPadding > 10 || labelDataPadding < -10; i++) {
				labelDataPadding /= 10;
			}
			labelDataPadding = Math.rint(labelDataPadding);
			labelDataPadding *= Math.pow(10, i);
		} else {
			labelDataPadding = Math.rint(labelDataPadding);
		}

		int labelCount = (int) Math.round(boundDistance / labelDataPadding);
		labels = new double[labelCount];
		if (labelCount > 0) {
			labels[0] = Math.ceil(bottomBound / labelDataPadding)
					* labelDataPadding;
			for (int i = 1; i < labelCount; i++) {
				labels[i] = labels[i - 1] + labelDataPadding;
			}
		}
	}

	public double getBottomBound() {
		return bottomBound;
	}

	public int getFontColor() {
		return fontColor;
	}

	/**
	 * Gets the font size in pixel.
	 * 
	 * @return the font size in pixel.
	 */
	public int getFontSize() {
		return fontSize.getSizeInPixel();
	}

	public int getGridColor() {
		return gridColor;
	}

	/**
	 * Gets the maximum label size. This should be either the maximum label
	 * width for domain axes or label height for range axes.
	 * 
	 * @return The maximum label size.
	 */
	protected abstract float getMaxLabelSize();

	private int getPreferredLabelPadding() {
		return fontSize.getSizeInPixel() * FONT_SIZE_RELATIVE_LABEL_PADDING;
	}

	public double getTopBound() {
		return topBound;
	}

	protected abstract boolean isDomain();

	public boolean isMovable() {
		return movable;
	}

	public boolean isShowGrid() {
		return showGrid;
	}

	public boolean isZoomable() {
		return zoomable;
	}

	public abstract int measureSize();

	/**
	 * Moves the chart along the axis with the specified distance.
	 * 
	 * @param distance
	 */
	public void move(float distance) {
		if (movable) {
			float size = isDomain() ? width : height;
			double scale = size / (topBound - bottomBound);

			if (isDomain()) {
				bottomBound += distance / scale;
				topBound += distance / scale;
			} else {
				bottomBound -= distance / scale;
				topBound -= distance / scale;
			}
			generateLabels();
		}
	}

	/**
	 * Restores the default bounds, which means reset move and zoom.
	 */
	public void restoreDefaultBounds() {
		topBound = defaultTopBound;
		bottomBound = defaultBottomBound;
		generateLabels();
	}

	/**
	 * Sets the default bottom bound. This is used, when resetting move or zoom.
	 * If the current bottom bound is equals the current default bottom bound,
	 * it will also be set to the new value.
	 * 
	 * @param defaultBottomBound
	 */
	public void setDefaultBottomBound(double defaultBottomBound) {
		if (bottomBound == this.defaultBottomBound) {
			bottomBound = defaultBottomBound;
			generateLabels();
		}
		this.defaultBottomBound = defaultBottomBound;
	}

	/**
	 * Set the default top bound. This is used, when resetting move or zoom. If
	 * the current top bound is equals the current default top bound, it will
	 * also be set to the new value.
	 * 
	 * @param defaultTopBound
	 */
	public void setDefaultTopBound(double defaultTopBound) {
		if (topBound == this.defaultTopBound) {
			topBound = defaultTopBound;
			generateLabels();
		}
		this.defaultTopBound = defaultTopBound;
	}

	public void setFontColor(int fontColor) {
		this.fontColor = fontColor;
	}

	/**
	 * Sets the font size of the labels in pixel.
	 * 
	 * @param fontSize
	 *            the font size in pixel.
	 */
	public void setFontSize(int fontSize) {
		setFontSize(fontSize, TypedValue.COMPLEX_UNIT_PT);
	}

	/**
	 * Sets the font size of the labels in the specified unit type.
	 * 
	 * @param fontSize
	 *            the font size.
	 * @param type
	 *            the unit type as in {@link http
	 *            ://developer.android.com/reference
	 *            /android/util/TypedValue.html#TYPE_DIMENSION}.
	 */
	public void setFontSize(int fontSize, int type) {
		this.fontSize.setSize(fontSize);
		this.fontSize.setType(type);
		paint.setTextSize(this.fontSize.getSizeInPixel());
		generateLabels();
	}

	public void setGridColor(int gridColor) {
		this.gridColor = gridColor;
	}

	/**
	 * Sets the label formatter.
	 * 
	 * @param labelFormatter
	 */
	public void setLabelFormatter(AxisLabelFormatter labelFormatter) {
		this.labelFormatter = labelFormatter;
		generateLabels();
	}

	/**
	 * Manually sets the labels. When using this method, labels won't be
	 * generated automatically anymore. If you want to have automatically
	 * generated labels again, set labels to null.
	 * 
	 * @param labels
	 *            an array of the values, at with a label should be drawn, or
	 *            null, if labels should be generated automatically.
	 */
	public void setLabels(double[] labels) {
		if (labels == null) {
			autoGenerateLabels = true;
			generateLabels();
		} else {
			autoGenerateLabels = false;
			this.labels = labels;
		}
	}

	public void setMovable(boolean movable) {
		this.movable = movable;
	}

	public void setShowGrid(boolean showGrid) {
		this.showGrid = showGrid;
	}

	public void setZoomable(boolean zoomable) {
		this.zoomable = zoomable;
	}

	/**
	 * Zooms the chart along the axis at the specified center with the specified
	 * distance.
	 * 
	 * @param center
	 * @param scaleDistance
	 */
	public void zoom(PointF center, float scaleDistance) {
		if (zoomable) {
			float size = isDomain() ? width : height;
			float middle = isDomain() ? center.x : center.y;

			double middleValue = (((topBound - bottomBound) / size) * middle)
					+ bottomBound;
			bottomBound = middleValue
					- ((middleValue - bottomBound) * scaleDistance);
			topBound = middleValue + ((topBound - middleValue) * scaleDistance);

			generateLabels();
		}
	}
}
