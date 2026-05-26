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
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.juanro.autumandu.FuelConsumption;
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

    private final String fuelTypeName;
    private final String fuelTypeCategory;
    private final String stationName;
    private final String carName;
    private final int carColor;
    private final int carInitialMileage;
    private final Date carSuspendedSince;
    private final double carBuyingPrice;
    private final int carNumTires;

    private boolean guessed = false;
    private boolean valid = true;

    private Float consumption;
    private Integer mileageDifference;

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

    private BalancedRefueling(RefuelingData data, Metadata metadata) {
        this.id = data.id();
        this.date = data.date();
        this.mileage = data.mileage();
        this.volume = data.volume();
        this.price = data.price();
        this.partial = data.partial();
        this.note = data.note();

        this.fuelTypeId = metadata.fuelTypeId();
        this.stationId = metadata.stationId();
        this.carId = metadata.carId();
        this.fuelTypeName = metadata.fuelTypeName();
        this.fuelTypeCategory = metadata.fuelTypeCategory();
        this.stationName = metadata.stationName();

        CarInfo carInfo = metadata.carInfo();
        this.carName = carInfo.name();
        this.carColor = carInfo.color();
        this.carInitialMileage = carInfo.initialMileage();
        this.carSuspendedSince = carInfo.suspendedSince();
        this.carBuyingPrice = carInfo.buyingPrice();
        this.carNumTires = carInfo.numTires();
    }

    private record RefuelingData(long id, Date date, int mileage, float volume, float price, boolean partial, String note) {}
    private record Metadata(long fuelTypeId, long stationId, long carId, String fuelTypeName, String fuelTypeCategory, String stationName, CarInfo carInfo) {}
    private record CarInfo(String name, int color, int initialMileage, Date suspendedSince, double buyingPrice, int numTires) {}

    /**
     * Balances a list of refuelings by guessing missing data and calculating consumption.
     *
     * @param input List of refuelings (usually ordered newest first)
     * @param consumptionType The unit type for fuel consumption calculation
     * @param guessMissingData Whether to guess missing data (interpolation)
     * @param orderDescending Whether to return the result ordered newest first
     * @return Balanced list
     */
    public static List<BalancedRefueling> balance(List<RefuelingWithDetails> input,
                                                 FuelConsumption.Type consumptionType,
                                                 boolean guessMissingData,
                                                 boolean orderDescending) {
        List<BalancedRefueling> refuelings = new ArrayList<>();
        List<RefuelingWithDetails> sortedInput = new ArrayList<>(input);
        sortedInput.sort(Comparator.comparing(RefuelingWithDetails::date));

        for (RefuelingWithDetails rwd : sortedInput) {
            refuelings.add(new BalancedRefueling(rwd));
        }

        if (refuelings.isEmpty()) {
            return refuelings;
        }

        boolean allValid = areRefuelingsValid(refuelings);

        if (guessMissingData && allValid) {
            calculateBalancedRefuelings(refuelings);
        }

        calculateConsumptions(refuelings, consumptionType);

        if (orderDescending) {
            Collections.reverse(refuelings);
        }

        return refuelings;
    }

    private static void calculateConsumptions(List<BalancedRefueling> refuelings, FuelConsumption.Type consumptionType) {
        for (int i = 0; i < refuelings.size(); i++) {
            BalancedRefueling current = refuelings.get(i);

            if (i > 0) {
                int diff = current.getMileage() - refuelings.get(i - 1).getMileage();
                current.setMileageDifference(diff);
            }

            if (!current.isPartial()) {
                current.setConsumption(calculateConsumptionForFullRefueling(refuelings, i, consumptionType));
            }
        }
    }

    private static Float calculateConsumptionForFullRefueling(List<BalancedRefueling> refuelings, int index, FuelConsumption.Type consumptionType) {
        BalancedRefueling current = refuelings.get(index);
        float volumeSinceLastFull = current.getVolume();
        for (int j = index - 1; j >= 0; j--) {
            BalancedRefueling older = refuelings.get(j);
            if (!older.isPartial()) {
                int diffSinceLastFull = current.getMileage() - older.getMileage();
                if (diffSinceLastFull > 0) {
                    return FuelConsumption.computeFuelConsumption(consumptionType, volumeSinceLastFull, diffSinceLastFull);
                }
                break;
            }
            volumeSinceLastFull += older.getVolume();
        }
        return null;
    }

    private static void calculateBalancedRefuelings(List<BalancedRefueling> refuelings) {
        int avgDistance = getBalancedAverageDistanceOfFullRefuelings(refuelings);
        float avgVolume = getBalancedAverageVolumeOfFullRefuelings(refuelings);
        if (avgDistance <= 0) return;
        float avgConsumption = avgVolume / avgDistance;
        float avgPricePerUnit = getAveragePricePerUnit(refuelings);

        RefuelingBalanceContext context = new RefuelingBalanceContext(avgDistance, avgConsumption, avgPricePerUnit);

        int i = 0;
        while (i < refuelings.size()) {
            BalancedRefueling refueling = refuelings.get(i);
            if (context.lastFullRefueling >= 0) {
                context.distance += refueling.getMileage() - refuelings.get(i - 1).getMileage();
                context.volume += refueling.getVolume();
                if (!refueling.isPartial()) {
                    i = balanceFullRefuelingInterval(refuelings, i, context);
                    context.resetInterval(i);
                }
            } else if (!refueling.isPartial()) {
                context.lastFullRefueling = i;
            }
            i++;
        }
    }

    private static int balanceFullRefuelingInterval(List<BalancedRefueling> refuelings, int currentIndex, RefuelingBalanceContext ctx) {
        int i = currentIndex;
        double consumption = ctx.volume / ctx.distance;
        if (consumption / ctx.avgConsumption < (1 - MAX_RELATIVE_CONSUMPTION_DEVIATION)) {
            float missingVolume = ctx.avgConsumption * ctx.distance - ctx.volume;
            i = fillInternalMissingVolume(refuelings, i, ctx, missingVolume);

            float currentVolume = calculateVolumeInInterval(refuelings, ctx.lastFullRefueling, i);
            int currentDistance = refuelings.get(i).getMileage() - refuelings.get(ctx.lastFullRefueling).getMileage();
            missingVolume = ctx.avgConsumption * currentDistance - currentVolume;

            i = fillTrailingMissingVolume(refuelings, i, ctx, missingVolume);
        }
        return i;
    }

    private static float calculateVolumeInInterval(List<BalancedRefueling> refuelings, int start, int end) {
        float vol = 0;
        for (int i = start + 1; i <= end; i++) {
            vol += refuelings.get(i).getVolume();
        }
        return vol;
    }

    private static int fillInternalMissingVolume(List<BalancedRefueling> refuelings, int currentIndex, RefuelingBalanceContext ctx, float missingVolume) {
        int i = currentIndex;
        float remainingMissing = missingVolume;
        int pDistanceLimit = ctx.avgDistance;

        int pI = ctx.lastFullRefueling + 1;
        while (pI < i && remainingMissing > 0) {
            int pDistance = refuelings.get(pI).getMileage() - refuelings.get(pI - 1).getMileage();
            if (pDistance <= pDistanceLimit) {
                pDistanceLimit -= pDistance;
                pDistanceLimit += (int) (refuelings.get(pI).getVolume() / ctx.avgConsumption);
            } else {
                float newVolume = Math.min(ctx.avgDistance * ctx.avgConsumption, remainingMissing);
                BalancedRefueling guess = createGuessedRefueling(refuelings, pI, ctx, newVolume);
                refuelings.add(pI, guess);

                remainingMissing -= newVolume;
                pDistanceLimit = ctx.avgDistance;
                ctx.lastFullRefueling = pI;
                i++;
                pI++;
            }
            pI++;
        }
        return i;
    }

    private static int fillTrailingMissingVolume(List<BalancedRefueling> refuelings, int currentIndex, RefuelingBalanceContext ctx, float missingVolume) {
        int i = currentIndex;
        float remainingMissing = missingVolume;
        int pDist = ctx.avgDistance;

        while (remainingMissing > 0) {
            float newVolume = Math.min(ctx.avgDistance * ctx.avgConsumption, remainingMissing);
            float volumeSinceLastFull = calculateVolumeInInterval(refuelings, ctx.lastFullRefueling, i-1) + newVolume;

            int newMileage = refuelings.get(ctx.lastFullRefueling).getMileage() + (int) (volumeSinceLastFull / ctx.avgConsumption);
            boolean partial = false;
            if (newMileage < refuelings.get(i - 1).getMileage()) {
                newMileage = refuelings.get(i - 1).getMileage() + pDist + (int) (refuelings.get(i - 1).getVolume() / ctx.avgConsumption);
                partial = true;
            }

            BalancedRefueling guess = createGuessedRefuelingAtEnd(refuelings, i, ctx, newVolume, newMileage, partial);
            refuelings.add(i, guess);

            remainingMissing -= newVolume;
            ctx.lastFullRefueling = i;
            i++;
        }
        return i;
    }

    private static BalancedRefueling createGuessedRefueling(List<BalancedRefueling> refuelings, int index, RefuelingBalanceContext ctx, float volume) {
        BalancedRefueling refueling = refuelings.get(index);
        float volumeSinceLastFull = volume + calculateVolumeInInterval(refuelings, ctx.lastFullRefueling, index - 1);
        int newMileage = ctx.lastFullRefuelingMileage(refuelings, ctx.lastFullRefueling) + (int) (volumeSinceLastFull / ctx.avgConsumption);

        return createGuessedRefuelingInternal(refuelings, index, ctx, volume, newMileage, false, refueling);
    }

    private static BalancedRefueling createGuessedRefuelingAtEnd(List<BalancedRefueling> refuelings, int index, RefuelingBalanceContext ctx, float volume, int mileage, boolean partial) {
        BalancedRefueling refueling = refuelings.get(index - 1);
        return createGuessedRefuelingInternal(refuelings, index, ctx, volume, mileage, partial, refueling);
    }

    private static BalancedRefueling createGuessedRefuelingInternal(List<BalancedRefueling> refuelings, int index, RefuelingBalanceContext ctx, float volume, int mileage, boolean partial, BalancedRefueling template) {
        int distance = refuelings.get(index).getMileage() - refuelings.get(index - 1).getMileage();
        long timeDiff = refuelings.get(index).getDate().getTime() - refuelings.get(index - 1).getDate().getTime();
        Date newDate = new Date(refuelings.get(index - 1).getDate().getTime() + (long) (timeDiff / (double) distance * (volume / ctx.avgConsumption)));

        CarInfo carInfo = new CarInfo(template.getCarName(), template.getCarColor(), template.getCarInitialMileage(),
                template.getCarSuspendedSince(), template.getCarBuyingPrice(), template.getCarNumTires());

        RefuelingData refuelingData = new RefuelingData(ctx.nextId++, newDate, mileage, volume, volume * ctx.avgPricePerUnit, partial, "");
        Metadata metadata = new Metadata(template.getFuelTypeId(), template.getStationId(), template.getCarId(),
                template.getFuelTypeName(), template.getFuelTypeCategory(), template.getStationName(), carInfo);

        BalancedRefueling guess = new BalancedRefueling(refuelingData, metadata);
        guess.setGuessed(true);
        return guess;
    }

    private static class RefuelingBalanceContext {
        final int avgDistance;
        final float avgConsumption;
        final float avgPricePerUnit;
        int distance = 0;
        float volume = 0;
        int lastFullRefueling = -1;
        long nextId = Long.MAX_VALUE / 2;

        RefuelingBalanceContext(int avgDistance, float avgConsumption, float avgPricePerUnit) {
            this.avgDistance = avgDistance;
            this.avgConsumption = avgConsumption;
            this.avgPricePerUnit = avgPricePerUnit;
        }

        void resetInterval(int lastFull) {
            this.distance = 0;
            this.volume = 0;
            this.lastFullRefueling = lastFull;
        }

        int lastFullRefuelingMileage(List<BalancedRefueling> refuelings, int lastFull) {
            return refuelings.get(lastFull).getMileage();
        }
    }

    private static int getBalancedAverageDistanceOfFullRefuelings(List<BalancedRefueling> refuelings) {
        List<Integer> allDistances = extractDistances(refuelings);
        if (allDistances.isEmpty()) return 0;

        List<Integer> filteredDistances = removeOutliers(allDistances);

        int avgDistance = (int) Calculator.avg(filteredDistances.toArray(new Integer[0]));
        return refineAverageDistance(filteredDistances, avgDistance);
    }

    private static List<Integer> extractDistances(List<BalancedRefueling> refuelings) {
        List<Integer> allDistances = new ArrayList<>();
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
        return allDistances;
    }

    private static <T extends Comparable<T>> List<T> removeOutliers(List<T> list) {
        List<T> sortedList = new ArrayList<>(list);
        Collections.sort(sortedList);
        int removeCount = Math.round(sortedList.size() * 0.2f);
        if (removeCount * 2 >= sortedList.size()) {
            return sortedList;
        }
        return new ArrayList<>(sortedList.subList(removeCount, sortedList.size() - removeCount));
    }

    private static int refineAverageDistance(List<Integer> allDistances, int initialAvg) {
        int avgDistance = initialAvg;
        boolean updated;
        do {
            updated = false;
            for (int i = allDistances.size() - 1; i >= 0; i--) {
                float relativeDistance = (float) allDistances.get(i) / avgDistance;
                if (relativeDistance < (1 - MAX_RELATIVE_CONSUMPTION_DEVIATION)
                        || relativeDistance > (1 + MAX_RELATIVE_CONSUMPTION_DEVIATION)) {
                    allDistances.remove(i);
                    if (!allDistances.isEmpty()) {
                        avgDistance = (int) Calculator.avg(allDistances.toArray(new Integer[0]));
                    }
                    updated = true;
                }
            }
        } while (updated && !allDistances.isEmpty());
        return avgDistance;
    }

    private static float getBalancedAverageVolumeOfFullRefuelings(List<BalancedRefueling> refuelings) {
        List<Float> allVolumes = extractVolumes(refuelings);
        if (allVolumes.isEmpty()) return 0;

        List<Float> filteredVolumes = removeOutliers(allVolumes);

        float avgVolume = (float) Calculator.avg(filteredVolumes.toArray(new Float[0]));
        return refineAverageVolume(filteredVolumes, avgVolume);
    }

    private static List<Float> extractVolumes(List<BalancedRefueling> refuelings) {
        List<Float> allVolumes = new ArrayList<>();
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
        return allVolumes;
    }

    private static float refineAverageVolume(List<Float> allVolumes, float initialAvg) {
        float avgVolume = initialAvg;
        boolean updated;
        do {
            updated = false;
            for (int i = allVolumes.size() - 1; i >= 0; i--) {
                float relativeVolume = allVolumes.get(i) / avgVolume;
                if (relativeVolume < (1 - MAX_RELATIVE_CONSUMPTION_DEVIATION)
                        || relativeVolume > (1 + MAX_RELATIVE_CONSUMPTION_DEVIATION)) {
                    allVolumes.remove(i);
                    if (!allVolumes.isEmpty()) {
                        avgVolume = (float) Calculator.avg(allVolumes.toArray(new Float[0]));
                    }
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
    public String getFuelTypeCategory() { return fuelTypeCategory; }
    public String getStationName() { return stationName; }
    public String getCarName() { return carName; }
    public int getCarColor() { return carColor; }
    public int getCarInitialMileage() { return carInitialMileage; }
    public Date getCarSuspendedSince() { return carSuspendedSince; }
    public double getCarBuyingPrice() { return carBuyingPrice; }
    public int getCarNumTires() { return carNumTires; }
    public boolean isGuessed() { return guessed; }
    public void setGuessed(boolean guessed) { this.guessed = guessed; }
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public Float getConsumption() { return consumption; }
    public void setConsumption(Float consumption) { this.consumption = consumption; }
    public Integer getMileageDifference() { return mileageDifference; }
    public void setMileageDifference(Integer mileageDifference) { this.mileageDifference = mileageDifference; }
}
