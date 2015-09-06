/*
 * Copyright (c) 2009-2014 Dropbox, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.dropbox.client2;

import java.io.Serializable;

/***
 * SerializableObject implementation that keeps track of compatible version changes.
 */
public abstract class VersionedSerializable implements Serializable {

    private final int instanceVersion;

    /**
     * @return Version number associated with this instance This data is serialized with the
     *         instance
     */
    public int getInstanceVersion() {
        return instanceVersion;
    }

    /**
     * @return The newest version of class' data format.
     */
    public abstract int getLatestVersion();

    /**
     * @return Whether some fields of this instance were not present in deserialized data If true,
     *         app should retetch data from server
     */
    public boolean isStale() {
        return instanceVersion < getLatestVersion();
    }

    public VersionedSerializable() {
        instanceVersion = getLatestVersion();
    }

}
