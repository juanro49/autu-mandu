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
import java.util.List;

import me.kuehle.chartlib.chart.RectD;
import me.kuehle.chartlib.data.PointD;
import me.kuehle.chartlib.data.Series;
import me.kuehle.chartlib.util.Size;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.TypedValue;

/**
 * A renderer that draws lines for the specified series.
 * <p>
 * Use {@link #setSeriesDrawPoints}, {@link #setSeriesFillBelowLine},
 * {@link #setSeriesLineWidth} and {@link #setSeriesPathEffect} to customize the
 * lines.
 */
public class LineRenderer extends AbstractRenderer implements Clickable {
	protected static final float RELATIVE_POINT_RADIUS = 0.75f;
	protected static final float RELATIVE_CLICK_RADIUS = 5;
	protected static final int DEFAULT_MARK_COLOR = Color.YELLOW;

	protected SparseArray<Size> lineWidths = new SparseArray<Size>();
	protected final Size defaultLineWidth = new Size(context, 1,
			TypedValue.COMPLEX_UNIT_PX);
	protected SparseArray<PathEffect> pathEffects = new SparseArray<PathEffect>();
	protected SparseBooleanArray drawPoints = new SparseBooleanArray();
	protected SparseBooleanArray fillBelowLine = new SparseBooleanArray();

	protected SparseIntArray markColors = new SparseIntArray();
	protected SparseArray<PathEffect> markPathEffects = new SparseArray<PathEffect>();
	protected SparseArray<List<PointD>> markPoints = new SparseArray<List<PointD>>();
	protected SparseArray<List<List<PointD>>> markLines = new SparseArray<List<List<PointD>>>();

	private OnClickListener onClickListener;
	private RectF lastDrawnArea;
	private RectD lastDrawnAxisBounds;
	private SparseArray<Series> lastDrawnSeries;

	private Path linePath;
	private Path fillPath;
	private Path pointPath;
	private Path markLinePath;
	private Path markPointPath;

	public LineRenderer(Context context) {
		super(context);
		this.linePath = new Path();
		this.fillPath = new Path();
		this.pointPath = new Path();
		this.markLinePath = new Path();
		this.markPointPath = new Path();
	}

	@Override
	public void click(PointF point) {
		if (onClickListener == null || lastDrawnArea == null
				|| lastDrawnAxisBounds == null || lastDrawnSeries == null) {
			return;
		}

		for (int s = 0; s < lastDrawnSeries.size(); s++) {
			if (!isSeriesDrawPoints(lastDrawnSeries.keyAt(s))) {
				continue;
			}
			float clickRadius = getPointRadius(lastDrawnSeries.keyAt(s))
					* RELATIVE_CLICK_RADIUS;
			for (int p = 0; p < lastDrawnSeries.valueAt(s).size(); p++) {
				int seriesKey = lastDrawnSeries.keyAt(s);
				PointD seriesPoint = lastDrawnSeries.valueAt(s).get(p);

				PointF pointInSeries = translate(seriesPoint, lastDrawnArea,
						lastDrawnAxisBounds);
				RectF region = new RectF(pointInSeries.x - clickRadius,
						pointInSeries.y - clickRadius, pointInSeries.x
								+ clickRadius, pointInSeries.y + clickRadius);
				if (region.contains(point.x, point.y)) {
					onClickListener.onSeriesClick(seriesKey, p,
							isSeriesMarkPoint(seriesKey, seriesPoint));
				}
			}
		}
	}

