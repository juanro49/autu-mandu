/*
 * Copyright 2012 Jan Kühle
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

package me.kuehle.carreport.util.gui;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Transformation;
import android.widget.LinearLayout;

public class WeightAnimator {
	private View view;
	private float weight;
	private long duration;

	public WeightAnimator(View view, long duration) {
		this.view = view;
		this.weight = ((LinearLayout.LayoutParams) view.getLayoutParams()).weight;
		this.duration = duration;
	}

	public void expand(Runnable onStart, Runnable onEnd) {
		Animation anim = new Animation() {
			@Override
			protected void applyTransformation(float interpolatedTime,
					Transformation t) {
				float newWeight = weight * interpolatedTime;
				((LinearLayout.LayoutParams) view.getLayoutParams()).weight = newWeight;
				view.requestLayout();
			}

			@Override
			public boolean willChangeBounds() {
				return true;
			}
		};
		anim.setDuration(duration);
		WeightAnimator.attachRunnable(anim, onStart, onEnd);
		view.startAnimation(anim);
	}

	public void collapse(Runnable onStart, Runnable onEnd) {
		Animation anim = new Animation() {
			@Override
			protected void applyTransformation(float interpolatedTime,
					Transformation t) {
				float newWeight = weight * (1 - interpolatedTime);
				((LinearLayout.LayoutParams) view.getLayoutParams()).weight = newWeight;
				view.requestLayout();
			}

			@Override
			public boolean willChangeBounds() {
				return true;
			}
		};
		anim.setDuration(duration);
		WeightAnimator.attachRunnable(anim, onStart, onEnd);
		view.startAnimation(anim);
	}

	private static void attachRunnable(Animation anim, final Runnable onStart,
			final Runnable onEnd) {
		if (onStart != null || onEnd != null) {
			anim.setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {
					if (onStart != null) {
						onStart.run();
					}
				}

				@Override
				public void onAnimationRepeat(Animation animation) {
				}

				@Override
				public void onAnimationEnd(Animation animation) {
					if (onEnd != null) {
						onEnd.run();
					}
				}
			});
		}
	}
}
