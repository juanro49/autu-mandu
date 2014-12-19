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
		return numbers.length == 0 ? 0 : sum(numbers) / numbers.length;
	}

	public static Double avg(Double... numbers) {
		return numbers.length == 0 ? 0 : sum(numbers) / numbers.length;
	}

	public static float avg(float... numbers) {
		return numbers.length == 0 ? 0 : sum(numbers) / numbers.length;
	}

	public static Float avg(Float... numbers) {
		return numbers.length == 0 ? 0 : sum(numbers) / numbers.length;
	}

	public static int avg(int... numbers) {
		return numbers.length == 0 ? 0 : sum(numbers) / numbers.length;
	}

	public static Integer avg(Integer... numbers) {
		return numbers.length == 0 ? 0 : sum(numbers) / numbers.length;
	}

	public static long avg(long... numbers) {
		return numbers.length == 0 ? 0 : sum(numbers) / numbers.length;
	}

	public static Long avg(Long... numbers) {
		return numbers.length == 0 ? 0 : sum(numbers) / numbers.length;
	}

	public static double max(double... numbers) {
		double max = Double.MIN_VALUE;
		for (double number : numbers) {
			if (number > max) {
				max = number;
			}
		}

		return max;
	}

	public static Double max(Double... numbers) {
		Double max = Double.MIN_VALUE;
		for (Double number : numbers) {
			if (number > max) {
				max = number;
			}
		}

		return max;
	}

	public static float max(float... numbers) {
		float max = Float.MIN_VALUE;
		for (float number : numbers) {
			if (number > max) {
				max = number;
			}
		}

		return max;
	}

	public static Float max(Float... numbers) {
		Float max = Float.MIN_VALUE;
		for (Float number : numbers) {
			if (number > max) {
				max = number;
			}
		}

		return max;
	}

	public static int max(int... numbers) {
		int max = Integer.MIN_VALUE;
		for (int number : numbers) {
			if (number > max) {
				max = number;
			}
		}

		return max;
	}

	public static Integer max(Integer... numbers) {
		Integer max = Integer.MIN_VALUE;
		for (Integer number : numbers) {
			if (number > max) {
				max = number;
			}
		}

		return max;
	}

	public static long max(long... numbers) {
		long max = Long.MIN_VALUE;
		for (long number : numbers) {
			if (number > max) {
				max = number;
			}
		}

		return max;
	}

	public static Long max(Long... numbers) {
		Long max = Long.MIN_VALUE;
		for (Long number : numbers) {
			if (number > max) {
				max = number;
			}
		}

		return max;
	}

	public static double min(double... numbers) {
		double min = Double.MAX_VALUE;
		for (double number : numbers) {
			if (number < min) {
				min = number;
			}
		}

		return min;
	}

	public static Double min(Double... numbers) {
		Double min = Double.MAX_VALUE;
		for (Double number : numbers) {
			if (number < min) {
				min = number;
			}
		}

		return min;
	}

	public static float min(float... numbers) {
		float min = Float.MAX_VALUE;
		for (float number : numbers) {
			if (number < min) {
				min = number;
			}
		}

		return min;
	}

	public static Float min(Float... numbers) {
		Float min = Float.MAX_VALUE;
		for (Float number : numbers) {
			if (number < min) {
				min = number;
			}
		}

		return min;
	}

	public static int min(int... numbers) {
		int min = Integer.MAX_VALUE;
		for (int number : numbers) {
			if (number < min) {
				min = number;
			}
		}

		return min;
	}

	public static Integer min(Integer... numbers) {
		Integer min = Integer.MAX_VALUE;
		for (Integer number : numbers) {
			if (number < min) {
				min = number;
			}
		}

		return min;
	}

	public static long min(long... numbers) {
		long min = Long.MAX_VALUE;
		for (long number : numbers) {
			if (number < min) {
				min = number;
			}
		}

		return min;
	}

	public static Long min(Long... numbers) {
		Long min = Long.MAX_VALUE;
		for (Long number : numbers) {
			if (number < min) {
				min = number;
			}
		}

		return min;
	}

	public static double sum(double... numbers) {
		double sum = 0;
		for (double number : numbers) {
			sum += number;
		}

		return sum;
	}

	public static Double sum(Double... numbers) {
		Double sum = 0d;
		for (Double number : numbers) {
			sum += number;
		}

		return sum;
	}

	public static float sum(float... numbers) {
		float sum = 0;
		for (float number : numbers) {
			sum += number;
		}

		return sum;
	}

	public static Float sum(Float... numbers) {
		Float sum = 0f;
		for (Float number : numbers) {
			sum += number;
		}

		return sum;
	}

	public static int sum(int... numbers) {
		int sum = 0;
		for (int number : numbers) {
			sum += number;
		}

		return sum;
	}

	public static Integer sum(Integer... numbers) {
		Integer sum = 0;
		for (Integer number : numbers) {
			sum += number;
		}

		return sum;
	}

	public static long sum(long... numbers) {
		long sum = 0;
		for (long number : numbers) {
			sum += number;
		}

		return sum;
	}

	public static Long sum(Long... numbers) {
		Long sum = 0l;
		for (Long number : numbers) {
			sum += number;
		}

		return sum;
	}
}