	@Override
	public void draw(Canvas canvas, RectF area, RectD axisBounds,
			SparseArray<Series> series) {
		// TODO: When points are top or bottom out, drawing is not good

		canvas.save();
		canvas.clipRect(area);

		for (int s = 0; s < series.size(); s++) {
			if (series.valueAt(s).size() == 0
					|| series.valueAt(s).maxX() <= axisBounds.getLeft()
					|| series.valueAt(s).minX() >= axisBounds.getRight()
					|| (series.valueAt(s).maxY() <= axisBounds.getBottom() && 0 <= axisBounds
							.getBottom())
					|| (series.valueAt(s).minY() >= axisBounds.getTop() && 0 >= axisBounds
							.getTop())) {
				continue;
			}

			linePath.reset();
			fillPath.reset();
			pointPath.reset();
			markLinePath.reset();
			markPointPath.reset();
			PointF startPoint = null;
			PointF endPoint = null;

			PointF prevPoint = null;
			PointF nextPoint = translate(series.valueAt(s).get(0), area,
					axisBounds);
			for (int p = 0; p < series.valueAt(s).size(); p++) {
				PointF point = nextPoint;
				if (p < series.valueAt(s).size() - 1) {
					nextPoint = translate(series.valueAt(s).get(p + 1), area,
							axisBounds);
				} else {
					nextPoint = null;
				}

				if (point.x < area.left) {
					// Point is left out and the next point is in.
					if (nextPoint != null && nextPoint.x >= area.left) {
						float m = (point.y - nextPoint.y)
								/ (point.x - nextPoint.x);
						float x = area.left;
						float y = nextPoint.y - ((nextPoint.x - x) * m);
						linePath.moveTo(x, y);
						markLinePath.moveTo(x, y);
						fillPath.moveTo(x, y);
						startPoint = new PointF(x, y);
					}
				} else if (point.x > area.right) {
					// Point is right out and the previous point is in.
					if (prevPoint != null && prevPoint.x <= area.right) {
						float m = (prevPoint.y - point.y)
								/ (prevPoint.x - point.x);
						float x = area.right;
						float y = point.y - ((point.x - x) * m);
						drawLine(series.keyAt(s), series.valueAt(s).get(p - 1),
								series.valueAt(s).get(p), x, y);
						fillPath.lineTo(x, y);
						endPoint = new PointF(x, y);
					}
					break;
				} else if (point.y < area.top) {
					// Point is top out
					// ... and previous point is in.
					if (prevPoint != null && prevPoint.y >= area.top) {
						float m = (prevPoint.y - point.y)
								/ (prevPoint.x - point.x);
						float y = area.top;
						float x = ((y - point.y) / m) + point.x;
						drawLine(series.keyAt(s), series.valueAt(s).get(p - 1),
								series.valueAt(s).get(p), x, y);
						fillPath.lineTo(x, y);
						endPoint = new PointF(x, y);
					}
					// ... and next point is in.
					if (nextPoint != null && nextPoint.y >= area.top) {
						float m = (point.y - nextPoint.y)
								/ (point.x - nextPoint.x);
						float y = area.top;
						float x = ((y - nextPoint.y) / m) + nextPoint.x;
						linePath.moveTo(x, y);
						markLinePath.moveTo(x, y);
						if (fillPath.isEmpty()) {
							fillPath.moveTo(x, y);
							startPoint = new PointF(x, y);
						} else {
							fillPath.lineTo(x, y);
							endPoint = new PointF(x, y);
						}
					}
				} else if (point.y > area.bottom) {
					// Point is bottom out
					// ... and previous point is in.
					if (prevPoint != null && prevPoint.y <= area.bottom) {
						float m = (prevPoint.y - point.y)
								/ (prevPoint.x - point.x);
						float y = area.bottom;
						float x = ((y - point.y) / m) + point.x;
						drawLine(series.keyAt(s), series.valueAt(s).get(p - 1),
								series.valueAt(s).get(p), x, y);
						fillPath.lineTo(x, y);
						endPoint = new PointF(x, y);
					}
					// ... and next point is in.
					if (nextPoint != null && nextPoint.y <= area.bottom) {
						float m = (point.y - nextPoint.y)
								/ (point.x - nextPoint.x);
						float y = area.bottom;
						float x = ((y - nextPoint.y) / m) + nextPoint.x;
						linePath.moveTo(x, y);
						markLinePath.moveTo(x, y);
						if (fillPath.isEmpty()) {
							fillPath.moveTo(x, y);
							startPoint = new PointF(x, y);
						} else {
							fillPath.lineTo(x, y);
							endPoint = new PointF(x, y);
						}
					}
				} else if (prevPoint == null) {
					// Point is in and point is first point.
					linePath.moveTo(point.x, point.y);
					markLinePath.moveTo(point.x, point.y);
					fillPath.moveTo(point.x, point.y);
					startPoint = new PointF(point.x, point.y);
					if (isSeriesDrawPoints(series.keyAt(s))) {
						drawPoint(series.keyAt(s), series.valueAt(s).get(p),
								point.x, point.y);
					}
				} else {
					// Point is in.
					drawLine(series.keyAt(s), series.valueAt(s).get(p - 1),
							series.valueAt(s).get(p), point.x, point.y);
					fillPath.lineTo(point.x, point.y);
					endPoint = new PointF(point.x, point.y);
					if (isSeriesDrawPoints(series.keyAt(s))) {
						drawPoint(series.keyAt(s), series.valueAt(s).get(p),
								point.x, point.y);
					}
				}

				prevPoint = point;
			}

			Paint paint = getSeriesPaint(series.keyAt(s));
			paint.setStyle(Paint.Style.STROKE);
			canvas.drawPath(linePath, paint);

			paint.setColor(getSeriesMarkColor(series.keyAt(s)));
			paint.setPathEffect(getSeriesMarkPathEffect(series.keyAt(s)));
			canvas.drawPath(markLinePath, paint);

			if (isSeriesFillBelowLine(series.keyAt(s))) {
				paint.setStyle(Paint.Style.FILL);
				paint.setColor(getSeriesFillBelowLineColor(series.keyAt(s)));
				paint.setPathEffect(getSeriesPathEffect(series.keyAt(s)));

				PointF dataBottomLeft = translate(new PointD(series.valueAt(s)
						.minX(), series.valueAt(s).minY()), area, axisBounds);
				PointF dataTopRight = translate(new PointD(series.valueAt(s)
						.maxX(), series.valueAt(s).maxY()), area, axisBounds);
				PointF zero = translate(new PointD(0, 0), area, axisBounds);
				if (startPoint == null) {
					startPoint = dataBottomLeft;
				}
				if (endPoint == null) {
					endPoint = dataTopRight;
				}

				fillPath.lineTo(
						Math.max(area.left,
								Math.min(dataTopRight.x, area.right)),
						Math.max(area.top, Math.min(endPoint.y, area.bottom)));
				fillPath.lineTo(
						Math.max(area.left,
								Math.min(dataTopRight.x, area.right)),
						Math.max(area.top, Math.min(zero.y, area.bottom)));
				fillPath.lineTo(
						Math.max(area.left,
								Math.min(dataBottomLeft.x, area.right)),
						Math.max(area.top, Math.min(zero.y, area.bottom)));
				fillPath.lineTo(
						Math.max(area.left,
								Math.min(dataBottomLeft.x, area.right)),
						Math.max(area.top, Math.min(startPoint.y, area.bottom)));

				canvas.drawPath(fillPath, paint);
			}

			paint.setStyle(Paint.Style.FILL_AND_STROKE);
			paint.setColor(getSeriesColor(series.keyAt(s)));
			paint.setPathEffect(getSeriesPathEffect(series.keyAt(s)));
			canvas.drawPath(pointPath, paint);

			paint.setColor(getSeriesMarkColor(series.keyAt(s)));
			paint.setPathEffect(getSeriesMarkPathEffect(series.keyAt(s)));
			canvas.drawPath(markPointPath, paint);
		}

		canvas.restore();

		lastDrawnArea = area;
		lastDrawnAxisBounds = axisBounds;
		lastDrawnSeries = series;
	}

