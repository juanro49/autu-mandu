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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.juanro.autumandu.util.Calculator;

public class BalancedRefueling {
    private static final float MAX_RELATIVE_CONSUMPTION_DEVIATION = 0.2f;

    private long id;
    private Date date;
    private int mileage;
    private float volume;
    private float price;
    private boolean partial;
    private String note;
    private long fuelTypeId;
    private long stationId;
    private long carId;

    private String fuelTypeName;
    private String fuelTypeCategory;
    private String stationName;
    private String carName;
    private int carColor;
    private int carInitialMileage;
    private Date carSuspendedSince;
    private double carBuyingPrice;
    private int carNumTires;

    private boolean guessed = false;
    private boolean valid = true;

    private Float consumption;
    private Integer mileageDifference;

    public BalancedRefueling() {
    }

    public BalancedRefueling(RefuelingWithDetails refueling) {
        this.id = refueling.id();
        this.date = refueling.date();
        this.mileage = refueling.mileage();
        this.volume = refueling.volume();
        this.price = refueling.price();
        this.partial = refueling.partial();
        this.note = refueling.note();
        this.fuelTypeId = refueling.fuelTypeId();
        this.stationId = refueling.stationId();
        this.carId = refueling.carId();

        this.fuelTypeName = refueling.fuelTypeName();
        this.fuelTypeCategory = refueling.fuelTypeCategory();
        this.stationName = refueling.stationName();

        this.carName = refueling.carName();
        this.carColor = refueling.carColor();
        this.carInitialMileage = refueling.carInitialMileage();
        this.carSuspendedSince = refueling.carSuspendedSince();
        this.carBuyingPrice = refueling.carBuyingPrice();
        this.carNumTires = refueling.carNumTires();
    }

    private BalancedRefueling(long id, Date date, int mileage, float volume, float price,
                              boolean partial, String note, long fuelTypeId, long stationId, long carId,
                              String fuelTypeName, String fuelTypeCategory, String stationName, String carName,
                              int carColor, int carInitialMileage, Date carSuspendedSince, double carBuyingPrice, int carNumTires) {
        this.id = id;
        this.date = date;
        this.mileage = mileage;
        this.volume = volume;
        this.price = price;
        this.partial = partial;
        this.note = note;
        this.fuelTypeId = fuelTypeId;
        this.stationId = stationId;
        this.carId = carId;

        this.fuelTypeName = fuelTypeName;
        this.fuelTypeCategory = fuelTypeCategory;
        this.stationName = stationName;

        this.carName = carName;
        this.carColor = carColor;
        this.carInitialMileage = carInitialMileage;
        this.carSuspendedSince = carSuspendedSince;
        this.carBuyingPrice = carBuyingPrice;
        this.carNumTires = carNumTires;
    }

    /**
     * Balances a list of refuelings by guessing missing data and calculating consumption.
     *
     * @param input List of refuelings (usually ordered newest first)
     * @param guessMissingData Whether to guess missing data (interpolation)
     * @param orderDescending Whether to return the result ordered newest first
     * @return Balanced list
     */
    public static List<BalancedRefueling> balance(List<RefuelingWithDetails> input,
                                                 boolean guessMissingData,
                                                 boolean orderDescending) {
        List<BalancedRefueling> refuelings = new ArrayList<>();
        // In the input, the order might be newest first. We need oldest first for processing.
        List<RefuelingWithDetails> sortedInput = new ArrayList<>(input);
        Collections.sort(sortedInput, (o1, o2) -> o1.date().compareTo(o2.date()));

        for (RefuelingWithDetails rwd : sortedInput) {
            refuelings.add(new BalancedRefueling(rwd));
        }

        if (refuelings.isEmpty()) {
            return refuelings;
        }

        // Validate mileages
        boolean allValid = areRefuelingsValid(refuelings);

        // Guess missing refuelings if requested and all current refuelings have valid mileages.
        if (guessMissingData && allValid) {
            refuelings = calculateBalancedRefuelings(refuelings);
        }

        // ALWAYS calculate consumption and mileage difference.
        // Mileage difference is calculated for ALL refuelings (distance since previous record).
        // Consumption is only calculated for full refuelings (accumulating partials).
        for (int i = 0; i < refuelings.size(); i++) {
            BalancedRefueling current = refuelings.get(i);

            // Calculate mileage difference since the immediately PREVIOUS record (for UI display)
            if (i > 0) {
                int diff = current.getMileage() - refuelings.get(i - 1).getMileage();
                current.setMileageDifference(diff);
            }

            if (!current.isPartial()) {
                float volumeSinceLastFull = current.getVolume();
                // Find previous full refueling (ignoring partials in between for consumption calculation)
                for (int j = i - 1; j >= 0; j--) {
                    BalancedRefueling older = refuelings.get(j);
                    if (!older.isPartial()) {
                        int diffSinceLastFull = current.getMileage() - older.getMileage();
                        if (diffSinceLastFull > 0) {
                            current.setConsumption((volumeSinceLastFull / diffSinceLastFull) * 100);
                        }
                        break;
                    }
                    volumeSinceLastFull += older.getVolume();
                }
            }
        }

        if (orderDescending) {
            Collections.reverse(refuelings);
        }

        return refuelings;
    }

