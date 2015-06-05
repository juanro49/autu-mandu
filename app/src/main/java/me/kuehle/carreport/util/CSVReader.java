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

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import me.kuehle.carreport.provider.othercost.RecurrenceInterval;
import me.kuehle.carreport.provider.reminder.TimeSpanUnit;

public class CSVReader {
    private static final String TAG = "CSVReader";

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
        } catch (IOException e) {
            Log.e(TAG, "Error while reading input file.", e);
        }

        return new CSVReader(data.toString(), hasHeader);
    }

    private String[] header;
    private String[][] data;

    private DateFormat dateFormat;
    private NumberFormat floatFormat;

    public CSVReader(String data, boolean hasHeader) {
        dateFormat = DateFormat.getDateTimeInstance();
        floatFormat = NumberFormat.getInstance();

        String[] rows = data.split(String.valueOf(NEW_LINE));
        ArrayList<String[]> dataRows = new ArrayList<>();
        int maxColumnCount = 0;

        // Collect data.
        for (String row : rows) {
            if (row.trim().length() == 0) {
                continue;
            }

            String[] cols = row.substring(1, row.length() - 1).split(
                    String.valueOf(QUOTE) + String.valueOf(SEPARATOR)
                            + String.valueOf(QUOTE));
            maxColumnCount = Math.max(maxColumnCount, cols.length);
            String[] parsedCols = new String[cols.length];
            for (int c = 0; c < cols.length; c++) {
                cols[c] = cols[c].replaceAll(
                        String.valueOf(ESCAPE) + String.valueOf(QUOTE),
                        String.valueOf(QUOTE));
                cols[c] = cols[c].replaceAll(
                        String.valueOf(ESCAPE) + String.valueOf(ESCAPE),
                        String.valueOf(ESCAPE));

                parsedCols[c] = cols[c];
            }

            dataRows.add(parsedCols);
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

    public Date getDate(int row, int col) {
        return parseDate(getString(row, col));
    }

    public Date getDate(int row, String title) {
        return parseDate(getString(row, title));
    }

    public Float getFloat(int row, int col) {
        return parseFloat(getString(row, col));
    }

    public Float getFloat(int row, String title) {
        return parseFloat(getString(row, title));
    }

    public Integer getInteger(int row, int col) {
        return parseInteger(getString(row, col));
    }

    public Integer getInteger(int row, String title) {
        return parseInteger(getString(row, title));
    }

    public Long getLong(int row, int col) {
        return parseLong(getString(row, col));
    }

    public Long getLong(int row, String title) {
        return parseLong(getString(row, title));
    }

    public Boolean getBoolean(int row, int col) {
        return parseBoolean(getString(row, col));
    }

    public Boolean getBoolean(int row, String title) {
        return parseBoolean(getString(row, title));
    }

    public RecurrenceInterval getRecurrenceInterval(int row, int col) {
        return parseRecurrenceInterval(getString(row, col));
    }

    public RecurrenceInterval getRecurrenceInterval(int row, String title) {
        return parseRecurrenceInterval(getString(row, title));
    }

    public TimeSpanUnit getTimeSpanUnit(int row, int col) {
        return parseTimeSpanUnit(getString(row, col));
    }

    public TimeSpanUnit getTimeSpanUnit(int row, String title) {
        return parseTimeSpanUnit(getString(row, title));
    }

    private Date parseDate(String value) {
        try {
            return dateFormat.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    private Float parseFloat(String value) {
        try {
            return floatFormat.parse(value).floatValue();
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean parseBoolean(String value) {
        if (value.isEmpty()) {
            return null;
        } else {
            return Boolean.parseBoolean(value);
        }
    }

    public RecurrenceInterval parseRecurrenceInterval(String value) {
        Integer intValue = parseInteger(value);
        if (intValue != null) {
            return RecurrenceInterval.values()[intValue];
        } else {
            return null;
        }
    }

    public TimeSpanUnit parseTimeSpanUnit(String value) {
        Integer intValue = parseInteger(value);
        if (intValue != null) {
            return TimeSpanUnit.values()[intValue];
        } else {
            return null;
        }
    }
}
