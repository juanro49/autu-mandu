package me.kuehle.carreport.util;

import android.content.Context;
import android.util.TypedValue;

public class Calculator {
	private Context context;

	public Calculator(Context context) {
		this.context = context;
	}

	public int dpToPx(float dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
				context.getResources().getDisplayMetrics());
	}

	public int spToPx(float sp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp,
				context.getResources().getDisplayMetrics());
	}

	public float pxToSp(int px) {
		float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
		return px / scaledDensity;
	}
}
