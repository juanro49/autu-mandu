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

/**
 * Applications should implement this, if they want to have a custom label
 * formatting. It can be applied to an axis by using
 * {@link AbstractAxis#setLabelFormatter}.
 */
public interface AxisLabelFormatter {
	/**
	 * This is called for every label and should format the vale in the desired
	 * way.
	 * 
	 * @param value
	 *            the value at the labels position.
	 * @return the formatted label.
	 */
	public abstract String formatLabel(double value);
}
