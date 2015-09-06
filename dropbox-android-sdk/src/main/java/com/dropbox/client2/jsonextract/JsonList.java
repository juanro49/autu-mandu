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
package com.dropbox.client2.jsonextract;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * (Internal class for extracting JSON.)
 *
 * A JSON "array" (a list of arbitrary JSON values).
 */
public final class JsonList extends JsonBase<List<Object>> implements Iterable<JsonThing> {

    public JsonList(List<Object> internal, String path) {
        super(internal, path);
    }

    public JsonList(List<Object> internal) {
        super(internal);
    }

    private static String pathConcatIndex(String path, int index) {
        return JsonThing.pathConcat(path, Integer.toString(index));
    }

    public JsonThing get(int index) throws JsonExtractionException {
        if (index >= internal.size()) {
            throw error("expecting array to have an element at index " + index + ", but it only has length " + internal.size());
        }
        return new JsonThing(internal.get(index), pathConcatIndex(path, index));
    }

    public void expectLength(int length) throws JsonExtractionException {
        if (internal.size() != length) {
            throw error("expecting array to have length " + length + ", but it has length " + internal.size());
        }
    }

    public int length() {
        return internal.size();
    }

    private static final class WrapperIterator implements Iterator<JsonThing> {
        private int numReturned = 0;
        private final String path;
        private final Iterator<Object> internal;
        private WrapperIterator(String path, Iterator<Object> internal) {
            this.path = path;
            this.internal = internal;
        }

        public boolean hasNext() { return internal.hasNext(); }
        public void remove() { throw new UnsupportedOperationException("can't remove"); }

        public JsonThing next() {
            int index = numReturned++;
            return new JsonThing(internal.next(), pathConcatIndex(path, index));
        }
    }

    public Iterator<JsonThing> iterator() {
        return new WrapperIterator(path, internal.iterator());
    }

    public static final class Extractor<T> extends JsonExtractor<List<T>> {
        public final JsonExtractor<T> elementExtractor;
        public Extractor(JsonExtractor<T> elementExtractor) {
            this.elementExtractor = elementExtractor;
        }

        public List<T> extract(JsonThing jt) throws JsonExtractionException {
            return jt.expectList().extract(this.elementExtractor);
        }
    }

    public <T> ArrayList<T> extract(JsonExtractor<T> elementExtractor) throws JsonExtractionException {
        ArrayList<T> result = new ArrayList<T>(length());
        for (Object o : internal) {
            result.add(elementExtractor.extract(new JsonThing(o)));
        }
        return result;
    }
}
