package io.axual.ksml.configs;

import java.util.TreeMap;

public class DefinitionDetails extends TreeMap<String, String> {
    public DefinitionDetails add(String key, String value) {
        put(key, value);
        return this;
    }
}
