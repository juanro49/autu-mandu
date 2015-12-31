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
package me.kuehle.carreport.gui.util;

import android.os.Handler;
import android.support.v7.widget.RecyclerView;

import com.github.clans.fab.FloatingActionMenu;

public class FloatingActionButtonRevealer {
    private static final int SCROLL_OFFSET = 4;

    public static void setup(final FloatingActionMenu fab, final RecyclerView list) {
        showDelayed(fab);
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
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

    private static void showDelayed(final FloatingActionMenu fab) {
        fab.hideMenuButton(false);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                fab.showMenuButton(true);
            }
        }, 300);
    }
}
