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

package me.kuehle.carreport.reports;

import java.text.DateFormat;
import java.util.Date;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.Helper;
import me.kuehle.carreport.db.OtherCost;
import me.kuehle.carreport.db.OtherCostTable;
import me.kuehle.carreport.db.Refueling;
import me.kuehle.carreport.db.RefuelingTable;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Months;
import org.joda.time.Years;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.jjoe64.graphview.GraphView;

public class CostsReport extends AbstractReport {
	private String unit;

	public CostsReport(Context context) {
		super(context);
		
		Preferences prefs = new Preferences(context);
		unit = prefs.getUnitCurrency();

		Car[] cars = Car.getAll();
		for (Car car : cars) {
			int startTacho = Integer.MAX_VALUE;
			int endTacho = Integer.MIN_VALUE;
			DateTime startDate = new DateTime();
			DateTime endDate = new DateTime();
			double costs = 0;

			Refueling[] refuelings = Refueling.getAllForCar(car, true);
			OtherCost[] otherCosts = OtherCost.getAllForCar(car, true);

			if ((refuelings.length + otherCosts.length) < 2) {
				addData(context.getString(R.string.report_not_enough_data), "",
						car);
				continue;
			}

			// Get startTacho and endTacho.
			Helper helper = Helper.getInstance();
			SQLiteDatabase db = helper.getReadableDatabase();
			Cursor cursor = db.rawQuery(String.format(
					"SELECT min(tacho), max(tacho) FROM ("
							+ "SELECT %s AS tacho FROM %s UNION "
							+ "SELECT %s AS tacho FROM %s WHERE tacho > -1)",
					RefuelingTable.COL_TACHO, RefuelingTable.NAME,
					OtherCostTable.COL_TACHO, OtherCostTable.NAME), null);
			cursor.moveToFirst();
			startTacho = cursor.getInt(0);
			endTacho = cursor.getInt(1);
			cursor.close();

			// Get startDate and endDate.
			if (refuelings.length > 0) {
				startDate = new DateTime(refuelings[0].getDate());
				endDate = new DateTime(
						refuelings[refuelings.length - 1].getDate());
			}
			if (otherCosts.length > 0) {
				startDate = startDate.isBefore(otherCosts[0].getDate()
						.getTime()) ? startDate : new DateTime(
						otherCosts[0].getDate());
				endDate = endDate.isAfter(otherCosts[otherCosts.length - 1]
						.getDate().getTime()) ? endDate : new DateTime(
						otherCosts[otherCosts.length - 1].getDate());
			}

			for (Refueling refueling : refuelings) {
				if (startDate.isBefore(refueling.getDate().getTime() + 1)) {
					costs += refueling.getPrice();
				}
			}
			for (OtherCost otherCost : otherCosts) {
				Date date = otherCost.getDate();
				if (startDate.isBefore(date.getTime() + 1)) {
					costs += otherCost.getPrice()
							* otherCost.getRecurrence().getRecurrencesSince(
									date);
				}
			}

			int elapsedDays = Math.max(1, Days.daysBetween(startDate, endDate)
					.getDays());
			addData(context.getString(R.string.report_day),
					String.format("%.2f %s", costs / elapsedDays, unit), car);
			int elapsedMonths = Math.max(1,
					Months.monthsBetween(startDate, endDate).getMonths());
			addData(context.getString(R.string.report_month),
					String.format("%.2f %s", costs / elapsedMonths, unit), car);
			int elapsedYears = Math.max(1,
					Years.yearsBetween(startDate, endDate).getYears());
			addData(context.getString(R.string.report_year),
					String.format("%.2f %s", costs / elapsedYears, unit), car);
			int tachoDiff = Math.max(1, endTacho - startTacho);
			addData(prefs.getUnitDistance(),
					String.format("%.2f %s", costs / tachoDiff, unit), car);

			addData(context.getString(R.string.report_since, DateFormat
					.getDateInstance().format(startDate.toDate())),
					String.format("%.2f %s", costs, unit), car);
		}
	}

	@Override
	public GraphView getGraphView() {
		return null;
	}
}
