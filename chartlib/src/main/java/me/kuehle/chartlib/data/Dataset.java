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
 * A set of all data for the chart. The data is specified by {@link Series}.
 * <p>
 * Use {@link #add} to add a series.
 */
public class Dataset {
	public interface DataChangedListener {
		public abstract void onGraphDataChanged();
	}

	private ArrayList<Series> series = new ArrayList<Series>();
	private DataChangedListener dataChangedListener = null;

	public Dataset() {
	}

	/**
	 * Adds the specified series to the dataset.
	 * 
	 * @param series
	 */
	public void add(Series series) {
		this.series.add(series);
		series.setParent(this);
		graphDataChanged();
	}

	/**
	 * Gets the series at the specified index.
	 * 
	 * @param index
	 * @return the series at the index.
	 */
	public Series get(int index) {
		return series.get(index);
	}

	/**
	 * Gets all series.
	 * 
	 * @return an array of all series.
	 */
	public Series[] getAll() {
		return series.toArray(new Series[series.size()]);
	}

	/**
	 * Calls the {@link DataChangedListener}, if there is one registered. You
	 * shouldn't have to call this manually.
	 */
	public void graphDataChanged() {
		if (dataChangedListener != null) {
			dataChangedListener.onGraphDataChanged();
		}
	}

	/**
	 * Gets the maximum x value in the dataset.
	 * 
	 * @return the maximum x value.
	 */
	public double maxX() {
		double maxX = Double.MIN_VALUE;
		for (Series series : this.series) {
			maxX = Math.max(series.maxX(), maxX);
		}
		return maxX;
	}

	/**
	 * Gets the maximum y value in the dataset.
	 * 
	 * @return the maximum y value.
	 */
	public double maxY() {
		double maxY = Double.MIN_VALUE;
		for (Series series : this.series) {
			maxY = Math.max(series.maxY(), maxY);
		}
		return maxY;
	}

	/**
	 * Gets the minimum x value in the dataset.
	 * 
	 * @return the minimum x value.
	 */
	public double minX() {
		double minX = Double.MAX_VALUE;
		for (Series series : this.series) {
			minX = Math.min(series.minX(), minX);
		}
		return minX;
	}

	/**
	 * Gets the minimum y value in the dataset.
	 * 
	 * @return the minimum y value.
	 */
	public double minY() {
		double minY = Double.MAX_VALUE;
		for (Series series : this.series) {
			minY = Math.min(series.minY(), minY);
		}
		return minY;
	}

	/**
	 * Sets the {@link DataChangedListener}. You shouldn't use this method.
	 * 
	 * @param dataChangedListener
	 */
	public void setDataChangedListener(DataChangedListener dataChangedListener) {
		this.dataChangedListener = dataChangedListener;
	}

	/**
	 * Gets the number of series in the dataset.
	 * 
	 * @return the number of series.
	 */
	public int size() {
		return series.size();
	}
}
