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

package me.kuehle.chartlib.util;

import android.content.Context;
import android.util.TypedValue;

/**
 * This class provides simple use of complex unit types like device independent
 * pixel, or scaled pixel.
 * <p>
 * Use {@link #getSizeInPixel()} to retrieve the value in pixel.
 */
public class Size {
	private Context context;
	private int size;
	private int type;

	/**
	 * Creates a new Size object.
	 * 
	 * @param context
	 *            an android context
	 * @param size
	 *            the value
	 * @param type
	 *            the unit type as in {@link http
	 *            ://developer.android.com/reference
	 *            /android/util/TypedValue.html#TYPE_DIMENSION}
	 */
	public Size(Context context, int size, int type) {
		this.context = context;
		this.size = size;
		this.type = type;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	/**
	 * Gets the unit type as in {@link http ://developer.android.com/reference
	 * /android/util/TypedValue.html#TYPE_DIMENSION}
	 * 
	 * @return the unit type
	 */
	public int getType() {
		return type;
	}

	/**
	 * Sets the unit type as in {@link http ://developer.android.com/reference
	 * /android/util/TypedValue.html#TYPE_DIMENSION}
	 * 
	 * @param type
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * Gets the size converted from the complex type to pixels.
	 * 
	 * @return the size converted in pixel.
	 */
	public int getSizeInPixel() {
		return (int) TypedValue.applyDimension(type, size, context
				.getResources().getDisplayMetrics());
	}
}
