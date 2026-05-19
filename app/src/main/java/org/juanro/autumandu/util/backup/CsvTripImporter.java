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
            for (CSVRecord record : parser) {
                if (i >= maxRows) break;
                preview.add(record.toMap());
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

            for (CSVRecord record : parser) {
                try {
                    Trip trip = mapRecordToTrip(record, carId, format);

                    // Check if a similar trip already exists (e.g. same date, times, and car)
                    boolean exists = db.getTripDao().getTripsInDateRange(carId, trip.getDate(), trip.getDate()).stream()
                            .anyMatch(t -> t.getTimeStart().equals(trip.getTimeStart()) && t.getKmStart() == trip.getKmStart());

                    if (!exists) {
                        db.getTripDao().insert(trip);
                        result.incrementSuccess();
                    } else {
                        Log.d(TAG, "Skipping duplicate trip at line " + record.getRecordNumber());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error importing record at line " + record.getRecordNumber(), e);
                    result.addError("Line " + record.getRecordNumber() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            result.addError("Global error: " + e.getMessage());
        }

        return result;
    }

    private Trip mapRecordToTrip(CSVRecord record, long carId, CSVTripFormat format) {
        Trip trip = new Trip();
        trip.setCarId(carId);
        trip.setCreatedAt(LocalDateTime.now());
        trip.setUpdatedAt(LocalDateTime.now());

        Map<String, String> mapping = new HashMap<>(format.getColumnMapping());
        if (columnMapping != null && !columnMapping.isEmpty()) {
            mapping.putAll(columnMapping);
        }

        try {
            if (mapping.containsKey("date")) {
                String val = record.get(mapping.get("date"));
                if (format == CSVTripFormat.SKODA) {
                    trip.setDate(LocalDate.parse(val, DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                } else {
                    trip.setDate(LocalDate.parse(val));
                }
            }
            if (mapping.containsKey("time_start")) {
                trip.setTimeStart(LocalTime.parse(record.get(mapping.get("time_start"))));
            }
            if (mapping.containsKey("time_end")) {
                trip.setTimeEnd(LocalTime.parse(record.get(mapping.get("time_end"))));
            }
            if (mapping.containsKey("route")) {
                trip.setRouteTarget(record.get(mapping.get("route")));
            }
            if (mapping.containsKey("purpose")) {
                trip.setPurpose(record.get(mapping.get("purpose")));
            }
            if (mapping.containsKey("km_start")) {
                trip.setKmStart(Integer.parseInt(record.get(mapping.get("km_start"))));
            }
            if (mapping.containsKey("km_end")) {
                trip.setKmEnd(Integer.parseInt(record.get(mapping.get("km_end"))));
            }
            if (mapping.containsKey("km_business")) {
                trip.setKmBusiness(Integer.parseInt(record.get(mapping.get("km_business"))));
            }
            if (mapping.containsKey("km_private")) {
                trip.setKmPrivate(Integer.parseInt(record.get(mapping.get("km_private"))));
            }
            if (mapping.containsKey("km_home_work")) {
                trip.setKmHomeWork(Integer.parseInt(record.get(mapping.get("km_home_work"))));
            }
        } catch (Exception e) {
            // Log the problematic record for easier debugging
            Log.e(TAG, "Failed to map record: " + record.toString(), e);
            throw new RuntimeException("Mapping error: " + e.getMessage());
        }

        return trip;
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
                if (clean.contains("date") || clean.contains("datum")) {
                    if (clean.contains("end") || clean.contains("ende")) {
                        guessed.put("date_end", h.trim());
                    } else {
                        guessed.put("date", h.trim());
                    }
                }
                if (clean.contains("start") || clean.contains("abfahrt")) guessed.put("time_start", h.trim());
                if (clean.contains("end") || clean.contains("ankunft")) guessed.put("time_end", h.trim());
                if (clean.contains("route") || clean.contains("strecke") || clean.contains("target")) guessed.put("route", h.trim());
                if (clean.contains("purpose") || clean.contains("zweck")) guessed.put("purpose", h.trim());
                if (clean.contains("km") || clean.contains("mileage") || clean.contains("stand")) {
                    if (clean.contains("start") || clean.contains("anfang") || clean.contains("beginning")) {
                        guessed.put("km_start", h.trim());
                    } else if (clean.contains("end") || clean.contains("ende")) {
                        guessed.put("km_end", h.trim());
                    } else if (clean.contains("business") || clean.contains("geschäft")) {
                        guessed.put("km_business", h.trim());
                    } else if (clean.contains("private") || clean.contains("privat")) {
                        guessed.put("km_private", h.trim());
                    } else if (clean.contains("work") || clean.contains("arbeit")) {
                        guessed.put("km_home_work", h.trim());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error guessing mapping", e);
        }
        return guessed;
    }
}
