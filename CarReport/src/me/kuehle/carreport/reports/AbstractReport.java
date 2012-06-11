package me.kuehle.carreport.reports;

import java.util.ArrayList;
import java.util.HashMap;

import android.graphics.Color;

import com.jjoe64.graphview.GraphView;

public abstract class AbstractReport {
	public static final String KEY_LABEL = "label";
	public static final String KEY_VALUE = "value";

	protected static final int[] COLORS = { Color.BLUE, Color.RED, Color.GREEN,
			Color.YELLOW, Color.WHITE, Color.GRAY, Color.MAGENTA, Color.LTGRAY,
			Color.CYAN, Color.DKGRAY };

	private ArrayList<HashMap<String, String>> data = new ArrayList<HashMap<String, String>>();

	public abstract GraphView getGraphView();

	public void addData(String label, String value) {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(KEY_LABEL, label);
		map.put(KEY_VALUE, value);
		data.add(map);
	}

	public ArrayList<HashMap<String, String>> getData() {
		return data;
	}

	public String[] getDataKeys() {
		return new String[] { KEY_LABEL, KEY_VALUE };
	}
}
