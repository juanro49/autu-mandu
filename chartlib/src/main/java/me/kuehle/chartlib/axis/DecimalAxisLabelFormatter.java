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
 * A predefined label formatter, which formats the labels as decimals with the
 * specified amount of digits.
 */
public class DecimalAxisLabelFormatter implements AxisLabelFormatter {
	private String format;

	/**
	 * Creates a new label formatter with the specified amount of digits.
	 * 
	 * @param digits
	 */
	public DecimalAxisLabelFormatter(int digits) {
		format = "%." + digits + "f";
	}

	@Override
	public String formatLabel(double value) {
		return String.format(format, value);
	}
}
