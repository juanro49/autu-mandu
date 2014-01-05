package me.kuehle.carreport.data.calculation;

import java.util.ArrayList;
import java.util.List;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.data.balancing.RefuelingBalancer;
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
				RefuelingBalancer balancer = new RefuelingBalancer(context);
				List<Refueling> refuelings = balancer
						.getBalancedRefuelings(fuelTank);

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

					distance += refueling.mileage
							- refuelings.get(i - 1).mileage;
					volume += refueling.volume;

					if (!refueling.partial) {
						totalDistance += distance;
						totalVolume += volume;

						distance = 0;
						volume = 0;

						lastFullRefueling = i;
					}
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