	/**
	 * Gets the radius of points for the specified series.
	 * 
	 * @param series
	 *            the index of the series.
	 * @return the point radius for the series.
	 */
	protected float getPointRadius(int series) {
		int lineWidth = getSeriesLineWidth(series);
		return lineWidth * RELATIVE_POINT_RADIUS;
	}

	/**
	 * Gets the color that should be used to fill the space below the line for
	 * the specified series.
	 * 
	 * @param series
	 *            the index of the series.
	 * @return the color for the series.
	 */
	protected int getSeriesFillBelowLineColor(int series) {
		int color = getSeriesColor(series);
		return Color.argb(Color.alpha(color) / 4, Color.red(color),
				Color.green(color), Color.blue(color));
	}

	/**
	 * Gets the color that should be used to mark specified points and lines.
	 * 
	 * @param series
	 *            the index of the series.
	 * @return the mark color.
	 */
	public int getSeriesMarkColor(int series) {
		return markColors.get(series, DEFAULT_MARK_COLOR);
	}

	/**
	 * Gets the marked {@link http
	 * ://developer.android.com/reference/android/graphics/PathEffect.html} for
	 * the specified series.
	 * 
	 * @param series
	 *            the index of the series.
	 * @return the {@link http
	 *         ://developer.android.com/reference/android/graphics
	 *         /PathEffect.html} for the series.
	 */
	public PathEffect getSeriesMarkPathEffect(int series) {
		return markPathEffects.get(series);
	}

