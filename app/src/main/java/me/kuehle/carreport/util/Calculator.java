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

package me.kuehle.carreport.util;

public class Calculator {
    public static double avg(double... numbers) {
        return avg(0, numbers.length, numbers);
    }

    public static double avg(int startIndex, int count, double... numbers) {
        return count == 0 ? 0 : sum(startIndex, count, numbers) / numbers.length;
    }

    public static float avg(float... numbers) {
        return avg(0, numbers.length, numbers);
    }

    public static float avg(int startIndex, int count, float... numbers) {
        return count == 0 ? 0 : sum(startIndex, count, numbers) / numbers.length;
    }

    public static int avg(int... numbers) {
        return avg(0, numbers.length, numbers);
    }

    public static int avg(int startIndex, int count, int... numbers) {
        return count == 0 ? 0 : sum(startIndex, count, numbers) / numbers.length;
    }

    public static long avg(long... numbers) {
        return avg(0, numbers.length, numbers);
    }

    public static long avg(int startIndex, int count, long... numbers) {
        return count == 0 ? 0 : sum(startIndex, count, numbers) / numbers.length;
    }

    public static double max(double... numbers) {
        return max(0, numbers.length, numbers);
    }

    public static double max(int startIndex, int count, double... numbers) {
        double max = Double.MIN_VALUE;
        for (int i = startIndex, endIndex = startIndex + count; i < endIndex; i++) {
            if (numbers[i] > max) {
                max = numbers[i];
            }
        }

        return max;
    }

    public static float max(float... numbers) {
        return max(0, numbers.length, numbers);
    }

    public static float max(int startIndex, int count, float... numbers) {
        float max = Float.MIN_VALUE;
        for (int i = startIndex, endIndex = startIndex + count; i < endIndex; i++) {
            if (numbers[i] > max) {
                max = numbers[i];
            }
        }

        return max;
    }

    public static int max(int... numbers) {
        return max(0, numbers.length, numbers);
    }

    public static int max(int startIndex, int count, int... numbers) {
        int max = Integer.MIN_VALUE;
        for (int i = startIndex, endIndex = startIndex + count; i < endIndex; i++) {
            if (numbers[i] > max) {
                max = numbers[i];
            }
        }

        return max;
    }

    public static long max(long... numbers) {
        return max(0, numbers.length, numbers);
    }

    public static long max(int startIndex, int count, long... numbers) {
        long max = Long.MIN_VALUE;
        for (int i = startIndex, endIndex = startIndex + count; i < endIndex; i++) {
            if (numbers[i] > max) {
                max = numbers[i];
            }
        }

        return max;
    }

    public static double min(double... numbers) {
        return min(0, numbers.length, numbers);
    }

    public static double min(int startIndex, int count, double... numbers) {
        double min = Double.MAX_VALUE;
        for (int i = startIndex, endIndex = startIndex + count; i < endIndex; i++) {
            if (numbers[i] < min) {
                min = numbers[i];
            }
        }

        return min;
    }

    public static float min(float... numbers) {
        return min(0, numbers.length, numbers);
    }

    public static float min(int startIndex, int count, float... numbers) {
        float min = Float.MAX_VALUE;
        for (int i = startIndex, endIndex = startIndex + count; i < endIndex; i++) {
            if (numbers[i] < min) {
                min = numbers[i];
            }
        }

        return min;
    }

    public static int min(int... numbers) {
        return min(0, numbers.length, numbers);
    }

    public static int min(int startIndex, int count, int... numbers) {
        int min = Integer.MAX_VALUE;
        for (int i = startIndex, endIndex = startIndex + count; i < endIndex; i++) {
            if (numbers[i] < min) {
                min = numbers[i];
            }
        }

        return min;
    }

    public static long min(long... numbers) {
        return min(0, numbers.length, numbers);
    }

    public static long min(int startIndex, int count, long... numbers) {
        long min = Long.MAX_VALUE;
        for (int i = startIndex, endIndex = startIndex + count; i < endIndex; i++) {
            if (numbers[i] < min) {
                min = numbers[i];
            }
        }

        return min;
    }

    public static double sum(double... numbers) {
        return sum(0, numbers.length, numbers);
    }

    public static double sum(int startIndex, int count, double... numbers) {
        double sum = 0;
        for (int i = startIndex, endIndex = startIndex + count; i < endIndex; i++) {
            sum += numbers[i];
        }

        return sum;
    }

    public static float sum(float... numbers) {
        return sum(0, numbers.length, numbers);
    }

    public static float sum(int startIndex, int count, float... numbers) {
        float sum = 0;
        for (int i = startIndex, endIndex = startIndex + count; i < endIndex; i++) {
            sum += numbers[i];
        }

        return sum;
    }

    public static int sum(int... numbers) {
        return sum(0, numbers.length, numbers);
    }

    public static int sum(int startIndex, int count, int... numbers) {
        int sum = 0;
        for (int i = startIndex, endIndex = startIndex + count; i < endIndex; i++) {
            sum += numbers[i];
        }

        return sum;
    }

    public static long sum(long... numbers) {
        return sum(0, numbers.length, numbers);
    }

    public static long sum(int startIndex, int count, long... numbers) {
        long sum = 0;
        for (int i = startIndex, endIndex = startIndex + count; i < endIndex; i++) {
            sum += numbers[i];
        }

        return sum;
    }
}
