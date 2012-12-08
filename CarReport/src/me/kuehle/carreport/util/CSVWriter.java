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
import java.text.NumberFormat;
import java.util.Locale;

import android.database.Cursor;

public class CSVWriter {
	private char quote;
	private char escape;
	private char separator;
	private char newLine;

	private StringBuilder data;

	public CSVWriter() {
		quote = '"';
		escape = '\\';
		newLine = '\n';
		String locale = Locale.getDefault().getLanguage()
				.substring(0, 2).toLowerCase(Locale.US);
		if (locale.equals("de")) {
			separator = ';';
		} else {
			separator = ',';
		}
		
		data = new StringBuilder();
	}

	public void write(Cursor cursor) {
		write(cursor, false);
	}

	public void write(Cursor cursor, boolean includeHeader) {
		if (includeHeader) {
			writeLine(cursor.getColumnNames());
		}

		NumberFormat floatFormat = NumberFormat.getInstance();
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			for (int i = 0; i < cursor.getColumnCount(); i++) {
				if (i != 0) {
					data.append(separator);
				}
				
				if(cursor.getType(i) == Cursor.FIELD_TYPE_FLOAT) {
					writeColumn(floatFormat.format(cursor.getFloat(i)));
				} else {
					writeColumn(cursor.getString(i));
				}
			}
			data.append(newLine);
			cursor.moveToNext();
		}
	}

	public void writeLine(String[] line) {
		for (int i = 0; i < line.length; i++) {
			if (i != 0) {
				data.append(separator);
			}
			writeColumn(line[i]);
		}
		data.append(newLine);
	}

	public void writeColumn(String column) {
		data.append(quote);
		for (int j = 0; j < column.length(); j++) {
			char nextChar = column.charAt(j);
			if (nextChar == quote || nextChar == escape) {
				data.append(escape).append(nextChar);
			} else if (nextChar != newLine) {
				data.append(nextChar);
			}
		}
		data.append(quote);
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
