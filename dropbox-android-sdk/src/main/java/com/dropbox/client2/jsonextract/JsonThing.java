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

/**
 * (Internal class for extracting JSON.)
 *
 * <p>
 * A utility class to let you extract your required structure out of an
 * org.json.simple object.
 * </p>
 *
 * <p>
 * As you descend into the object and pull our your data, these classes keep
 * track of where you are, so if there's an error in the JSON value, you'll get
 * a "path" string describing exactly where the problem is.
 * </p>
 */
public final class JsonThing extends JsonBase<Object> {

    public JsonThing(Object internal, String path) {
        super(internal, path);
    }

    public JsonThing(Object internal) {
        super(internal, null);
    }

    private static final java.util.HashMap<Class<?>, String> TypeNames = new java.util.HashMap<Class<?>, String>();
    static {
        TypeNames.put(String.class, "a string");
        TypeNames.put(Number.class, "a number");
        TypeNames.put(Boolean.class, "a boolean");
        TypeNames.put(java.util.Map.class, "an object");
        TypeNames.put(java.util.List.class, "an array");
    }

    private static String typeNameForClass(Class<?> c) {
        if (c == null) return "null";
        String name = TypeNames.get(c);
        assert name != null;
        return name;
    }

    private static String typeNameForObject(Object o) {
        if (o == null) return "null";
        if (o instanceof Number) return "a number";
        if (o instanceof String) return "a string";
        if (o instanceof Boolean) return "a boolean";
        if (o instanceof java.util.Map) return "an object";
        if (o instanceof java.util.List) return "an array";
        throw new IllegalArgumentException("not a valid org.json.simple type: " + o.getClass().getName());
    }

    private boolean is(Class<?> type) {
        assert type != null;
        return type.isInstance(internal);
    }

    private <T> T expect(Class<T> type) throws JsonExtractionException {
        assert type != null;

        if (type.isInstance(internal)) {
            @SuppressWarnings("unchecked")
            T recast = (T) internal;
            return recast;
        }

        throw error("expecting " + typeNameForClass(type) + ", found " + typeNameForObject(internal));
    }

    public void expectNull() throws JsonExtractionException {
        if (internal != null) {
            throw error("expecting null");
        }
    }

    public boolean isNull() {
        return internal == null;
    }

    public JsonMap expectMap() throws JsonExtractionException {
        @SuppressWarnings("unchecked")
        java.util.Map<String,Object> mapInternal = expect(java.util.Map.class);
        return new JsonMap(mapInternal, path);
    }

    public boolean isMap() {
        return is(java.util.Map.class);
    }

    public JsonList expectList() throws JsonExtractionException {
        @SuppressWarnings("unchecked")
        java.util.List<Object> listInternal = expect(java.util.List.class);
        return new JsonList(listInternal, path);
    }

    public boolean isList() {
        return is(java.util.List.class);
    }

    public Number expectNumber() throws JsonExtractionException {
        return expect(Number.class);
    }

    public boolean isNumber() {
        return is(Number.class);
    }

    public long expectInt64() throws JsonExtractionException {
        if (internal instanceof Number) {
            Number number = (Number) internal;
            // TODO: Be robust, since JSON actually defines "number" to mean "IEEE double"
            // - Make sure there's no fractional part.
            // - Make sure there's no overflow.
            return number.longValue();
        }
        else {
            throw error("expecting an integer, found " + typeNameForObject(internal));
        }
    }

    public boolean isInt64() {
        try {
            expectInt64();
            return true;
        }
        catch (JsonExtractionException ex) {
            return false;
        }
    }

    public int expectInt32() throws JsonExtractionException {
        if (internal instanceof Number) {
            Number number = (Number) internal;
            // TODO: Be robust, since JSON actually defines "number" to mean "IEEE double"
            // - Make sure there's no fractional part.
            // - Make sure there's no overflow.
            return number.intValue();
        }
        else {
            throw error("expecting an integer, found " + typeNameForObject(internal));
        }
    }

    public boolean isInt32() {
        try {
            expectInt32();
            return true;
        }
        catch (JsonExtractionException ex) {
            return false;
        }
    }

    public double expectFloat64() throws JsonExtractionException {
        if (internal instanceof Number) {
            Number number = (Number) internal;
            return number.doubleValue();
        }
        else {
            throw error("expecting a floating point number, found " + typeNameForObject(internal));
        }
    }

    public boolean isFloat64() {
        try {
            expectFloat64();
            return true;
        }
        catch (JsonExtractionException ex) {
            return false;
        }
    }

    public String expectString() throws JsonExtractionException {
        return expect(String.class);
    }

    public String expectStringOrNull() throws JsonExtractionException {
        if (internal == null) return null;
        return expect(String.class);
    }

    public boolean isString() {
        return is(String.class);
    }

    public boolean expectBoolean() throws JsonExtractionException {
        return expect(Boolean.class);
    }

    public boolean isBoolean() {
        return is(Boolean.class);
    }

    static String pathConcat(String a, String b) {
        if (a == null) return b;
        return a + "/" + b;
    }

    public JsonExtractionException unexpected() {
        return error("unexpected type: " + typeNameForObject(internal));
    }

    public static final class OptionalExtractor<T> extends JsonExtractor<T> {
        public final JsonExtractor<T> elementExtractor;
        public OptionalExtractor(JsonExtractor<T> elementExtractor) {
            this.elementExtractor = elementExtractor;
        }

        @Override
        public T extract(JsonThing jt) throws JsonExtractionException {
            return jt.optionalExtract(this.elementExtractor);
        }
    }

    public <T> T optionalExtract(JsonExtractor<T> extractor) throws JsonExtractionException {
        if (isNull()) return null;
        return extractor.extract(this);
    }
}
