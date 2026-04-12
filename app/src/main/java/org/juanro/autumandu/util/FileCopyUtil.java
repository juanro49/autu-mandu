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

package org.juanro.autumandu.util;

import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility class for copying files and streams.
 */
public final class FileCopyUtil {

    private FileCopyUtil() {
        // Utility class
    }

    public static boolean copyFile(@NonNull File from, @NonNull File to) {
        try (var inStream = new FileInputStream(from);
             var outStream = new FileOutputStream(to)) {
            return copyFile(inStream, outStream);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean copyFile(@NonNull File from, @NonNull ParcelFileDescriptor to) {
        try (var inStream = new FileInputStream(from);
             var outStream = new FileOutputStream(to.getFileDescriptor())) {
            return copyFile(inStream, outStream);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean copyFile(@NonNull ParcelFileDescriptor from, @NonNull ParcelFileDescriptor to) {
        try (var inStream = new FileInputStream(from.getFileDescriptor());
             var outStream = new FileOutputStream(to.getFileDescriptor())) {
            return copyFile(inStream, outStream);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean copyFile(@NonNull InputStream from, @NonNull OutputStream to) {
        try {
            // transferTo is available from API 33+, but for compatibility with API 25
            // we use a robust manual transfer if needed.
            // In Java 21/Android modern contexts, this is highly efficient.
            byte[] buffer = new byte[8192];
            int len;
            while ((len = from.read(buffer)) > 0) {
                to.write(buffer, 0, len);
            }
            to.flush();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
