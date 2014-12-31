/*
 * Copyright 2014 Jan Kühle
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

package me.kuehle.carreport.data.balancing;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.Refueling;
import me.kuehle.carreport.util.Calculator;
import android.content.Context;

public class RefuelingBalancer {
	public static final float MAX_RELATIVE_CONSUMPTION_DEVIATION = 0.2f;

	private Preferences prefs;

	public RefuelingBalancer(Context context) {
		this.prefs = new Preferences(context);
	}

    public List<Refueling> getBalancedRefuelings(Car car) {
        return getBalancedRefuelings(car.getRefuelings());
    }

    public List<Refueling> getBalancedRefuelings(Car car, String fuelTypeCategory) {
        return getBalancedRefuelings(car.getRefuelingsByFuelTypeCategory(fuelTypeCategory));
    }

	private List<Refueling> getBalancedRefuelings(List<Refueling> refuelings) {
		if (!areRefuelingsValid(refuelings)) {
			return refuelings;
		}

		if (!this.prefs.isAutoGuessMissingDataEnabled()) {
			return refuelings;
		}

		float avgConsumption = getBalancedAverageConsumption(refuelings);
		int avgDistance = getBalancedAverageDistanceOfFullRefuelings(refuelings);
		float avgPricePerUnit = getAveragePricePerUnit(refuelings);

		int distance = 0;
		float volume = 0;
		int lastFullRefueling = -1;
		for (int i = 0; i < refuelings.size(); i++) {
			Refueling refueling = refuelings.get(i);
			if (lastFullRefueling < 0) {
				if (!refueling.partial) {
					lastFullRefueling = i;
				}

				continue;
			}

			distance += refueling.mileage - refuelings.get(i - 1).mileage;
			volume += refuelings.get(i).volume;
			if (refueling.partial) {
				continue;
			}

			double consumption = volume / distance;
			if (consumption / avgConsumption < (1 - MAX_RELATIVE_CONSUMPTION_DEVIATION)) {
				// There seem to be missing entries before this refueling. In
				// order to get an average consumption for this refueling, the
				// following amount of fuel is missing. In the following we will
				// create guessed refuelings, that will count up exactly to this
				// volume.
				float missingVolume = avgConsumption * distance - volume;

				// If there are partial entries between this and the last full
				// refueling, some of the missing refuelings probably belong
				// between these partial one.
				// We check this by comparing the distance, which the car can
				// probably drive with one full tank, to the distance of the
				// partial refuelings. If the refueling is to far away
				// (distance-wise), we assume a refueling is missing before it
				// and create one.
				// Possible distance is always the distance the car can possibly
				// drive since the last (including partial) refueling.
				int possibleDistance = avgDistance;
				for (int pI = lastFullRefueling + 1; pI < i; pI++) {
					int pDistance = refuelings.get(pI).mileage - refuelings.get(pI - 1).mileage;
					if (pDistance <= possibleDistance) {
						// Distance is possible so we assume nothing is missing
						// and adjust the possible distance for the next
						// refueling.
						possibleDistance -= pDistance;
						possibleDistance += (int) (refuelings.get(pI).volume / avgConsumption);
					} else {
						// It doesn't seem possible that the car could drive this
						// far without another refueling so we add one.

						// Always try to refill as much fuel as possible...
						float newVolume = avgDistance * avgConsumption;
						// but ensure that the current partial refueling is
						// feasible
						// --> the volume is possible (tank is not overfull
						// after refueling)...
						float pVolume = refuelings.get(pI).volume;
						if (newVolume > pDistance * avgConsumption - pVolume) {
							newVolume = pDistance * avgConsumption - pVolume;
						}
						// and ensure that we don't add more volume than what's
						// actually missing.
						if (newVolume > missingVolume) {
							newVolume = missingVolume;
						}

						// Based on the volume calculate the mileage at which
						// the refueling happened, so the consumption is about
						// average.
						float volumeSinceLastFullRefueling = newVolume;
						for (int pI2 = lastFullRefueling + 1; pI2 < pI; pI2++) {
							volumeSinceLastFullRefueling += refuelings.get(pI2).volume;
						}
						int newMileage = refuelings.get(lastFullRefueling).mileage
								+ (int) (volumeSinceLastFullRefueling / avgConsumption);

						// Try to calculate a date when the refueling happened.
						long pTimeDiff = refuelings.get(pI).date.getTime()
								- refuelings.get(pI - 1).date.getTime();
						Date newDate = new Date(refuelings.get(pI - 1).date.getTime()
                                + (long) (pTimeDiff / pDistance * (newVolume / avgConsumption)));

						// Calculate an average price.
						float newPrice = newVolume * avgPricePerUnit;

						// Add the refueling to the list and mark it as "guessed".
						Refueling guess = new Refueling(newDate, newMileage, newVolume, newPrice,
                                false, "", refueling.fuelType, refueling.car);
						guess.guessed = true;
						refuelings.add(pI, guess);

						missingVolume -= newVolume;
						possibleDistance = avgDistance;
						lastFullRefueling = pI;
						i++;

						// Do not increase pI to check this refueling again.
						// There may be missing more refuelings before this one.
						// pI++;
					}
				}

				// Now for the remaining missing volume add refueling just
				// before the current one.
				while (missingVolume > 0) {
					// Always try to refill as much fuel as possible...
					float newVolume = avgDistance * avgConsumption;
					// but ensure that we don't add more volume than what's
					// actually missing.
					if (newVolume > missingVolume) {
						newVolume = missingVolume;
					}

					// Based on the volume calculate the mileage at which the
					// refueling happened, so the consumption is about average.
					float volumeSinceLastFullRefueling = newVolume;
					for (int pI2 = lastFullRefueling + 1; pI2 < i; pI2++) {
						volumeSinceLastFullRefueling += refuelings.get(pI2).volume;
					}

					boolean partial = false;
					int newMileage = refuelings.get(lastFullRefueling).mileage
							+ (int) (volumeSinceLastFullRefueling / avgConsumption);

					// TODO: These partial refuelings are sometimes after a very
					// short distance (< 200km), which is unlikely.
					if (newMileage < refuelings.get(i - 1).mileage) {
						newMileage = refuelings.get(i - 1).mileage
								+ possibleDistance
								+ (int) (refuelings.get(i - 1).volume / avgConsumption);
						partial = true;
					}

					// Try to calculate a date when the refueling happened.
					int cDistance = refueling.mileage - refuelings.get(i - 1).mileage;
					long cTimeDiff = refueling.date.getTime()
                            - refuelings.get(i - 1).date.getTime();
					Date newDate = new Date(refuelings.get(i - 1).date.getTime()
                            + (long) (cTimeDiff / cDistance * (newVolume / avgConsumption)));

					// Calculate an average price.
					float newPrice = newVolume * avgPricePerUnit;

					// Add the refueling to the list and mark it as "guessed".
					Refueling guess = new Refueling(newDate, newMileage, newVolume, newPrice,
                            partial, "", refueling.fuelType, refueling.car);
					guess.guessed = true;
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

	/**
	 * Gets the average fuel consumption, not taking into account the
	 * outstanding values, where refuelings were missing.
	 * 
	 * @param refuelings
	 *            a list of refuelings with the same fuel tank.
	 * @return the balanced average fuel consumption.
	 */
	private static float getBalancedAverageConsumption(List<Refueling> refuelings) {
		Vector<Float> allConsumptions = new Vector<>();
		Vector<Integer> allDistances = new Vector<>();
		Vector<Float> allVolumes = new Vector<>();

		// Calculate consumptions for all refuelings in the specified list.
		// There is not need to use the fuel consumption class here because
		// these values are just for internal comparison.
		int totalDistance = 0, distance = 0;
		float totalVolume = 0, volume = 0;
		int lastFullRefueling = -1;
		for (int i = 0; i < refuelings.size(); i++) {
			Refueling refueling = refuelings.get(i);
			if (lastFullRefueling < 0) {
				if (!refueling.partial) {
					lastFullRefueling = i;
				}

				continue;
			}

			distance += refueling.mileage - refuelings.get(i - 1).mileage;
			volume += refueling.volume;

			if (!refueling.partial) {
				totalDistance += distance;
				totalVolume += volume;

				allConsumptions.add(volume / distance);
				allDistances.add(distance);
				allVolumes.add(volume);

				distance = 0;
				volume = 0;

				lastFullRefueling = i;
			}
		}

		float avgConsumption = totalVolume / totalDistance;

		// Remove outstanding values from the average.
		boolean updated;
		do {
			updated = false;
			for (int i = allConsumptions.size() - 1; i >= 0; i--) {
				if (allConsumptions.get(i) / avgConsumption < (1 - MAX_RELATIVE_CONSUMPTION_DEVIATION)) {
					totalDistance -= allDistances.get(i);
					totalVolume -= allVolumes.get(i);
					avgConsumption = totalVolume / totalDistance;

					allConsumptions.remove(i);
					allDistances.remove(i);
					allVolumes.remove(i);

					updated = true;
				}
			}
		} while (updated);

		return avgConsumption;
	}

	/**
	 * Gets the average distance, that was driven before a full refueling. This
	 * should give an idea of how far the car gets with a full tank.
	 * 
	 * @param refuelings
	 *            a list of refuelings with the same fuel tank.
	 * @return the average distance for full refuelings.
	 */
	private static int getBalancedAverageDistanceOfFullRefuelings(List<Refueling> refuelings) {
		Vector<Integer> allDistances = new Vector<>();

		for (int i = 1; i < refuelings.size(); i++) {
			if (!refuelings.get(i).partial) {
				allDistances.add(refuelings.get(i).mileage
						- refuelings.get(i - 1).mileage);
			}
		}

		// Remove the top 20% to get rid of the very high distances.
		Collections.sort(allDistances);
		int size = allDistances.size();
		for (int i = size - 1; i >= size * 0.8; i--) {
			allDistances.remove(i);
		}

		int avgDistance = Calculator.avg(allDistances
				.toArray(new Integer[allDistances.size()]));

		// Remove outstanding values from the average.
		boolean updated;
		do {
			updated = false;
			for (int i = allDistances.size() - 1; i >= 0; i--) {
				float relativeDistance = (float) allDistances.get(i)
						/ (float) avgDistance;
				if (relativeDistance < (1 - MAX_RELATIVE_CONSUMPTION_DEVIATION)
						|| relativeDistance > (1 + MAX_RELATIVE_CONSUMPTION_DEVIATION)) {
					allDistances.remove(i);
					avgDistance = Calculator.avg(allDistances
							.toArray(new Integer[allDistances.size()]));

					updated = true;
				}
			}
		} while (updated);

		return avgDistance;
	}

	/**
	 * Gets the average fuel price per unit. It can be used to calculate an
	 * average price for a refueling based on the volume.
	 * 
	 * @param refuelings
	 *            a list of refuelings with the same fuel tank.
	 * @return the average price per unit (e.g. EUR / liter).
	 */
	private static float getAveragePricePerUnit(List<Refueling> refuelings) {
		float[] allPrices = new float[refuelings.size()];
		for (int i = 0; i < allPrices.length; i++) {
			allPrices[i] = refuelings.get(i).getFuelPrice();
		}

		return Calculator.avg(allPrices);
	}

	/**
	 * Checks if the specified refuelings (that are ordered by date) are ordered
	 * by mileage as well. In previous versions and using the CSV import users
	 * are able to insert refuelings with any mileage value.
	 * 
	 * When refuelings are found, that don't have an increasing mileage, they
	 * are flagged as invalid and false is returned.
	 * 
	 * @param refuelings
	 * @return
	 */
	private static boolean areRefuelingsValid(List<Refueling> refuelings) {
		boolean valid = true;
		for (int i = 1; i < refuelings.size(); i++) {
			Refueling previousRefueling = refuelings.get(i - 1);
			Refueling refueling = refuelings.get(i);

			if (refueling.mileage <= previousRefueling.mileage) {
				refueling.valid = false;
				valid = false;
			}
		}

		return valid;
	}
}
