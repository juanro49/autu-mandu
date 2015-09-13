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

/**
 * Applications, that want to be informed of clicks on specific points in the
 * chart have to implement this interface and register it using
 * {@link Clickable#setOnClickListener}.
 */
public interface OnClickListener {
	/**
	 * This method is executed, if a click is registered on a point in the
	 * chart.
	 * 
	 * @param series
	 *            the index of the series, that has been clicked.
	 * @param point
	 *            the index of the point in the series, that has been clicked.
	 * @param marked
	 *            true, if the point is marked; otherwise false.
	 */
	public abstract void onSeriesClick(int series, int point, boolean marked);
}
