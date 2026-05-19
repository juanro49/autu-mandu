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

package org.juanro.autumandu.model.dto;

import java.util.List;
import org.juanro.autumandu.model.entity.Trip;

public record TripStatistics(
        int totalTrips,
        int totalDistance,
        int businessKm,
        int privateKm,
        int homeWorkKm,
        double totalCosts
) {
    public static TripStatistics fromTrips(List<Trip> trips) {
        if (trips == null || trips.isEmpty()) {
            return new TripStatistics(0, 0, 0, 0, 0, 0.0);
        }

        return new TripStatistics(
                trips.size(),
                trips.stream().mapToInt(Trip::getTotalDistance).sum(),
                trips.stream().mapToInt(Trip::getKmBusiness).sum(),
                trips.stream().mapToInt(Trip::getKmPrivate).sum(),
                trips.stream().mapToInt(Trip::getKmHomeWork).sum(),
                trips.stream().mapToDouble(Trip::getTotalCost).sum()
        );
    }
}
