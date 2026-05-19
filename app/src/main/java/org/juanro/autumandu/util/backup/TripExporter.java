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

import org.apache.commons.csv.CSVPrinter;
import org.juanro.autumandu.model.entity.Trip;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TripExporter {
    private static final String TAG = "TripExporter";
    private final Context context;

    public TripExporter(Context context) {
        this.context = context;
    }

    public boolean exportToCsv(List<Trip> trips, Uri uri, CSVTripFormat format) {
        try (OutputStream out = context.getContentResolver().openOutputStream(uri);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
             CSVPrinter printer = new CSVPrinter(writer, format.getFormat())) {

            for (Trip trip : trips) {
                printer.printRecord(
                        trip.getDate().toString(),
                        trip.getDateEnd().toString(),
                        trip.getTimeStart().toString(),
                        trip.getTimeEnd().toString(),
                        trip.getRouteTarget(),
                        trip.getPurpose(),
                        trip.getKmStart(),
                        trip.getKmEnd(),
                        trip.getKmBusiness(),
                        trip.getKmPrivate(),
                        trip.getKmHomeWork()
                );
            }
            printer.flush();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error exporting trips to CSV", e);
            return false;
        }
    }
}
