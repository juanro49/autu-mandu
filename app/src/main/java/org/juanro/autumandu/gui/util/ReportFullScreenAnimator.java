/*
 * Copyright 2026 Juanro49
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

package org.juanro.autumandu.gui.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import com.google.android.material.appbar.AppBarLayout;

/**
 * Helper class to handle full screen chart animations.
 */
public class ReportFullScreenAnimator {
    private final AppBarLayout appBarLayout;
    private final FrameLayout fullScreenChart;
    private final View fullScreenChartHolder;
    private Animator animator;
    private View originView;
    private Rect startBounds;
    private float startScaleX;
    private float startScaleY;

    public ReportFullScreenAnimator(AppBarLayout appBarLayout, FrameLayout fullScreenChart, View fullScreenChartHolder) {
        this.appBarLayout = appBarLayout;
        this.fullScreenChart = fullScreenChart;
        this.fullScreenChartHolder = fullScreenChartHolder;
    }

    public void show(View v, View rootView, View kubitView, int animationTime) {
        if (animator != null) {
            animator.cancel();
        }

        originView = v;
        fullScreenChart.removeAllViews();
        fullScreenChart.addView(kubitView);

        startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        originView.getGlobalVisibleRect(startBounds);
        rootView.getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        startScaleX = (float) startBounds.width() / finalBounds.width();
        startScaleY = (float) startBounds.height() / finalBounds.height();

        originView.setVisibility(View.INVISIBLE);
        fullScreenChartHolder.setVisibility(View.VISIBLE);

        fullScreenChartHolder.setPivotX(0f);
        fullScreenChartHolder.setPivotY(0f);

        AnimatorSet set = new AnimatorSet();
        set.play(ObjectAnimator.ofFloat(fullScreenChartHolder, View.X,
                        startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(fullScreenChartHolder, View.Y,
                        startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(fullScreenChartHolder, View.SCALE_X,
                        startScaleX, 1f))
                .with(ObjectAnimator.ofFloat(fullScreenChartHolder, View.SCALE_Y,
                        startScaleY, 1f));
        set.setDuration(animationTime);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animator = null;
                appBarLayout.setVisibility(View.INVISIBLE);
            }
        });
        set.start();
        animator = set;
    }

    public boolean isVisible() {
        return originView != null;
    }

    public void hide(int animationTime) {
        if (animator != null) {
            animator.cancel();
        }

        AnimatorSet set = new AnimatorSet();
        set.play(ObjectAnimator.ofFloat(fullScreenChartHolder, View.X,
                        startBounds.left))
                .with(ObjectAnimator.ofFloat(fullScreenChartHolder, View.Y,
                        startBounds.top))
                .with(ObjectAnimator.ofFloat(fullScreenChartHolder, View.SCALE_X,
                        startScaleX))
                .with(ObjectAnimator.ofFloat(fullScreenChartHolder, View.SCALE_Y,
                        startScaleY));
        set.setDuration(animationTime);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                appBarLayout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                originView.setVisibility(View.VISIBLE);
                originView = null;
                fullScreenChartHolder.setVisibility(View.GONE);
                animator = null;
                fullScreenChart.removeAllViews();
            }
        });
        set.start();
        animator = set;
    }
}
