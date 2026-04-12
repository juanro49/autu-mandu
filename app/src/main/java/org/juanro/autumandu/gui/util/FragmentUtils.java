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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class for fragment-related operations.
 * Includes thread-safe fragment animation control.
 */
public class FragmentUtils {
    /**
     * Counter to disable fragment animations.
     * Use AtomicInteger to ensure thread safety when multiple fragments or threads
     * are manipulating the animation state.
     */
    private static final AtomicInteger disableFragmentAnimations = new AtomicInteger(0);

    /**
     * Increments the counter to disable animations.
     */
    public static void disableAnimations() {
        disableFragmentAnimations.incrementAndGet();
    }

    /**
     * Checks if animations should be disabled and decrements the counter if so.
     * @return true if animations were disabled and the counter was decremented.
     */
    public static boolean shouldDisableAndDecrement() {
        int current;
        do {
            current = disableFragmentAnimations.get();
            if (current <= 0) {
                return false;
            }
        } while (!disableFragmentAnimations.compareAndSet(current, current - 1));
        return true;
    }
}
