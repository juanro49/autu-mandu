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

import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.FuelTank;
import me.kuehle.carreport.db.FuelType;
import me.kuehle.carreport.db.OtherCost;
import me.kuehle.carreport.db.PossibleFuelTypeForFuelTank;
import me.kuehle.carreport.db.Refueling;

import org.joda.time.DateTime;

import android.graphics.Color;

public class DemoData {
	public static void addDemoData() {
		FuelType super95 = createFuelType("Super 95");
		FuelType superE10 = createFuelType("Super E10");
		FuelType lpg = createFuelType("LPG");

		// Fiat Punto

		Car punto = createCar("Fiat Punto", Color.BLUE);
		FuelTank puntoTank = createFuelTank(punto, "Fuel");
		createPossibleFuelTypeForFuelTank(super95, puntoTank);
		createPossibleFuelTypeForFuelTank(superE10, puntoTank);

		int puntoCount = 50;
		DateTime puntoDate = DateTime.now().minusMonths(puntoCount / 2);
		int puntoMileage = 15000;

		createOtherCost("Rechtes Abblendlicht",
				puntoDate.plusDays(randInt(50, 100)), null, 121009, 10,
				new Recurrence(RecurrenceInterval.ONCE), "", punto);
		createOtherCost("Steuern", puntoDate, null, -1, 210, new Recurrence(
				RecurrenceInterval.YEAR), "", punto);

		for (int i = 0; i < puntoCount; i++) {
			puntoDate = puntoDate.plusDays(randInt(12, 18));
			puntoMileage += randInt(570, 620);
			float volume = randFloat(43, 51);
			boolean partial = false;
			if (randInt(0, 5) == 5) {
				volume -= randFloat(20, 40);
				partial = true;
			}

			float price = volume * randFloat(140, 170) / 100;
			FuelType fuelType = super95;
			if (randInt(0, 3) == 3) {
				fuelType = superE10;
			}

			if (randInt(0, 9) != 9) {
				createRefueling(puntoDate, puntoMileage, volume, price,
						partial, "", fuelType, puntoTank);
			}
		}

		// Opel Astra

		Car astra = createCar("Opel Astra", Color.RED);
		FuelTank astraTankFuel = createFuelTank(astra, "Fuel");
		FuelTank astraTankGas = createFuelTank(astra, "Gas");
		createPossibleFuelTypeForFuelTank(super95, astraTankFuel);
		createPossibleFuelTypeForFuelTank(lpg, astraTankGas);

		int astraCount = 30;
		DateTime astraDate = DateTime.now().minusMonths(astraCount / 3);
		int astraMileage = 120000;

		createOtherCost("Steuern", astraDate, null, -1, 250, new Recurrence(
				RecurrenceInterval.YEAR), "", astra);
		createOtherCost("Versicherung", astraDate, null, -1, 40,
				new Recurrence(RecurrenceInterval.MONTH), "", astra);

		for (int i = 0; i < astraCount; i++) {
			astraDate = astraDate.plusDays(randInt(8, 13));
			astraMileage += randInt(570, 620);
			float volume = randFloat(49, 60);
			boolean partial = false;
			if (randInt(0, 5) == 5) {
				volume -= randFloat(20, 40);
				partial = true;
			}

			float price = volume * randFloat(140, 170) / 100;
			FuelTank fuelTank = astraTankFuel;
			FuelType fuelType = super95;
			if (randInt(0, 1) == 1) {
				fuelTank = astraTankGas;
				fuelType = lpg;
			}

			if (randInt(0, 9) != 9) {
				createRefueling(astraDate, astraMileage, volume, price,
						partial, "", fuelType, fuelTank);
			}
		}
	}

	private static Car createCar(String name, int color) {
		Car car = new Car(name, color, null);
		car.save();
		return car;
	}

	private static FuelType createFuelType(String name) {
		FuelType fuelType = new FuelType(name);
		fuelType.save();
		return fuelType;
	}

	private static FuelTank createFuelTank(Car car, String name) {
		FuelTank fuelTank = new FuelTank(car, name);
		fuelTank.save();
		return fuelTank;
	}

	private static PossibleFuelTypeForFuelTank createPossibleFuelTypeForFuelTank(
			FuelType fuelType, FuelTank fuelTank) {
		PossibleFuelTypeForFuelTank possibleFuelTypeForFuelTank = new PossibleFuelTypeForFuelTank(
				fuelType, fuelTank);
		possibleFuelTypeForFuelTank.save();
		return possibleFuelTypeForFuelTank;
	}

	private static Refueling createRefueling(DateTime date, int mileage,
			float volume, float price, boolean partial, String note,
			FuelType fuelType, FuelTank fuelTank) {
		Refueling refueling = new Refueling(date.toDate(), mileage, volume,
				price, partial, note, fuelType, fuelTank);
		refueling.save();
		return refueling;
	}

	private static OtherCost createOtherCost(String title, DateTime date,
			DateTime endDate, int mileage, float price, Recurrence recurrence,
			String note, Car car) {
		OtherCost otherCost = new OtherCost(title, date.toDate(),
				endDate == null ? null : endDate.toDate(), mileage, price,
				recurrence, note, car);
		otherCost.save();
		return otherCost;
	}

	private static int randInt(int min, int max) {
		return min + (int) (Math.random() * ((max - min) + 1));
	}

	private static float randFloat(int min, int max) {
		return (float) randInt(min * 10, max * 10) / 10;
	}
}
