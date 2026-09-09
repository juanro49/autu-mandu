/*
 * Copyright 2017 Jan Kühle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.juanro.autumandu.data.report;

import androidx.annotation.NonNull;
import java.util.Date;

/**
 * Utility class to convert between {@link Date} and {@code float}.
 * The chart library (Kubit) supports only float values. In order to fit timestamps in the
 * charts without losing too much precision, we convert the dates to a "days since base date" format.
 * 1. The integer part represents the days since the base date (usually the first record).
 * 2. The decimal part represents the time of day (fraction of 24h).
 * This approach keeps the values small (starting from 0.0), allowing 32-bit floats to maintain
 * second-level precision for several years of data.
 *
 * @see <a href="https://bitbucket.org/frigus02/car-report/issues/83">Issue #83 for more details</a>
 */
public final class ReportDateHelper {
    private static final float SECONDS_PER_DAY = 86400.0f;
    private static final float MILLIS_PER_SECOND = 1000.0f;

    private ReportDateHelper() {
        // Utility class
    }

    /**
     * Converts a Date to a float representation relative to the base date.
     *
     * @param date The date to convert.
     * @param baseTime The base time (in millis) that represents 0.0f.
     * @return Float representation (days since base date).
     */
    public static float toFloat(@NonNull Date date, long baseTime) {
        return ((date.getTime() - baseTime) / MILLIS_PER_SECOND) / SECONDS_PER_DAY;
    }

    /**
     * Converts a float representation back to a Date relative to the base date.
     *
     * @param date Float representation (days since base date).
     * @param baseTime The base time (in millis) that represents 0.0f.
     * @return The corresponding Date object.
     */
    public static @NonNull Date toDate(float date, long baseTime) {
        return new Date((long) (date * SECONDS_PER_DAY * MILLIS_PER_SECOND) + baseTime);
    }
}
