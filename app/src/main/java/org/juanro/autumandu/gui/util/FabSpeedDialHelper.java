/*
 * Copyright 2026 Juanro49
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.juanro.autumandu.gui.util;

import android.view.View;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.juanro.autumandu.R;

/**
 * Helper class to manage the Speed Dial FAB behavior using official Material Components.
 */
public class FabSpeedDialHelper {
    private final FloatingActionButton mainFab;
    private final View miniContainer;
    private final View overlay;
    private boolean isExpanded = false;

    public FabSpeedDialHelper(@NonNull View container) {
        this.mainFab = container.findViewById(R.id.fab_main);
        this.miniContainer = container.findViewById(R.id.fab_mini_container);
        this.overlay = container.findViewById(R.id.fab_overlay);

        setupListeners();
    }

    private void setupListeners() {
        mainFab.setOnClickListener(v -> toggle());
        if (overlay != null) {
            overlay.setOnClickListener(v -> close());
        }

        // Mini FAB Listeners
        View refueling = miniContainer.findViewById(R.id.fab_add_refueling);
        if (refueling != null) {
            refueling.setOnClickListener(v -> {
                if (mainFab.getContext() instanceof org.juanro.autumandu.gui.MainActivity activity) {
                    activity.onFABAddRefuelingClicked(v);
                }
            });
        }

        View otherExpenditure = miniContainer.findViewById(R.id.fab_add_other_expenditure);
        if (otherExpenditure != null) {
            otherExpenditure.setOnClickListener(v -> {
                if (mainFab.getContext() instanceof org.juanro.autumandu.gui.MainActivity activity) {
                    activity.onFABAddOtherExpenditureClicked(v);
                }
            });
        }

        View otherIncome = miniContainer.findViewById(R.id.fab_add_other_income);
        if (otherIncome != null) {
            otherIncome.setOnClickListener(v -> {
                if (mainFab.getContext() instanceof org.juanro.autumandu.gui.MainActivity activity) {
                    activity.onFABAddOtherIncomeClicked(v);
                }
            });
        }

        View tires = miniContainer.findViewById(R.id.fab_add_tires);
        if (tires != null) {
            tires.setOnClickListener(v -> {
                if (mainFab.getContext() instanceof org.juanro.autumandu.gui.MainActivity activity) {
                    activity.onFABAddTiresClicked(v);
                }
            });
        }
    }

    public void toggle() {
        if (isExpanded) {
            close();
        } else {
            expand();
        }
    }

    public void expand() {
        if (isExpanded()) return;
        isExpanded = true;

        // Update content description for accessibility
        mainFab.setContentDescription(mainFab.getContext().getString(R.string.fab_collapse));

        // Rotate main FAB
        mainFab.animate()
                .rotation(45f)
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator())
                .start();

        // Show overlay
        if (overlay != null) {
            overlay.setVisibility(View.VISIBLE);
            overlay.setAlpha(0f);
            overlay.animate().alpha(1f).setDuration(300).start();
        }

        // Show mini FABs
        miniContainer.setVisibility(View.VISIBLE);
        miniContainer.setAlpha(0f);
        miniContainer.setTranslationY(miniContainer.getHeight() / 4f);
        miniContainer.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }

    public void close() {
        if (miniContainer == null || miniContainer.getVisibility() != View.VISIBLE) {
            isExpanded = false;
            return;
        }
        isExpanded = false;

        // Update content description for accessibility
        mainFab.setContentDescription(mainFab.getContext().getString(R.string.fab_expand));

        // Rotate main FAB back
        mainFab.animate()
                .rotation(0f)
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator())
                .start();

        // Hide overlay
        if (overlay != null) {
            overlay.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> overlay.setVisibility(View.GONE))
                    .start();
        }

        // Hide mini FABs
        miniContainer.animate()
                .alpha(0f)
                .translationY(miniContainer.getHeight() / 4f)
                .setDuration(300)
                .withEndAction(() -> miniContainer.setVisibility(View.GONE))
                .start();
    }

    public boolean isExpanded() {
        return isExpanded || (miniContainer != null && miniContainer.getVisibility() == View.VISIBLE);
    }

    /**
     * Shows or hides the entire FAB container (e.g., when scrolling).
     */
    public void setVisible(boolean visible) {
        if (visible) {
            mainFab.show();
        } else {
            if (isExpanded) {
                close();
            }
            mainFab.hide();
        }
    }
}