    private static List<BalancedRefueling> calculateBalancedRefuelings(List<BalancedRefueling> refuelings) {
        int avgDistance = getBalancedAverageDistanceOfFullRefuelings(refuelings);
        float avgVolume = getBalancedAverageVolumeOfFullRefuelings(refuelings);
        if (avgDistance <= 0) return refuelings;
        float avgConsumption = avgVolume / avgDistance;
        float avgPricePerUnit = getAveragePricePerUnit(refuelings);

        int distance = 0;
        float volume = 0;
        int lastFullRefueling = -1;
        long nextId = Long.MAX_VALUE / 2;

        for (int i = 0; i < refuelings.size(); i++) {
            BalancedRefueling refueling = refuelings.get(i);
            if (lastFullRefueling < 0) {
                if (!refueling.isPartial()) {
                    lastFullRefueling = i;
                }
                continue;
            }

            distance += refueling.getMileage() - refuelings.get(i - 1).getMileage();
            volume += refuelings.get(i).getVolume();
            if (refueling.isPartial()) {
                continue;
            }

            double consumption = volume / distance;
            if (consumption / avgConsumption < (1 - MAX_RELATIVE_CONSUMPTION_DEVIATION)) {
                float missingVolume = avgConsumption * distance - volume;
                int possibleDistance = avgDistance;

                for (int pI = lastFullRefueling + 1; pI < i && missingVolume > 0; pI++) {
                    int pDistance = refuelings.get(pI).getMileage() - refuelings.get(pI - 1).getMileage();
                    if (pDistance <= possibleDistance) {
                        possibleDistance -= pDistance;
                        possibleDistance += (int) (refuelings.get(pI).getVolume() / avgConsumption);
                    } else {
                        float newVolume = Math.min(avgDistance * avgConsumption, missingVolume);
                        float volumeSinceLastFullRefueling = newVolume;
                        for (int pI2 = lastFullRefueling + 1; pI2 < pI; pI2++) {
                            volumeSinceLastFullRefueling += refuelings.get(pI2).getVolume();
                        }
                        int newMileage = refuelings.get(lastFullRefueling).getMileage()
                                + (int) (volumeSinceLastFullRefueling / avgConsumption);

                        long pTimeDiff = refuelings.get(pI).getDate().getTime() - refuelings.get(pI - 1).getDate().getTime();
                        Date newDate = new Date(refuelings.get(pI - 1).getDate().getTime()
                                + (long) (pTimeDiff / (double)pDistance * (newVolume / avgConsumption)));

                        float newPrice = newVolume * avgPricePerUnit;

                        BalancedRefueling guess = new BalancedRefueling(nextId++, newDate, newMileage, newVolume, newPrice,
                                false, "", refueling.getFuelTypeId(), refueling.getStationId(), refueling.getCarId(),
                                refueling.getFuelTypeName(), refueling.getFuelTypeCategory(), refueling.getStationName(),
                                refueling.getCarName(), refueling.getCarColor(), refueling.getCarInitialMileage(),
                                refueling.getCarSuspendedSince(), refueling.getCarBuyingPrice(), refueling.getCarNumTires());
                        guess.setGuessed(true);
                        refuelings.add(pI, guess);

                        missingVolume -= newVolume;
                        possibleDistance = avgDistance;
                        lastFullRefueling = pI;
                        i++;
                    }
                }

                while (missingVolume > 0) {
                    float newVolume = Math.min(avgDistance * avgConsumption, missingVolume);
                    float volumeSinceLastFullRefueling = newVolume;
                    for (int pI2 = lastFullRefueling + 1; pI2 < i; pI2++) {
                        volumeSinceLastFullRefueling += refuelings.get(pI2).getVolume();
                    }

                    boolean partial = false;
                    int newMileage = refuelings.get(lastFullRefueling).getMileage()
                            + (int) (volumeSinceLastFullRefueling / avgConsumption);

                    if (newMileage < refuelings.get(i - 1).getMileage()) {
                        newMileage = refuelings.get(i - 1).getMileage()
                                + possibleDistance
                                + (int) (refuelings.get(i - 1).getVolume() / avgConsumption);
                        partial = true;
                    }

                    int cDistance = refueling.getMileage() - refuelings.get(i - 1).getMileage();
                    long cTimeDiff = refueling.getDate().getTime() - refuelings.get(i - 1).getDate().getTime();
                    Date newDate = new Date(refuelings.get(i - 1).getDate().getTime()
                            + (long) (cTimeDiff / (double)cDistance * (newVolume / avgConsumption)));

                    float newPrice = newVolume * avgPricePerUnit;

                    BalancedRefueling guess = new BalancedRefueling(nextId++, newDate, newMileage, newVolume, newPrice,
                            partial, "", refueling.getFuelTypeId(), refueling.getStationId(), refueling.getCarId(),
                            refuelings.get(i-1).getFuelTypeName(), refuelings.get(i-1).getFuelTypeCategory(), refuelings.get(i-1).getStationName(),
                            refuelings.get(i-1).getCarName(), refuelings.get(i-1).getCarColor(), refuelings.get(i-1).getCarInitialMileage(),
                            refuelings.get(i-1).getCarSuspendedSince(), refuelings.get(i-1).getCarBuyingPrice(), refuelings.get(i-1).getCarNumTires());
                    guess.setGuessed(true);
                    refuelings.add(i, guess);

                    missingVolume -= newVolume;
                    possibleDistance = avgDistance;
                    lastFullRefueling = i;
                    i++;
                }
            }

            distance = 0;
            volume = 0;
            lastFullRefueling = i;
        }

        return refuelings;
    }

