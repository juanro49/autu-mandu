package me.kuehle.carreport.data.calculation;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.OtherCost;
import me.kuehle.carreport.db.Refueling;
import me.kuehle.carreport.util.Calculator;
import android.content.Context;

public class PriceToDistanceCalculation extends AbstractCalculation {
	public PriceToDistanceCalculation(Context context) {
		super(context);
	}

	@Override
	public String getName() {
		return context.getString(R.string.calc_option_price_to_distance,
				getInputUnit(), getOutputUnit());
	}

	@Override
	public String getInputUnit() {
		Preferences prefs = new Preferences(context);
		return prefs.getUnitCurrency();
	}

	@Override
	public String getOutputUnit() {
		Preferences prefs = new Preferences(context);
		return prefs.getUnitDistance();
	}

	@Override
	public CalculationItem[] calculate(double input) {
		List<CalculationItem> items = new ArrayList<CalculationItem>();
		for (Car car : Car.getAll()) {
			Vector<Double> distancePricesRefuelings = new Vector<Double>();
			Vector<Double> distancePricesOtherCosts = new Vector<Double>();

			int lastMileage = -1;
			float price = 0;
			for (Refueling refueling : car.refuelings()) {
				price += refueling.price;
				if (!refueling.partial) {
					if (lastMileage > -1 && lastMileage < refueling.mileage) {
						double distancePrice = price
								/ (refueling.mileage - lastMileage);
						distancePricesRefuelings.add(distancePrice);
					}

					lastMileage = refueling.mileage;
					price = 0;
				}
			}

			lastMileage = -1;
			for (OtherCost otherCost : car.otherCosts()) {
				if (lastMileage > -1 && lastMileage < otherCost.mileage) {
					double distancePrice = otherCost.price
							/ (otherCost.mileage - lastMileage);
					distancePricesOtherCosts.add(distancePrice);
				}

				lastMileage = otherCost.mileage;
			}

			if (distancePricesRefuelings.size() > 0
					|| distancePricesOtherCosts.size() > 0) {
				double avgDistancePrice = 0;
				if (distancePricesRefuelings.size() > 0) {
					avgDistancePrice += Calculator
							.avg(distancePricesRefuelings);
				}

				if (distancePricesOtherCosts.size() > 0) {
					avgDistancePrice += Calculator
							.avg(distancePricesOtherCosts);
				}

				items.add(new CalculationItem(car.name, input
						/ avgDistancePrice));
			}
		}

		return items.toArray(new CalculationItem[items.size()]);
	}
}
