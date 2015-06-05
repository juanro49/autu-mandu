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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

public class CSVWriter {
    private static final String TAG = "CSVWriter";

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

    private DateFormat dateFormat;
    private NumberFormat floatFormat;

    public CSVWriter() {
        data = new StringBuilder();

        dateFormat = DateFormat.getDateTimeInstance();
        floatFormat = NumberFormat.getInstance();
    }

    public void writeLine(Object... columns) {
        for (int i = 0; i < columns.length; i++) {
            if (i != 0) {
                data.append(SEPARATOR);
            }

            writeColumn(columns[i]);
        }

        data.append(NEW_LINE);
    }

    private void writeColumn(Object value) {
        data.append(QUOTE);
        if (value != null) {
            String strValue;
            if (value instanceof Enum) {
                strValue = format((Enum) value);
            } else if (value instanceof Date) {
                strValue = format((Date) value);
            } else if (value instanceof Float) {
                strValue = format((Float) value);
            } else {
                strValue = value.toString();
            }

            for (int j = 0; j < strValue.length(); j++) {
                char nextChar = strValue.charAt(j);
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
            Log.e(TAG, "Error while writing to file.", e);
        }
    }

    @Override
    public String toString() {
        return data.toString();
    }

    private String format(Enum value) {
        try {
            return String.valueOf(value.ordinal());
        } catch (Exception e) {
            return "";
        }
    }

    private String format(Date value) {
        try {
            return dateFormat.format(value);
        } catch (Exception e) {
            return "";
        }
    }

    private String format(Float value) {
        try {
            return floatFormat.format(value);
        } catch (Exception e) {
            return "";
        }
    }
}
