package org.jvnet.jax_ws_commons.json.schema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * JavaScript object.
 *
 * @author Kohsuke Kawaguchi
 */
public class CompositeJsonType extends JsonType {
    private final String name;
    public final Map<String,JsonType> properties = new LinkedHashMap<String,JsonType>();

    public CompositeJsonType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getLink() {
        return String.format("<a href=#%1$s>%1$s</a>",name);
    }

    @Override
    public JsonType unwrap() {
        if(properties.size()!=1)
            return this;
        else
            return properties.values().iterator().next();
    }

    @Override
    public void listCompositeTypes(Set<CompositeJsonType> result) {
        if(result.add(this))
            for (JsonType t : properties.values())
                t.listCompositeTypes(result);
    }

    public Map<String, JsonType> getProperties() {
        return properties;
    }
}
