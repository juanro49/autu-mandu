package me.kuehle.carreport.reports;

import java.text.DateFormat;
import java.util.Date;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.OtherCost;
import me.kuehle.carreport.db.Refueling;
import android.content.Context;

import com.jjoe64.graphview.GraphView;

public class CostsReport extends AbstractReport {
	private String unit;

	public CostsReport(Context context) {
		Preferences prefs = new Preferences(context);
		unit = prefs.getUnitCurrency();

		Car[] cars = Car.getAll();
		for (Car car : cars) {
			int startTacho = Integer.MAX_VALUE, endTacho = Integer.MIN_VALUE;
			long startDate = Long.MAX_VALUE, endDate = Long.MIN_VALUE;
			double costs = 0;

			Refueling[] refuelings = Refueling.getAllForCar(car, true);
			OtherCost[] otherCosts = OtherCost.getAllForCar(car, true);

			if ((refuelings.length + otherCosts.length) < 2) {
				addData(car.getName(),
						context.getString(R.string.report_not_enough_data));
				continue;
			}

			if (refuelings.length > 0) {
				startTacho = refuelings[0].getTachometer();
				endTacho = refuelings[refuelings.length - 1].getTachometer();
				startDate = refuelings[0].getDate().getTime();
				endDate = refuelings[refuelings.length - 1].getDate().getTime();
			}
			if (otherCosts.length > 0) {
				startTacho = Math
						.min(startTacho, otherCosts[0].getTachometer());
				endTacho = Math.max(endTacho,
						otherCosts[otherCosts.length - 1].getTachometer());
				startDate = Math.min(startDate, otherCosts[0].getDate()
						.getTime());
				endDate = Math.max(endDate, otherCosts[otherCosts.length - 1]
						.getDate().getTime());
			}

			for (Refueling refueling : refuelings) {
				if (refueling.getDate().getTime() != startDate) {
					costs += refueling.getPrice();
				}
			}
			for (OtherCost otherCost : otherCosts) {
				if (otherCost.getDate().getTime() != startDate) {
					costs += otherCost.getPrice();
				}
			}

			addData(car.getName() + ": "
					+ context.getString(R.string.report_day),
					String.format("%.2f %s", (costs / (endDate - startDate))
							* 1000 * 60 * 60 * 24, unit));
			addData(car.getName() + ": "
					+ context.getString(R.string.report_month),
					String.format("%.2f %s", (costs / (endDate - startDate))
							* 1000 * 60 * 60 * 24 * 30, unit));
			addData(car.getName() + ": "
					+ context.getString(R.string.report_year),
					String.format("%.2f %s", (costs / (endDate - startDate))
							* 1000 * 60 * 60 * 24 * 365, unit));
			addData(car.getName() + ": " + prefs.getUnitDistance(),
					String.format("%.2f %s", costs / (endTacho - startTacho),
							unit));

			addData(car.getName()
					+ ": "
					+ context.getString(R.string.report_since, DateFormat
							.getDateInstance().format(new Date(startDate))),
					String.format("%.2f %s", costs, unit));
		}
	}

	@Override
	public GraphView getGraphView() {
		return null;
	}
}
