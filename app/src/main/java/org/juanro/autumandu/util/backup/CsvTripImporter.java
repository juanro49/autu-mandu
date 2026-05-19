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

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.entity.Trip;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvTripImporter {
    private static final String TAG = "CsvTripImporter";

    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_TIME_START = "time_start";
    private static final String COLUMN_TIME_END = "time_end";
    private static final String COLUMN_ROUTE = "route";
    private static final String COLUMN_PURPOSE = "purpose";
    private static final String COLUMN_KM_START = "km_start";
    private static final String COLUMN_KM_END = "km_end";
    private static final String COLUMN_KM_BUSINESS = "km_business";
    private static final String COLUMN_KM_PRIVATE = "km_private";
    private static final String COLUMN_KM_HOME_WORK = "km_home_work";

    private final Context context;
    private Map<String, String> columnMapping = new HashMap<>();

    public CsvTripImporter(Context context) {
        this.context = context;
    }

    public CSVTripFormat detectFormat(Uri uri) {
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) return CSVTripFormat.GENERIC;

            // Check for SKODA specific headers or delimiter
            if (header.contains(";") && (header.contains("Datum") || header.contains("km-Stand") || header.contains("Fahrtstrecke"))) {
                return CSVTripFormat.SKODA;
            }

            // Check for GENERIC specific headers
            if (header.contains("Date") && header.contains("Km Start") && header.contains("Route")) {
                return CSVTripFormat.GENERIC;
            }

            // Fallback: check delimiter
            if (header.contains(";")) {
                return CSVTripFormat.SKODA;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error detecting format", e);
        }
        return CSVTripFormat.GENERIC;
    }

    public void setColumnMapping(Map<String, String> mapping) {
        this.columnMapping = mapping;
    }

    public List<Map<String, String>> previewData(Uri uri, int maxRows) {
        List<Map<String, String>> preview = new ArrayList<>();
        CSVTripFormat format = detectFormat(uri);
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
             CSVParser parser = CSVParser.parse(reader, format.getFormat())) {

            int i = 0;
            for (CSVRecord csvRecord : parser) {
                if (i >= maxRows) break;
                preview.add(csvRecord.toMap());
                i++;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error previewing data", e);
        }
        return preview;
    }

    public ImportResult importTrips(Uri uri, long carId) {
        ImportResult result = new ImportResult();
        CSVTripFormat format = detectFormat(uri);

        try (InputStream in = context.getContentResolver().openInputStream(uri);
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
             CSVParser parser = CSVParser.parse(reader, format.getFormat())) {

            AutuManduDatabase db = AutuManduDatabase.getInstance(context);

            for (CSVRecord csvRecord : parser) {
                importRecord(csvRecord, carId, format, db, result);
            }
        } catch (Exception e) {
            result.addError("Global error: " + e.getMessage());
        }

        return result;
    }

    private void importRecord(CSVRecord csvRecord, long carId, CSVTripFormat format, AutuManduDatabase db, ImportResult result) {
        try {
            Trip trip = mapRecordToTrip(csvRecord, carId, format);

            // Check if a similar trip already exists (e.g. same date, times, and car)
            boolean exists = db.getTripDao().getTripsInDateRange(carId, trip.getDate(), trip.getDate()).stream()
                    .anyMatch(t -> t.getTimeStart().equals(trip.getTimeStart()) && t.getKmStart() == trip.getKmStart());

            if (!exists) {
                db.getTripDao().insert(trip);
                result.incrementSuccess();
            } else {
                Log.d(TAG, "Skipping duplicate trip at line " + csvRecord.getRecordNumber());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error importing record at line " + csvRecord.getRecordNumber(), e);
            result.addError("Line " + csvRecord.getRecordNumber() + ": " + e.getMessage());
        }
    }

    private Trip mapRecordToTrip(CSVRecord csvRecord, long carId, CSVTripFormat format) {
        Trip trip = new Trip();
        trip.setCarId(carId);
        trip.setCreatedAt(LocalDateTime.now());
        trip.setUpdatedAt(LocalDateTime.now());

        Map<String, String> mapping = new HashMap<>(format.getColumnMapping());
        if (columnMapping != null && !columnMapping.isEmpty()) {
            mapping.putAll(columnMapping);
        }

        try {
            applyMapping(trip, csvRecord, mapping, format);
        } catch (Exception e) {
            // Log the problematic record for easier debugging
            Log.e(TAG, "Failed to map record: " + csvRecord.toString(), e);
            throw new IllegalArgumentException("Mapping error: " + e.getMessage(), e);
        }

        return trip;
    }

    private void applyMapping(Trip trip, CSVRecord csvRecord, Map<String, String> mapping, CSVTripFormat format) {
        if (mapping.containsKey(COLUMN_DATE)) {
            String val = csvRecord.get(mapping.get(COLUMN_DATE));
            if (format == CSVTripFormat.SKODA) {
                trip.setDate(LocalDate.parse(val, DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            } else {
                trip.setDate(LocalDate.parse(val));
            }
        }
        if (mapping.containsKey(COLUMN_TIME_START)) {
            trip.setTimeStart(LocalTime.parse(csvRecord.get(mapping.get(COLUMN_TIME_START))));
        }
        if (mapping.containsKey(COLUMN_TIME_END)) {
            trip.setTimeEnd(LocalTime.parse(csvRecord.get(mapping.get(COLUMN_TIME_END))));
        }
        if (mapping.containsKey(COLUMN_ROUTE)) {
            trip.setRouteTarget(csvRecord.get(mapping.get(COLUMN_ROUTE)));
        }
        if (mapping.containsKey(COLUMN_PURPOSE)) {
            trip.setPurpose(csvRecord.get(mapping.get(COLUMN_PURPOSE)));
        }
        if (mapping.containsKey(COLUMN_KM_START)) {
            trip.setKmStart(Integer.parseInt(csvRecord.get(mapping.get(COLUMN_KM_START))));
        }
        if (mapping.containsKey(COLUMN_KM_END)) {
            trip.setKmEnd(Integer.parseInt(csvRecord.get(mapping.get(COLUMN_KM_END))));
        }
        if (mapping.containsKey(COLUMN_KM_BUSINESS)) {
            trip.setKmBusiness(Integer.parseInt(csvRecord.get(mapping.get(COLUMN_KM_BUSINESS))));
        }
        if (mapping.containsKey(COLUMN_KM_PRIVATE)) {
            trip.setKmPrivate(Integer.parseInt(csvRecord.get(mapping.get(COLUMN_KM_PRIVATE))));
        }
        if (mapping.containsKey(COLUMN_KM_HOME_WORK)) {
            trip.setKmHomeWork(Integer.parseInt(csvRecord.get(mapping.get(COLUMN_KM_HOME_WORK))));
        }
    }

    public Map<String, String> guessMapping(Uri uri) {
        Map<String, String> guessed = new HashMap<>();
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return guessed;

            String delimiter = headerLine.contains(";") ? ";" : ",";
            String[] headers = headerLine.split(delimiter);

            for (String h : headers) {
                String clean = h.trim().toLowerCase();
                guessColumnMapping(guessed, h, clean);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error guessing mapping", e);
        }
        return guessed;
    }

    private void guessColumnMapping(Map<String, String> guessed, String original, String clean) {
        if (clean.contains(COLUMN_DATE) || clean.contains("datum")) {
            guessDateMapping(guessed, original, clean);
        }
        guessTimeAndRouteMapping(guessed, original, clean);
        if (clean.contains("km") || clean.contains("mileage") || clean.contains("stand")) {
            guessKmColumnMapping(guessed, original, clean);
        }
    }

    private void guessTimeAndRouteMapping(Map<String, String> guessed, String original, String clean) {
        if (clean.contains("start") || clean.contains("abfahrt")) guessed.put(COLUMN_TIME_START, original.trim());
        if (clean.contains("end") || clean.contains("ankunft")) guessed.put(COLUMN_TIME_END, original.trim());
        if (clean.contains(COLUMN_ROUTE) || clean.contains("strecke") || clean.contains("target")) guessed.put(COLUMN_ROUTE, original.trim());
        if (clean.contains(COLUMN_PURPOSE) || clean.contains("zweck")) guessed.put(COLUMN_PURPOSE, original.trim());
    }

    private void guessDateMapping(Map<String, String> guessed, String original, String clean) {
        if (clean.contains("end") || clean.contains("ende")) {
            guessed.put("date_end", original.trim());
        } else {
            guessed.put(COLUMN_DATE, original.trim());
        }
    }

    private void guessKmColumnMapping(Map<String, String> guessed, String original, String clean) {
        if (clean.contains("start") || clean.contains("anfang") || clean.contains("beginning")) {
            guessed.put(COLUMN_KM_START, original.trim());
        } else if (clean.contains("end") || clean.contains("ende")) {
            guessed.put(COLUMN_KM_END, original.trim());
        } else if (clean.contains("business") || clean.contains("geschäft")) {
            guessed.put(COLUMN_KM_BUSINESS, original.trim());
        } else if (clean.contains("private") || clean.contains("privat")) {
            guessed.put(COLUMN_KM_PRIVATE, original.trim());
        } else if (clean.contains("work") || clean.contains("arbeit")) {
            guessed.put(COLUMN_KM_HOME_WORK, original.trim());
        }
    }
}