    private static int getBalancedAverageDistanceOfFullRefuelings(List<BalancedRefueling> refuelings) {
        Vector<Integer> allDistances = new Vector<>();
        for (int i = 1; i < refuelings.size(); i++) {
            if (!refuelings.get(i).isPartial() && !refuelings.get(i - 1).isPartial()) {
                allDistances.add(refuelings.get(i).getMileage() - refuelings.get(i - 1).getMileage());
            }
        }
        if (allDistances.isEmpty()) {
            for (int i = 1; i < refuelings.size(); i++) {
                allDistances.add(refuelings.get(i).getMileage() - refuelings.get(i - 1).getMileage());
            }
        }
        if (allDistances.isEmpty()) return 0;

        Collections.sort(allDistances);
        int removeCount = Math.round(allDistances.size() * 0.2f);
        for (int i = 0; i < removeCount; i++) {
            allDistances.remove(0);
            allDistances.remove(allDistances.size() - 1);
        }

        int avgDistance = (int) Calculator.avg(allDistances.toArray(new Integer[0]));
        boolean updated;
        do {
            updated = false;
            for (int i = allDistances.size() - 1; i >= 0; i--) {
                float relativeDistance = (float) allDistances.get(i) / (float) avgDistance;
                if (relativeDistance < (1 - MAX_RELATIVE_CONSUMPTION_DEVIATION)
                        || relativeDistance > (1 + MAX_RELATIVE_CONSUMPTION_DEVIATION)) {
                    allDistances.remove(i);
                    avgDistance = (int) Calculator.avg(allDistances.toArray(new Integer[0]));
                    updated = true;
                }
            }
        } while (updated && !allDistances.isEmpty());

        return avgDistance;
    }

