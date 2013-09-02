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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.FuelTank;
import me.kuehle.carreport.db.FuelType;
import me.kuehle.carreport.db.OtherCost;
import me.kuehle.carreport.db.PossibleFuelTypeForFuelTank;
import me.kuehle.carreport.db.Refueling;
import android.annotation.SuppressLint;
import android.graphics.Color;

@SuppressLint("SimpleDateFormat")
public class DemoData {
	private static DateFormat dateFormat = new SimpleDateFormat(
			"dd.MM.yyyy kk:mm");

	public static void addDemoData() {
		FuelType benzinE5 = createFuelType("Benzin E5");
		FuelType benzinE10 = createFuelType("Benzin E10");
		FuelType gas = createFuelType("Gas");

		try {
			Car punto = createCar("Fiat Punto", Color.BLUE);
			FuelTank puntoTank = createFuelTank(punto, "A");
			createPossibleFuelTypeForFuelTank(benzinE5, puntoTank);
			createPossibleFuelTypeForFuelTank(benzinE10, puntoTank);
			createRefueling("01.06.2012 08:04", 120300, 41, 60.64f, false, "",
					benzinE5, puntoTank);
			createRefueling("13.06.2012 08:10", 120930, 44, 65.97f, false, "",
					benzinE5, puntoTank);
			createRefueling("28.06.2012 07:43", 121470, 37, 56.20f, true, "",
					benzinE5, puntoTank);
			createRefueling("10.07.2012 18:02", 122030, 40, 59.56f, false, "",
					benzinE5, puntoTank);
			createRefueling("25.07.2012 08:03", 122645, 42, 65.90f, false, "",
					benzinE10, puntoTank);
			createRefueling("08.08.2012 17:14", 123205, 39, 58.46f, false, "",
					benzinE10, puntoTank);
			createRefueling("27.08.2012 08:21", 123775, 41, 62.28f, false, "",
					benzinE5, puntoTank);
			createRefueling("05.09.2012 08:01", 124312, 45, 67.22f, false, "",
					benzinE5, puntoTank);
			createOtherCost("Rechtes Abblendlicht", "15.06.2012 07:25", null,
					121009, 10, new Recurrence(RecurrenceInterval.ONCE), "",
					punto);
			createOtherCost("Steuern", "01.06.2012 00:00", null, -1, 210,
					new Recurrence(RecurrenceInterval.YEAR), "", punto);

			Car astra = createCar("Opel Astra", Color.RED);
			FuelTank astraTankBenzin = createFuelTank(astra, "A");
			FuelTank astraTankGas = createFuelTank(astra, "B");
			createPossibleFuelTypeForFuelTank(benzinE5, astraTankBenzin);
			createPossibleFuelTypeForFuelTank(gas, astraTankGas);
			createRefueling("03.07.2012 08:03", 43000, 45, 67.01f, false, "",
					benzinE5, astraTankBenzin);
			createRefueling("14.07.2012 18:15", 43640, 51, 76.45f, false, "",
					benzinE5, astraTankBenzin);
			createRefueling("24.07.2012 19:04", 44300, 52, 79.51f, false, "",
					benzinE5, astraTankBenzin);
			createRefueling("03.08.2012 08:11", 44701, 34, 49.95f, true, "",
					gas, astraTankGas);
			createRefueling("17.08.2012 17:16", 45316, 49, 74.92f, false, "",
					benzinE5, astraTankBenzin);
			createRefueling("24.08.2012 07:50", 45401, 07, 11.26f, true, "",
					gas, astraTankGas);
			createRefueling("25.08.2012 07:54", 46082, 53, 78.92f, false, "",
					benzinE5, astraTankBenzin);
			createRefueling("02.09.2012 07:30", 46560, 42, 65.63f, false, "",
					gas, astraTankGas);
			createOtherCost("Steuern", "01.06.2012 00:00", null, -1, 250,
					new Recurrence(RecurrenceInterval.YEAR), "", astra);
			createOtherCost("Versicherung", "15.06.2012 00:00", null, -1, 40,
					new Recurrence(RecurrenceInterval.MONTH), "", astra);
		} catch (ParseException e) {
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

	private static Refueling createRefueling(String date, int mileage,
			float volume, float price, boolean partial, String note,
			FuelType fuelType, FuelTank fuelTank) throws ParseException {
		Refueling refueling = new Refueling(dateFormat.parse(date), mileage,
				volume, price, partial, note, fuelType, fuelTank);
		refueling.save();
		return refueling;
	}

	private static OtherCost createOtherCost(String title, String date,
			String endDate, int mileage, float price, Recurrence recurrence,
			String note, Car car) throws ParseException {
		OtherCost otherCost = new OtherCost(title, dateFormat.parse(date),
				endDate == null ? null : dateFormat.parse(endDate), mileage,
				price, recurrence, note, car);
		otherCost.save();
		return otherCost;
	}
}
