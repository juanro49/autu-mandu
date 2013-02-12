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

import java.util.List;
import java.util.Map;

public class Strings {
	public static String join(String[] elements, String separator) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < elements.length; i++) {
			if (i != 0) {
				sb.append(separator);
			}
			sb.append(elements[i]);
		}
		return sb.toString();
	}

	public static String join(List<String> elements, String separator) {
		return join(elements.toArray(new String[elements.size()]), separator);
	}

	public static String replaceMap(String s, Map<String, String> replacements) {
		for (String key : replacements.keySet()) {
			s = s.replaceAll(key, replacements.get(key));
		}
		return s;
	}
}
