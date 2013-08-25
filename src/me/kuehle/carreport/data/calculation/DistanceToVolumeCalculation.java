package me.kuehle.carreport.data.calculation;

import java.util.ArrayList;
import java.util.List;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.FuelTank;
import me.kuehle.carreport.db.Refueling;
import android.content.Context;

public class DistanceToVolumeCalculation extends AbstractCalculation {
	public DistanceToVolumeCalculation(Context context) {
		super(context);
	}

	@Override
	public String getName() {
		return context.getString(R.string.calc_option_distance_to_volume,
				getInputUnit(), getOutputUnit());
	}

	@Override
	public String getInputUnit() {
		Preferences prefs = new Preferences(context);
		return prefs.getUnitDistance();
	}

	@Override
	public String getOutputUnit() {
		Preferences prefs = new Preferences(context);
		return prefs.getUnitVolume();
	}

	@Override
	public CalculationItem[] calculate(double input) {
		List<CalculationItem> items = new ArrayList<CalculationItem>();
		for (Car car : Car.getAll()) {
			for (FuelTank fuelTank : car.fuelTanks()) {
				List<Refueling> refuelings = fuelTank.refuelings();
				if (refuelings.size() < 2) {
					continue;
				}

				int totalDistance = refuelings.get(refuelings.size() - 1).mileage
						- refuelings.get(0).mileage;
				double totalVolume = 0;
				for (int i = 1; i < refuelings.size(); i++) {
					totalVolume += refuelings.get(i).volume;
				}

				if (totalDistance > 0 && totalVolume > 0) {
					double avgConsumption = totalVolume / totalDistance;
					items.add(new CalculationItem(String.format("%s (%s)",
							car.name, fuelTank.name), input * avgConsumption));
				}
			}
		}

		return items.toArray(new CalculationItem[items.size()]);
	}
}