    private static float getBalancedAverageVolumeOfFullRefuelings(List<BalancedRefueling> refuelings) {
        Vector<Float> allVolumes = new Vector<>();
        for (int i = 1; i < refuelings.size(); i++) {
            if (!refuelings.get(i).isPartial() && !refuelings.get(i - 1).isPartial()) {
                allVolumes.add(refuelings.get(i).getVolume());
            }
        }
        if (allVolumes.isEmpty()) {
            for (int i = 1; i < refuelings.size(); i++) {
                allVolumes.add(refuelings.get(i).getVolume());
            }
        }
        if (allVolumes.isEmpty()) return 0;

        Collections.sort(allVolumes);
        int removeCount = Math.round(allVolumes.size() * 0.2f);
        for (int i = 0; i < removeCount; i++) {
            allVolumes.remove(0);
            allVolumes.remove(allVolumes.size() - 1);
        }

        float avgVolume = (float) Calculator.avg(allVolumes.toArray(new Float[0]));
        boolean updated;
        do {
            updated = false;
            for (int i = allVolumes.size() - 1; i >= 0; i--) {
                float relativeVolume = allVolumes.get(i) / avgVolume;
                if (relativeVolume < (1 - MAX_RELATIVE_CONSUMPTION_DEVIATION)
                        || relativeVolume > (1 + MAX_RELATIVE_CONSUMPTION_DEVIATION)) {
                    allVolumes.remove(i);
                    avgVolume = (float) Calculator.avg(allVolumes.toArray(new Float[0]));
                    updated = true;
                }
            }
        } while (updated && !allVolumes.isEmpty());

        return avgVolume;
    }

    private static float getAveragePricePerUnit(List<BalancedRefueling> refuelings) {
        if (refuelings.isEmpty()) return 0;
        Float[] allPrices = new Float[refuelings.size()];
        for (int i = 0; i < allPrices.length; i++) {
            allPrices[i] = refuelings.get(i).getPrice() / refuelings.get(i).getVolume();
        }
        return (float) Calculator.avg(allPrices);
    }

    private static boolean areRefuelingsValid(List<BalancedRefueling> refuelings) {
        boolean valid = true;
        for (int i = 1; i < refuelings.size(); i++) {
            BalancedRefueling previousRefueling = refuelings.get(i - 1);
            BalancedRefueling refueling = refuelings.get(i);
            if (refueling.getMileage() <= previousRefueling.getMileage()) {
                refueling.setValid(false);
                valid = false;
            }
        }
        return valid;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }
    public int getMileage() { return mileage; }
    public void setMileage(int mileage) { this.mileage = mileage; }
    public float getVolume() { return volume; }
    public void setVolume(float volume) { this.volume = volume; }
    public float getPrice() { return price; }
    public void setPrice(float price) { this.price = price; }
    public boolean isPartial() { return partial; }
    public void setPartial(boolean partial) { this.partial = partial; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public long getFuelTypeId() { return fuelTypeId; }
    public void setFuelTypeId(long fuelTypeId) { this.fuelTypeId = fuelTypeId; }
    public long getStationId() { return stationId; }
    public void setStationId(long stationId) { this.stationId = stationId; }
    public long getCarId() { return carId; }
    public void setCarId(long carId) { this.carId = carId; }
    public String getFuelTypeName() { return fuelTypeName; }
    public void setFuelTypeName(String fuelTypeName) { this.fuelTypeName = fuelTypeName; }
    public String getFuelTypeCategory() { return fuelTypeCategory; }
    public void setFuelTypeCategory(String fuelTypeCategory) { this.fuelTypeCategory = fuelTypeCategory; }
    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }
    public String getCarName() { return carName; }
    public void setCarName(String carName) { this.carName = carName; }
    public int getCarColor() { return carColor; }
    public void setCarColor(int carColor) { this.carColor = carColor; }
    public int getCarInitialMileage() { return carInitialMileage; }
    public void setCarInitialMileage(int carInitialMileage) { this.carInitialMileage = carInitialMileage; }
    public Date getCarSuspendedSince() { return carSuspendedSince; }
    public void setCarSuspendedSince(Date carSuspendedSince) { this.carSuspendedSince = carSuspendedSince; }
    public double getCarBuyingPrice() { return carBuyingPrice; }
    public void setCarBuyingPrice(double carBuyingPrice) { this.carBuyingPrice = carBuyingPrice; }
    public int getCarNumTires() { return carNumTires; }
    public void setCarNumTires(int carNumTires) { this.carNumTires = carNumTires; }
    public boolean isGuessed() { return guessed; }
    public void setGuessed(boolean guessed) { this.guessed = guessed; }
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public Float getConsumption() { return consumption; }
    public void setConsumption(Float consumption) { this.consumption = consumption; }
    public Integer getMileageDifference() { return mileageDifference; }
    public void setMileageDifference(Integer mileageDifference) { this.mileageDifference = mileageDifference; }
}
