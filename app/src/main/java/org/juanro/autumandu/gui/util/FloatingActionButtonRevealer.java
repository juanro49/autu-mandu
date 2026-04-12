/*
 * Copyright 2015 Jan Kühle
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

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.clans.fab.FloatingActionMenu;

/**
 * Utility class to show/hide a FloatingActionMenu based on RecyclerView scrolling.
 */
public class FloatingActionButtonRevealer {
    private static final int SCROLL_OFFSET = 4;
    private static final int SHOW_DELAY_MS = 300;

    /**
     * Sets up the FloatingActionMenu to hide/show based on the scroll direction of the RecyclerView.
     *
     * @param fab  The FloatingActionMenu to reveal/hide.
     * @param list The RecyclerView whose scroll events will trigger the reveal/hide.
     */
    public static void setup(@NonNull final FloatingActionMenu fab, @NonNull final RecyclerView list) {
        showDelayed(fab);
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (Math.abs(dy) > SCROLL_OFFSET) {
                    if (dy > 0) {
                        fab.hideMenuButton(true);
                    } else {
                        fab.showMenuButton(true);
                    }
                }
            }
        });
    }

    /**
     * Shows the FAB with a slight delay, useful for initial screen transitions.
     *
     * @param fab The FloatingActionMenu to show.
     */
    private static void showDelayed(@NonNull final FloatingActionMenu fab) {
        fab.hideMenuButton(false);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (fab.isAttachedToWindow()) {
                fab.showMenuButton(true);
            }
        }, SHOW_DELAY_MS);
    }
}
