/*
 * Copyright 2016 Jan Kühle
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
package org.juanro.autumandu.util.sync;

/**
 * Exception thrown when a network or I/O error occurs during sync.
 */
public class SyncIoException extends Exception {
    private static final String MESSAGE_PREFIX = "Network error: ";

    public SyncIoException(Throwable cause) {
        super(MESSAGE_PREFIX + cause.getMessage(), cause);
    }
}
