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
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class SimpleAnimator {
	public enum Property {
		Height, Weight
	}

	private Context context;
	private View view;
	private Property property;
	private int duration;

	private float origWeight;
	private int origHeight;
	private int origHeightMeasured;

	public SimpleAnimator(Context context, View view, Property property) {
		this(context, view, property, -1);
	}

	public SimpleAnimator(Context context, View view, Property property, int duration) {
		this.context = context;
		this.view = view;
		this.property = property;
		this.duration = duration;

        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params instanceof LinearLayout.LayoutParams) {
            origWeight = ((LinearLayout.LayoutParams) params).weight;
        } else if (property == Property.Weight) {
            throw new IllegalArgumentException("You can only animate weight property in linear " +
                    "layouts.");
        }

		origHeight = params.height;
		if (origHeight == ViewGroup.LayoutParams.WRAP_CONTENT
				|| origHeight == ViewGroup.LayoutParams.MATCH_PARENT) {
			int widthSpec = MeasureSpec.makeMeasureSpec(params.width, MeasureSpec.EXACTLY);
			int heightSpec = MeasureSpec.makeMeasureSpec(params.height, MeasureSpec.EXACTLY);
			view.measure(widthSpec, heightSpec);
			origHeightMeasured = view.getMeasuredHeight();
		}
	}

	public void show() {
		show(null, null);
	}

	public void show(Runnable onStart, Runnable onEnd) {
		AnimatorSet animator = (AnimatorSet) createAnimator(R.animator.show, onStart, onEnd);

		ValueAnimator valueAnimator = (ValueAnimator) animator.getChildAnimations().get(0);
		if (property == Property.Height) {
			int from = view.getLayoutParams().height;
			int to = origHeight;
			if (to == ViewGroup.LayoutParams.WRAP_CONTENT
					|| to == ViewGroup.LayoutParams.MATCH_PARENT) {
				to = origHeightMeasured;
				attachRunnable(animator, null, new Runnable() {
					@Override
					public void run() {
						view.getLayoutParams().height = origHeight;
						view.requestLayout();
					}
				});
			}

			applyHeightUpdater(valueAnimator, from, to);
		} else {
			float curWeight = ((LinearLayout.LayoutParams) view.getLayoutParams()).weight;
			applyWeightUpdater(valueAnimator, curWeight, origWeight);
		}

		animator.start();
	}

	public void hide() {
		hide(null, null);
	}

	public void hide(Runnable onStart, Runnable onEnd) {
		AnimatorSet animator = (AnimatorSet) createAnimator(R.animator.hide, onStart, onEnd);

		ValueAnimator valueAnimator = (ValueAnimator) animator.getChildAnimations().get(0);
		if (property == Property.Height) {
			int from = view.getLayoutParams().height;
			if (from == ViewGroup.LayoutParams.WRAP_CONTENT
					|| from == ViewGroup.LayoutParams.MATCH_PARENT) {
				from = origHeightMeasured;
			}

			applyHeightUpdater(valueAnimator, from, 0);
		} else {
			float curWeight = ((LinearLayout.LayoutParams) view.getLayoutParams()).weight;
			applyWeightUpdater(valueAnimator, curWeight, 0);
		}

		animator.start();
	}

	private Animator createAnimator(int animatorId, Runnable onStart, Runnable onEnd) {
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
                view.getLayoutParams().height = (Integer) animation.getAnimatedValue();
				view.requestLayout();
			}
		});
	}

	private void applyWeightUpdater(ValueAnimator animator, float from, float to) {
		animator.setValues(PropertyValuesHolder.ofFloat((String) null, from, to));
		animator.addUpdateListener(new AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
                ((LinearLayout.LayoutParams) view.getLayoutParams()).weight = (Float) animation
                        .getAnimatedValue();
				view.requestLayout();
			}
		});
	}

	private void attachRunnable(Animator animator, final Runnable onStart, final Runnable onEnd) {
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
