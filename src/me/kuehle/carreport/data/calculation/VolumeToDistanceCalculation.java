package me.kuehle.carreport.data.calculation;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.FuelTank;
import me.kuehle.carreport.db.Refueling;
import me.kuehle.carreport.util.Calculator;
import android.content.Context;

public class VolumeToDistanceCalculation extends AbstractCalculation {
	public VolumeToDistanceCalculation(Context context) {
		super(context);
	}

	@Override
	public String getName() {
		return context.getString(R.string.calc_option_volume_to_distance,
				getInputUnit(), getOutputUnit());
	}

	@Override
	public String getInputUnit() {
		Preferences prefs = new Preferences(context);
		return prefs.getUnitVolume();
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
			for (FuelTank fuelTank : car.fuelTanks()) {
				List<Refueling> refuelings = fuelTank.refuelings();
				Vector<Double> consumptions = new Vector<Double>();

				int lastMileage = -1;
				float volume = 0;
				for (Refueling refueling : refuelings) {
					volume += refueling.volume;
					if (!refueling.partial) {
						if (lastMileage > -1 && lastMileage < refueling.mileage) {
							double consumption = volume
									/ (refueling.mileage - lastMileage);
							consumptions.add(consumption);
						}

						lastMileage = refueling.mileage;
						volume = 0;
					}
				}

				if (consumptions.size() > 0) {
					double avgConsumption = Calculator.avg(consumptions);
					items.add(new CalculationItem(String.format("%s (%s)",
							car.name, fuelTank.name), input / avgConsumption));
				}
			}
		}

		return items.toArray(new CalculationItem[items.size()]);
	}
}
