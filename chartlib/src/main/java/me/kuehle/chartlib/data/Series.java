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

package me.kuehle.chartlib.data;

import java.util.ArrayList;

/**
 * The class for a series. It holds points for one element in the chart, for
 * example for one line.
 * <p>
 * Use {@link #add} to add a new point.<br>
 * Use {@link #setTitle} to specify a title for the series, that can be shown in
 * the {@link Legend}.
 */
public class Series {
	private String title;
	private ArrayList<PointD> points = new ArrayList<PointD>();

	private Dataset parent;

	/**
	 * Create a new series with a no title.
	 */
	public Series() {
		this(null);
	}

	/**
	 * Creates a new series with the specified title.
	 * 
	 * @param title
	 */
	public Series(String title) {
		this.title = title;
	}

	/**
	 * Adds a new point to the end of the series.
	 * 
	 * @param x
	 *            the x-coordinate.
	 * @param y
	 *            the y-coordinate.
	 */
	public void add(double x, double y) {
		if (Double.isInfinite(x) || Double.isInfinite(y) || Double.isNaN(x)
				|| Double.isNaN(y)) {
			throw new IllegalArgumentException(
					"Parameter x or y is Infinite or NaN.");
		}

		points.add(new PointD(x, y));
		if (parent != null) {
			parent.graphDataChanged();
		}
	}

	/**
	 * Gets the point at the specified index.
	 * 
	 * @param index
	 *            the index of the point.
	 * @return the point at the index.
	 */
	public PointD get(int index) {
		return points.get(index);
	}

	/**
	 * Gets the title of the series.
	 * 
	 * @return the title.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Gets the maximum x value in this series.
	 * 
	 * @return the maximum x value.
	 */
	public double maxX() {
		double maxX = Double.MIN_VALUE;
		for (PointD point : points) {
			maxX = Math.max(point.x, maxX);
		}
		return maxX;
	}

	/**
	 * Gets the maximum y value in this series.
	 * 
	 * @return the maximum y value.
	 */
	public double maxY() {
		double maxY = Double.MIN_VALUE;
		for (PointD point : points) {
			maxY = Math.max(point.y, maxY);
		}
		return maxY;
	}

	/**
	 * Gets the minimum x value in this series.
	 * 
	 * @return the minimum x value.
	 */
	public double minX() {
		double minX = Double.MAX_VALUE;
		for (PointD point : points) {
			minX = Math.min(point.x, minX);
		}
		return minX;
	}

	/**
	 * Gets the minimum y value in this series.
	 * 
	 * @return the minimum y value.
	 */
	public double minY() {
		double minY = Double.MAX_VALUE;
		for (PointD point : points) {
			minY = Math.min(point.y, minY);
		}
		return minY;
	}

	/**
	 * Removes a point at the specified index.
	 * 
	 * @param index
	 *            the index of the point, that should be removed.
	 */
	public void removeAt(int index) {
		points.remove(index);
		if (parent != null) {
			parent.graphDataChanged();
		}
	}

	/**
	 * Sets the parent {@link Dataset}. You should not call this manually.
	 * 
	 * @param parent
	 */
	public void setParent(Dataset parent) {
		this.parent = parent;
	}

	/**
	 * Sets the title for the series.
	 * 
	 * @param title
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Gets the number of points in the series.
	 * 
	 * @return the number of points in the series.
	 */
	public int size() {
		return points.size();
	}
}
