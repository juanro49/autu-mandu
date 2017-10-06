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
 *
 * This file was written by Michael Wodniok as contribution.
 */
package me.kuehle.carreport.data.report;

import android.graphics.Color;

class ReportColor {
    static final float RED_TO_GREY_MULTIPLICATOR = 0.299f;
    static final float GREEN_TO_GREY_MULTIPLICATOR = 0.587f;
    static final float BLUE_TO_GREY_MULTIPLICATOR = 0.114f;
    static final int INVERT_COLOR_UPPER_THRESHOLD = 30;
    static final int INVERT_COLOR_LOWER_THRESHOLD = 0;

    /**
     * Inverts a color if useful.
     * @param color The color to check
     * @return The color. If not inverted, the original will be returned.
     */
    static int invertIfUseful(int color) {
        int greyValue = (int) (Color.red(color) * RED_TO_GREY_MULTIPLICATOR +
                Color.green(color) * GREEN_TO_GREY_MULTIPLICATOR +
                Color.blue(color) * BLUE_TO_GREY_MULTIPLICATOR);
        if (INVERT_COLOR_LOWER_THRESHOLD <= greyValue && greyValue < INVERT_COLOR_UPPER_THRESHOLD) {
            return Color.rgb(255 - Color.red(color), 255 - Color.green(color),
                    255 - Color.blue(color));
        } else {
            return color;
        }
    }
}
