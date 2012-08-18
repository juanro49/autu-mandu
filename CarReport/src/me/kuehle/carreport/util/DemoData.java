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

import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.OtherCost;
import me.kuehle.carreport.db.Refueling;
import android.graphics.Color;

public class DemoData {
	public static void  addDemoData() {
		DateFormat date = DateFormat.getDateTimeInstance();
		
		try {
			Car car1 = Car.create("Fiat Punto", Color.BLUE);
			Refueling.create(date.parse("01.03.2012 08:04:00"), 120300, 41, 60.64f, false, "", car1);
			Refueling.create(date.parse("13.03.2012 08:10:00"), 120930, 44, 65.97f, false, "", car1);
			Refueling.create(date.parse("28.03.2012 07:43:00"), 121470, 37, 56.20f, true, "", car1);
			Refueling.create(date.parse("10.04.2012 18:02:00"), 122030, 40, 59.56f, false, "", car1);
			Refueling.create(date.parse("25.04.2012 08:03:00"), 122645, 42, 65.90f, false, "", car1);
			Refueling.create(date.parse("08.05.2012 17:14:00"), 123205, 39, 58.46f, false, "", car1);
			Refueling.create(date.parse("27.05.2012 08:21:00"), 123775, 41, 62.28f, false, "", car1);
			OtherCost.create("Rechtes Abblendlicht", date.parse("15.03.2012 07:25:00"), 121009, 10, new Recurrence(RecurrenceInterval.ONCE), "", car1);
			OtherCost.create("Steuern", date.parse("01.03.2012 00:00:00"), -1, 210, new Recurrence(RecurrenceInterval.YEAR), "", car1);
			
			Car car2 = Car.create("Opel Astra", Color.CYAN);
			Refueling.create(date.parse("03.04.2012 08:03:00"), 43000, 45, 67.01f, false, "", car2);
			Refueling.create(date.parse("14.04.2012 18:15:00"), 43640, 51, 76.45f, false, "", car2);
			Refueling.create(date.parse("24.04.2012 19:04:00"), 44300, 52, 79.51f, false, "", car2);
			Refueling.create(date.parse("03.05.2012 08:11:00"), 44701, 34, 49.95f, true, "", car2);
			Refueling.create(date.parse("17.05.2012 17:16:00"), 45316, 49, 74.92f, false, "", car2);
			Refueling.create(date.parse("24.05.2012 07:50:00"), 45401, 07, 11.26f, true, "", car2);
			Refueling.create(date.parse("25.05.2012 07:54:00"), 46082, 53, 78.92f, false, "", car2);
			OtherCost.create("Steuern", date.parse("01.03.2012 00:00:00"), -1, 250, new Recurrence(RecurrenceInterval.YEAR), "", car2);
			OtherCost.create("Versicherung", date.parse("15.03.2012 00:00:00"), -1, 40, new Recurrence(RecurrenceInterval.MONTH), "", car2);
		} catch (ParseException e) {
		}
	}
}
