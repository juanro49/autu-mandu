package me.kuehle.carreport.gui;

import java.util.Collections;
import java.util.List;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.FuelConsumption;
import me.kuehle.carreport.data.balancing.RefuelingBalancer;
import me.kuehle.carreport.db.Refueling;
import android.text.format.DateFormat;
import android.util.SparseArray;

public class DataListRefuelingFragment extends
		AbstractDataListFragment<Refueling> {
	@Override
	protected int getAlertDeleteManyMessage() {
		return R.string.alert_delete_refuelings_message;
	}

	@Override
	protected int getExtraEdit() {
		return DataDetailActivity.EXTRA_EDIT_REFUELING;
	}

	@Override
	protected SparseArray<String> getItemData(List<Refueling> refuelings,
			int position) {
		Preferences prefs = new Preferences(getActivity());
		java.text.DateFormat dateFmt = DateFormat.getDateFormat(getActivity());
		Refueling refueling = (Refueling) refuelings.get(position);

		SparseArray<String> data = new SparseArray<String>();

		if (refueling.guessed) {
			data.put(R.id.title, getString(R.string.missing_refueling));
			return data;
		}

		data.put(R.id.title, getString(R.string.edit_title_refueling));
		data.put(R.id.subtitle, refueling.fuelType.name);
		data.put(R.id.date, dateFmt.format(refueling.date));

		data.put(
				R.id.data1,
				String.format("%d %s", refueling.mileage,
						prefs.getUnitDistance()));
		Refueling nextRefueling = getNextRefueling(refuelings, position);
		if (nextRefueling != null) {
			data.put(
					R.id.data1_calculated,
					String.format("+ %d %s", refueling.mileage
							- nextRefueling.mileage, prefs.getUnitDistance()));
		}

		data.put(
				R.id.data2,
				String.format("%.2f %s", refueling.price,
						prefs.getUnitCurrency()));
		data.put(R.id.data2_calculated, String.format("%.3f %s/%s",
				refueling.price / refueling.volume, prefs.getUnitCurrency(),
				prefs.getUnitVolume()));

		data.put(
				R.id.data3,
				String.format("%.2f %s", refueling.volume,
						prefs.getUnitVolume()));
		if (refueling.partial) {
			data.put(R.id.data3_calculated, getString(R.string.label_partial));
		} else if (nextRefueling != null) {
			float diffVolume = refueling.volume;
			for (int i = position + 1; nextRefueling != null; i++) {
				if (nextRefueling.partial) {
					diffVolume += nextRefueling.volume;
				} else {
					int diffMileage = refueling.mileage - nextRefueling.mileage;
					FuelConsumption fc = new FuelConsumption(getActivity());
					data.put(R.id.data3_calculated, String.format("%.2f %s",
							fc.computeFuelConsumption(diffVolume, diffMileage),
							fc.getUnitLabel()));
					break;
				}

				nextRefueling = getNextRefueling(refuelings, i);
			}
		}

		return data;
	}

	@Override
	protected List<Refueling> getItems() {
		RefuelingBalancer balancer = new RefuelingBalancer(getActivity());
		List<Refueling> refuelings = balancer.getBalancedRefuelings(mCar);
		Collections.reverse(refuelings);
		return refuelings;
	}

	@Override
	protected boolean isMissingData(List<Refueling> refuelings, int position) {
		return refuelings.get(position).guessed;
	}

	private Refueling getNextRefueling(List<Refueling> refuelings, int position) {
		while (++position < refuelings.size()) {
			if (!refuelings.get(position).guessed) {
				return refuelings.get(position);
			}
		}

		return null;
	}
}