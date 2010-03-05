package org.jvnet.jax_ws_commons.json.schema;

import java.util.Set;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class JsonType {
    /**
     * Number primitive type.
     *
     * I needed to make primitive types public to keep Velocity happy. Argh.
     */
    public static final JsonType NUMBER = new NumberType();

    /**
     * Boolean primitive type.
     */
    public static final JsonType BOOLEAN = new BooleanType();

    /**
     * String primitive type.
     */
    public static final JsonType STRING = new StringType();

    /**
     * Possibly generates a hyperlink to this type.
     */
    public abstract String getLink();

    public final JsonType makeArray() {
        return new ArrayJsonType(this);
    }

    /**
     * If this object type is the composite type that only has one property, returns its type.
     */
    public JsonType unwrap() {
        return this;
    }

    /**
     * List up all the {@link CompositeJsonType}s reachable from this type.
     */
    public void listCompositeTypes(Set<CompositeJsonType> result) {
        // noop
    }

    public static class NumberType extends JsonType {
        public String getLink() {
            return "NUMBER";
        }
    }

    public static class BooleanType extends JsonType {
        public String getLink() {
                return "BOOLEAN";
            }
    }

    public static class StringType extends JsonType {
        public String getLink() {
                return "STRING";
            }
    }
}
