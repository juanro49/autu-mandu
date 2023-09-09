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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class FileCopyUtil {
    public static boolean copyFile(File from, File to) {
        try (FileInputStream inStream = new FileInputStream(from)) {
            try (FileOutputStream outStream = new FileOutputStream(to)) {
                return copyFile(inStream, outStream);
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean copyFile(File from, ParcelFileDescriptor to) {
        try (FileInputStream inStream = new FileInputStream(from)) {
            try (FileOutputStream outStream = new FileOutputStream(to.getFileDescriptor())) {
                return copyFile(inStream, outStream);
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean copyFile(ParcelFileDescriptor from, ParcelFileDescriptor to) {
        try (FileInputStream inStream = new FileInputStream(from.getFileDescriptor())) {
            try (FileOutputStream outStream = new FileOutputStream(to.getFileDescriptor())) {
                return copyFile(inStream, outStream);
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean copyFile(InputStream from, OutputStream to) {
        try {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = from.read(buffer)) > 0) {
                to.write(buffer, 0, len);
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
