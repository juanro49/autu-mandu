/*
 * Copyright 2026 Juanro49
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

package org.juanro.autumandu.util.backup;

import org.apache.commons.csv.CSVFormat;

import java.util.Map;

public enum CSVTripFormat {
    GENERIC(
            CSVFormat.DEFAULT.builder()
                    .setDelimiter(',')
                    .setHeader("Date", "Date End", "Start Time", "End Time", "Route", "Purpose", "Km Start", "Km End", "Business Km", "Private Km", "Home-Work Km", "Start Lat", "Start Lon", "End Lat", "End Lon")
                    .setSkipHeaderRecord(true)
                    .get(),
            Map.ofEntries(
                    Map.entry("date", "Date"),
                    Map.entry("date_end", "Date End"),
                    Map.entry("time_start", "Start Time"),
                    Map.entry("time_end", "End Time"),
                    Map.entry("route", "Route"),
                    Map.entry("purpose", "Purpose"),
                    Map.entry("km_start", "Km Start"),
                    Map.entry("km_end", "Km End"),
                    Map.entry("km_business", "Business Km"),
                    Map.entry("km_private", "Private Km"),
                    Map.entry("km_home_work", "Home-Work Km"),
                    Map.entry("start_lat", "Start Lat"),
                    Map.entry("start_lon", "Start Lon"),
                    Map.entry("end_lat", "End Lat"),
                    Map.entry("end_lon", "End Lon")
            )
    ),
    SKODA(
            CSVFormat.DEFAULT.builder()
                    .setDelimiter(';')
                    .setHeader("Datum", "Datum Ende", "Abfahrt", "Ankunft", "Fahrtstrecke", "Fahrtzweck", "km-Stand Anfang", "km-Stand Ende", "geschäftlich", "privat", "Wohnung/Arbeit")
                    .setSkipHeaderRecord(true)
                    .get(),
            Map.ofEntries(
                    Map.entry("date", "Datum"),
                    Map.entry("date_end", "Datum Ende"),
                    Map.entry("time_start", "Abfahrt"),
                    Map.entry("time_end", "Ankunft"),
                    Map.entry("route", "Fahrtstrecke"),
                    Map.entry("purpose", "Fahrtzweck"),
                    Map.entry("km_start", "km-Stand Anfang"),
                    Map.entry("km_end", "km-Stand Ende"),
                    Map.entry("km_business", "geschäftlich"),
                    Map.entry("km_private", "privat"),
                    Map.entry("km_home_work", "Wohnung/Arbeit")
            )
    );

    private final CSVFormat format;
    private final Map<String, String> columnMapping;

    CSVTripFormat(CSVFormat format, Map<String, String> columnMapping) {
        this.format = format;
        this.columnMapping = columnMapping;
    }

    public CSVFormat getFormat() {
        return format;
    }

    public Map<String, String> getColumnMapping() {
        return columnMapping;
    }
}