	/**
	 * Gets the line width in pixel for the specified series.
	 * 
	 * @param series
	 *            the index of the series.
	 * @return the line with in pixel for the series.
	 */
	public int getSeriesLineWidth(int series) {
		return lineWidths.get(series, defaultLineWidth).getSizeInPixel();
	}

	@Override
	protected Paint getSeriesPaint(int series) {
		Paint paint = super.getSeriesPaint(series);
		paint.setStrokeWidth(getSeriesLineWidth(series));
		paint.setPathEffect(getSeriesPathEffect(series));
		return paint;
	}

	/**
	 * Gets the {@link http
	 * ://developer.android.com/reference/android/graphics/PathEffect.html} for
	 * the specified series.
	 * 
	 * @param series
	 *            the index of the series.
	 * @return the {@link http
	 *         ://developer.android.com/reference/android/graphics
	 *         /PathEffect.html} for the series.
	 */
	public PathEffect getSeriesPathEffect(int series) {
		return pathEffects.get(series);
	}

	@Override
	public boolean isEnoughData(SparseArray<Series> series) {
		for (int s = 0; s < series.size(); s++) {
			if (series.get(s).size() >= 2) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Gets whether points should be drawn for the specified series.
	 * 
	 * @param series
	 *            the index of the series.
	 * @return true, if points are drawn.
	 */
	public boolean isSeriesDrawPoints(int series) {
		return drawPoints.get(series, true);
	}

	/**
	 * Gets whether the space below the line should be filled out for the
	 * specified series.
	 * 
	 * @param series
	 *            the index of the series.
	 * @return true, if the space should be filled.
	 */
	public boolean isSeriesFillBelowLine(int series) {
		return fillBelowLine.get(series, false);
	}

	/**
	 * Gets whether the specified point should be marked.
	 * 
	 * @param series
	 * @param point
	 * @return
	 */
	public boolean isSeriesMarkPoint(int series, PointD point) {
		List<PointD> points = markPoints.get(series);
		if (points == null) {
			return false;
		}

		return points.contains(point);
	}

	/**
	 * Gets whether the specified line should be marked.
	 * 
	 * @param series
	 * @param from
	 * @param to
	 * @return
	 */
	public boolean isSeriesMarkLine(int series, PointD from, PointD to) {
		List<List<PointD>> lines = markLines.get(series);
		if (lines == null) {
			return false;
		}

		for (List<PointD> line : lines) {
			if (line.get(0).equals(from) && line.get(1).equals(to)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void setOnClickListener(OnClickListener listener) {
		onClickListener = listener;
	}

	/**
	 * Sets whether points should be drawn for the specified series.
	 * 
	 * @param series
	 *            the index of the series.
	 * @param drawPoints
	 *            true, if points should be drawn.
	 */
	public void setSeriesDrawPoints(int series, boolean drawPoints) {
		this.drawPoints.put(series, drawPoints);
	}

	/**
	 * Sets whether the space below the line should be filled out for the
	 * specified series.
	 * 
	 * @param series
	 *            the index of the series.
	 * @param fill
	 *            true, if the space below the line should be filled.
	 */
	public void setSeriesFillBelowLine(int series, boolean fill) {
		fillBelowLine.put(series, fill);
	}

	/**
	 * Sets the color for marked points and lines on this series.
	 * 
	 * @param series
	 *            the index of the series.
	 * @param color
	 *            the color for the marked points and lines.
	 */
	public void setSeriesMarkColor(int series, int color) {
		markColors.put(series, color);
	}

	/**
	 * Adds a points of the series, that should be marked with the color
	 * specified through {@link #setSeriesMarkColor(int, int)}.
	 * 
	 * @param series
	 *            the index of the series.
	 * @param point
	 *            the point to mark.
	 */
	public void addSeriesMarkPoint(int series, PointD point) {
		if (markPoints.get(series) == null) {
			markPoints.put(series, new ArrayList<PointD>());
		}

		markPoints.get(series).add(point);
	}

	/**
	 * Adds a line of the series, that should be marked with the color specified
	 * through {@link #setSeriesMarkColor(int, int)}.
	 * 
	 * @param series
	 *            the index of the series.
	 * @param from
	 *            the start point of the line.
	 * @param to
	 *            the end point of the line.
	 */
	public void addSeriesMarkLine(int series, PointD from, PointD to) {
		if (markLines.get(series) == null) {
			markLines.put(series, new ArrayList<List<PointD>>());
		}

		ArrayList<PointD> line = new ArrayList<PointD>();
		line.add(from);
		line.add(to);

		markLines.get(series).add(line);
	}

	/**
	 * Sets the path effect for marked lines.
	 * 
	 * @param series
	 *            the index of the series.
	 * @param effect
	 *            the {@link http
	 *            ://developer.android.com/reference/android/graphics
	 *            /PathEffect.html}
	 */
	public void setSeriesMarkPathEffect(int series, PathEffect effect) {
		markPathEffects.put(series, effect);
	}

	/**
	 * Sets the line width of the specified series in pixel.
	 * 
	 * @param series
	 *            the index of the series.
	 * @param width
	 *            the with in pixel.
	 */
	public void setSeriesLineWidth(int series, int width) {
		setSeriesLineWidth(series, width, TypedValue.COMPLEX_UNIT_PX);
	}

	/**
	 * Sets the line width of the specified series in the specified unit type.
	 * 
	 * @param series
	 *            the index of the series.
	 * @param width
	 *            the width.
	 * @param type
	 *            the unit type as in {@link http
	 *            ://developer.android.com/reference
	 *            /android/util/TypedValue.html#TYPE_DIMENSION}
	 */
	public void setSeriesLineWidth(int series, int width, int type) {
		lineWidths.put(series, new Size(context, width, type));
	}

	/**
	 * Sets the {@link http
	 * ://developer.android.com/reference/android/graphics/PathEffect.html} for
	 * the specified series. For example, if you like to have a dashed line,
	 * use:
	 * 
	 * <pre>
	 * {@code
	 * renderer.setSeriesPathEffect(0, new DashPathEffect(new float[] { 5, 5 }, 0));
	 * }
	 * </pre>
	 * 
	 * @param series
	 *            the index of the series.
	 * @param effect
	 *            the {@link http
	 *            ://developer.android.com/reference/android/graphics
	 *            /PathEffect.html}
	 */
	public void setSeriesPathEffect(int series, PathEffect effect) {
		pathEffects.put(series, effect);
	}

	private void drawLine(int series, PointD from, PointD to, float toX,
			float toY) {
		if (isSeriesMarkLine(series, from, to)) {
			markLinePath.lineTo(toX, toY);
			linePath.moveTo(toX, toY);
		} else {
			markLinePath.moveTo(toX, toY);
			linePath.lineTo(toX, toY);
		}
	}

	private void drawPoint(int series, PointD point, float x, float y) {
		if (isSeriesMarkPoint(series, point)) {
			markPointPath.addCircle(x, y, getPointRadius(series), Direction.CW);
		} else {
			pointPath.addCircle(x, y, getPointRadius(series), Direction.CW);
		}
	}
}
