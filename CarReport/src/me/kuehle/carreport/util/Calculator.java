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

import java.util.Vector;

public class Calculator {
	@SuppressWarnings("unchecked")
	public static <E extends Number> E avg(Vector<E> numbers) {
		if (numbers.get(0) instanceof Double) {
			return (E) (Double) ((Double) sum(numbers) / numbers.size());
		} else if (numbers.get(0) instanceof Float) {
			return (E) (Float) ((Float) sum(numbers) / numbers.size());
		} else if (numbers.get(0) instanceof Integer) {
			return (E) (Integer) ((Integer) sum(numbers) / numbers.size());
		} else { // if (numbers.get(0) instanceof Long) {
			return (E) (Long) ((Long) sum(numbers) / numbers.size());
		}
	}

	@SuppressWarnings("unchecked")
	public static <E extends Number> E max(Vector<E> numbers) {
		if (numbers.get(0) instanceof Double) {
			Double max = Double.MIN_VALUE;
			for (E num : numbers) {
				max = Math.max(max, (Double) num);
			}
			return (E) max;
		} else if (numbers.get(0) instanceof Float) {
			Float max = Float.MIN_VALUE;
			for (E num : numbers) {
				max = Math.max(max, (Float) num);
			}
			return (E) max;
		} else if (numbers.get(0) instanceof Integer) {
			Integer max = Integer.MIN_VALUE;
			for (E num : numbers) {
				max = Math.max(max, (Integer) num);
			}
			return (E) max;
		} else { // if (numbers.get(0) instanceof Long) {
			Long max = Long.MIN_VALUE;
			for (E num : numbers) {
				max = Math.max(max, (Long) num);
			}
			return (E) max;
		}
	}

	@SuppressWarnings("unchecked")
	public static <E extends Number> E min(Vector<E> numbers) {
		if (numbers.get(0) instanceof Double) {
			Double min = Double.MAX_VALUE;
			for (E num : numbers) {
				min = Math.min(min, (Double) num);
			}
			return (E) min;
		} else if (numbers.get(0) instanceof Float) {
			Float min = Float.MAX_VALUE;
			for (E num : numbers) {
				min = Math.min(min, (Float) num);
			}
			return (E) min;
		} else if (numbers.get(0) instanceof Integer) {
			Integer min = Integer.MAX_VALUE;
			for (E num : numbers) {
				min = Math.min(min, (Integer) num);
			}
			return (E) min;
		} else { // if (numbers.get(0) instanceof Long) {
			Long min = Long.MAX_VALUE;
			for (E num : numbers) {
				min = Math.min(min, (Long) num);
			}
			return (E) min;
		}
	}

	@SuppressWarnings("unchecked")
	public static <E extends Number> E sum(Vector<E> numbers) {
		if (numbers.get(0) instanceof Double) {
			Double sum = Double.valueOf(0);
			for (E num : numbers) {
				sum += (Double) num;
			}
			return (E) sum;
		} else if (numbers.get(0) instanceof Float) {
			Float sum = Float.valueOf(0);
			for (E num : numbers) {
				sum += (Float) num;
			}
			return (E) sum;
		} else if (numbers.get(0) instanceof Integer) {
			Integer sum = Integer.valueOf(0);
			for (E num : numbers) {
				sum += (Integer) num;
			}
			return (E) sum;
		} else { // if (numbers.get(0) instanceof Long) {
			Long sum = Long.valueOf(0);
			for (E num : numbers) {
				sum += (Long) num;
			}
			return (E) sum;
		}
	}
}
