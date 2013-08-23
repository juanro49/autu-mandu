/*
 * Copyright 2013 Jan Kühle
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

package me.kuehle.carreport.gui.util;

import me.kuehle.carreport.R;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

public class SimpleAnimator {
	public static enum Property {
		Height, Weight
	}

	private Context context;
	private View view;
	private Property property;
	private int duration;

	private float origWeight;
	private int origHeight;

	public SimpleAnimator(Context context, View view, Property property) {
		this(context, view, property, -1);
	}

	public SimpleAnimator(Context context, View view, Property property,
			int duration) {
		this.context = context;
		this.view = view;
		this.property = property;
		this.duration = duration;

		this.origWeight = ((LinearLayout.LayoutParams) view.getLayoutParams()).weight;
		this.origHeight = view.getLayoutParams().height;
	}

	public void show() {
		show(null, null);
	}

	public void show(Runnable onStart, Runnable onEnd) {
		AnimatorSet animator = (AnimatorSet) createAnimator(R.animator.show,
				onStart, onEnd);

		ValueAnimator valueAnimator = (ValueAnimator) animator
				.getChildAnimations().get(0);
		if (property == Property.Height) {
			int curHeight = view.getLayoutParams().height;
			applyHeightUpdater(valueAnimator, curHeight, origHeight);
		} else {
			float curWeight = ((LinearLayout.LayoutParams) view
					.getLayoutParams()).weight;
			applyWeightUpdater(valueAnimator, curWeight, origWeight);
		}

		animator.start();
	}

	public void hide() {
		hide(null, null);
	}

	public void hide(Runnable onStart, Runnable onEnd) {
		AnimatorSet animator = (AnimatorSet) createAnimator(R.animator.hide,
				onStart, onEnd);

		ValueAnimator valueAnimator = (ValueAnimator) animator
				.getChildAnimations().get(0);
		if (property == Property.Height) {
			int curHeight = view.getLayoutParams().height;
			applyHeightUpdater(valueAnimator, curHeight, 0);
		} else {
			float curWeight = ((LinearLayout.LayoutParams) view
					.getLayoutParams()).weight;
			applyWeightUpdater(valueAnimator, curWeight, 0);
		}

		animator.start();
	}

	private Animator createAnimator(int animatorId, Runnable onStart,
			Runnable onEnd) {
		Animator animator = AnimatorInflater.loadAnimator(context, animatorId);
		animator.setTarget(view);
		if (duration > 0) {
			animator.setDuration(duration);
		}
		attachRunnable(animator, onStart, onEnd);
		return animator;
	}

	private void applyHeightUpdater(ValueAnimator animator, int from, int to) {
		animator.setValues(PropertyValuesHolder.ofInt((String) null, from, to));
		animator.addUpdateListener(new AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				int height = (Integer) animation.getAnimatedValue();
				view.getLayoutParams().height = height;
				view.requestLayout();
			}
		});
	}

	private void applyWeightUpdater(ValueAnimator animator, float from, float to) {
		animator.setValues(PropertyValuesHolder
				.ofFloat((String) null, from, to));
		animator.addUpdateListener(new AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				float weight = (Float) animation.getAnimatedValue();
				((LinearLayout.LayoutParams) view.getLayoutParams()).weight = weight;
				view.requestLayout();
			}
		});
	}

	private void attachRunnable(Animator animator, final Runnable onStart,
			final Runnable onEnd) {
		if (onStart != null || onEnd != null) {
			animator.addListener(new AnimatorListener() {
				@Override
				public void onAnimationStart(Animator animation) {
					if (onStart != null) {
						onStart.run();
					}
				}

				@Override
				public void onAnimationRepeat(Animator animation) {
				}

				@Override
				public void onAnimationEnd(Animator animation) {
					if (onEnd != null) {
						onEnd.run();
					}
				}

				@Override
				public void onAnimationCancel(Animator animation) {
				}
			});
		}
	}
}
