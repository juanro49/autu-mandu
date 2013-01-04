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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class CSVReader {
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

	public static CSVReader fromFile(File file, boolean hasHeader) {
		StringBuilder data = new StringBuilder();
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			String line;
			while ((line = in.readLine()) != null) {
				data.append(line).append(NEW_LINE);
			}
			in.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}

		return new CSVReader(data.toString(), hasHeader);
	}

	private String[] header;
	private String[][] data;

	public CSVReader(String data) {
		this(data, false);
	}

	public CSVReader(String data, boolean hasHeader) {
		String[] rows = data.split(String.valueOf(NEW_LINE));
		ArrayList<String[]> dataRows = new ArrayList<String[]>();
		int maxColumnCount = 0;

		// Collect data.
		for (int r = 0; r < rows.length; r++) {
			if (rows[r].trim().length() == 0) {
				continue;
			}

			String[] cols = rows[r].substring(1, rows[r].length() - 1).split(
					String.valueOf(QUOTE) + String.valueOf(SEPARATOR)
							+ String.valueOf(QUOTE));
			maxColumnCount = Math.max(maxColumnCount, cols.length);
			String[] row = new String[cols.length];
			for (int c = 0; c < cols.length; c++) {
				cols[c].replaceAll(
						String.valueOf(ESCAPE) + String.valueOf(QUOTE),
						String.valueOf(QUOTE));
				cols[c].replaceAll(
						String.valueOf(ESCAPE) + String.valueOf(ESCAPE),
						String.valueOf(ESCAPE));

				row[c] = cols[c];
			}

			dataRows.add(row);
		}

		// Fill columns to equal size.
		for (int r = 0; r < dataRows.size(); r++) {
			String[] row = dataRows.get(r);
			if (row.length < maxColumnCount) {
				String[] newRow = new String[maxColumnCount];
				Arrays.fill(newRow, "");
				System.arraycopy(row, 0, newRow, 0, row.length);
				dataRows.set(r, newRow);
			}
		}

		if (hasHeader && dataRows.size() > 0) {
			this.header = dataRows.get(0);
			dataRows.remove(0);
		} else {
			this.header = new String[0];
		}

		this.data = dataRows.toArray(new String[dataRows.size()][]);
	}

	public int getColumnCount() {
		return this.data.length > 0 ? this.data[0].length : 0;
	}

	public String[][] getData() {
		return data;
	}

	public int getRowCount() {
		return this.data.length;
	}

	public String getString(int row, int col) {
		return this.data[row][col];
	}

	public String getString(int row, String title) {
		for (int i = 0; i < header.length; i++) {
			if (header[i].equals(title)) {
				return getString(row, i);
			}
		}

		return null;
	}

	public float getFloat(int row, int col) {
		return parseFloat(getString(row, col));
	}

	public float getFloat(int row, String title) {
		return parseFloat(getString(row, title));
	}

	public int getInt(int row, int col) {
		return parseInt(getString(row, col));
	}

	public int getInt(int row, String title) {
		return parseInt(getString(row, title));
	}

	public long getLong(int row, int col) {
		return parseLong(getString(row, col));
	}

	public long getLong(int row, String title) {
		return parseLong(getString(row, title));
	}

	private float parseFloat(String value) {
		try {
			NumberFormat floatFormat = NumberFormat.getInstance();
			return floatFormat.parse(value).floatValue();
		} catch (Exception e) {
			return 0;
		}
	}

	private int parseInt(String value) {
		try {
			return Integer.parseInt(value);
		} catch (Exception e) {
			return 0;
		}
	}

	private long parseLong(String value) {
		try {
			return Long.parseLong(value);
		} catch (Exception e) {
			return 0;
		}
	}
}
