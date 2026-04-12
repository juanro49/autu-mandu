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

package org.juanro.autumandu.util;

import java.util.Arrays;

/**
 * Utility class for mathematical calculations.
 */
public final class Calculator {

    private Calculator() {
        // Utility class
    }

    public static double avg(double... numbers) {
        return Arrays.stream(numbers).average().orElse(0.0);
    }

    public static double max(double... numbers) {
        return Arrays.stream(numbers).max().orElse(Double.MIN_VALUE);
    }

    public static double min(double... numbers) {
        return Arrays.stream(numbers).min().orElse(Double.MAX_VALUE);
    }

    public static double sum(double... numbers) {
        return Arrays.stream(numbers).sum();
    }

    // Generic versions for Boxed types (Double, Integer, Long, etc.)
    public static <T extends Number> double avg(T[] numbers) {
        return Arrays.stream(numbers)
                .mapToDouble(Number::doubleValue)
                .average()
                .orElse(0.0);
    }

    public static <T extends Number> double max(T[] numbers) {
        return Arrays.stream(numbers)
                .mapToDouble(Number::doubleValue)
                .max()
                .orElse(Double.MIN_VALUE);
    }

    public static <T extends Number> double min(T[] numbers) {
        return Arrays.stream(numbers)
                .mapToDouble(Number::doubleValue)
                .min()
                .orElse(Double.MAX_VALUE);
    }

    public static <T extends Number> double sum(T[] numbers) {
        return Arrays.stream(numbers)
                .mapToDouble(Number::doubleValue)
                .sum();
    }
}
