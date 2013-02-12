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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.Format;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

import android.database.Cursor;
import android.util.SparseArray;

public class CSVWriter {
	public static class SpecialColumnType {
		private Class<?> type;
		private Format format;

		public SpecialColumnType(Class<?> type, Format format) {
			this.type = type;
			this.format = format;
		}

		public String format(String value) {
			if (type == Date.class) {
				try {
					Date date = new Date(Long.parseLong(value));
					return format.format(date);
				} catch (NumberFormatException e) {
					return "";
				}
			} else {
				return "";
			}
		}
	}

	private static final char QUOTE = '"';
	private static final char ESCAPE = '\\';
	private static final char SEPARATOR;
	private static final char NEW_LINE = '\n';

	static {
		String locale = Locale.getDefault().getLanguage().substring(0, 2)
				.toLowerCase(Locale.US);
		if (locale.equals("de")) {
			SEPARATOR = ';';
		} else {
			SEPARATOR = ',';
		}
	}

	private StringBuilder data;

	public CSVWriter() {
		data = new StringBuilder();
	}

	public void write(Cursor cursor, SparseArray<SpecialColumnType> columnTypes) {
		write(cursor, columnTypes, false);
	}

	public void write(Cursor cursor,
			SparseArray<SpecialColumnType> columnTypes, boolean includeHeader) {
		if (includeHeader) {
			writeLine(cursor.getColumnNames());
		}

		NumberFormat floatFormat = NumberFormat.getInstance();
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			for (int i = 0; i < cursor.getColumnCount(); i++) {
				if (i != 0) {
					data.append(SEPARATOR);
				}

				if (columnTypes.get(i) != null) {
					writeColumn(columnTypes.get(i).format(cursor.getString(i)));
				} else if (cursor.getType(i) == Cursor.FIELD_TYPE_FLOAT) {
					writeColumn(floatFormat.format(cursor.getFloat(i)));
				} else {
					writeColumn(cursor.getString(i));
				}
			}
			data.append(NEW_LINE);
			cursor.moveToNext();
		}
	}

	public void writeLine(String[] line) {
		for (int i = 0; i < line.length; i++) {
			if (i != 0) {
				data.append(SEPARATOR);
			}
			writeColumn(line[i]);
		}
		data.append(NEW_LINE);
	}

	public void writeColumn(String column) {
		data.append(QUOTE);
		if (column != null) {
			for (int j = 0; j < column.length(); j++) {
				char nextChar = column.charAt(j);
				if (nextChar == QUOTE || nextChar == ESCAPE) {
					data.append(ESCAPE).append(nextChar);
				} else if (nextChar != NEW_LINE) {
					data.append(nextChar);
				}
			}
		}
		data.append(QUOTE);
	}

	public void toFile(File file) {
		try {
			PrintWriter out = new PrintWriter(file);
			out.write(toString());
			out.close();
		} catch (FileNotFoundException e) {
		}
	}

	@Override
	public String toString() {
		return data.toString();
	}
}
