package me.kuehle.carreport.data.calculation;

import java.util.ArrayList;
import java.util.List;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.data.balancing.RefuelingBalancer;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.OtherCost;
import me.kuehle.carreport.db.Refueling;
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
		List<CalculationItem> items = new ArrayList<>();
		for (Car car : Car.getAll()) {
			double totalCosts = 0;
			int startMileage = -1;
			int endMileage = -1;

			RefuelingBalancer balancer = new RefuelingBalancer(context);
			List<Refueling> refuelings = balancer.getBalancedRefuelings(car);
			for (int i = 0; i < refuelings.size(); i++) {
				Refueling refueling = refuelings.get(i);
				if (startMileage == -1) {
					if (!refueling.partial) {
						startMileage = refueling.mileage;
					}

					continue;
				}

				totalCosts += refueling.price;
				endMileage = refueling.mileage;
			}

			List<OtherCost> otherCosts = car.getOtherCosts();
			for (OtherCost otherCost : otherCosts) {
				int recurrences;
				if (otherCost.endDate == null) {
					recurrences = otherCost.recurrence.getRecurrencesSince(otherCost.date);
				} else {
					recurrences = otherCost.recurrence.getRecurrencesBetween(otherCost.date,
                            otherCost.endDate);
				}

				totalCosts += otherCost.price * recurrences;

				if (otherCost.mileage > -1) {
					startMileage = Math.min(startMileage, otherCost.mileage);
					endMileage = Math.max(endMileage, otherCost.mileage);
				}
			}

			if (totalCosts > 0 && startMileage > -1 && endMileage > startMileage) {
				double avgDistancePrice = totalCosts / (endMileage - startMileage);
				items.add(new CalculationItem(car.name, input / avgDistancePrice));
			}
		}

		return items.toArray(new CalculationItem[items.size()]);
	}
}
