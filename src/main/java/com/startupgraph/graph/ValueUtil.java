package com.startupgraph.graph;

import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.neo4j.driver.types.TypeSystem;
import org.neo4j.driver.Value;

public final class ValueUtil {

    private ValueUtil() {
    }

    public static Object normalize(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }
        TypeSystem types = TypeSystem.getDefault();
        if (value.hasType(types.NODE())) {
            Node node = value.asNode();
            Map<String, Object> map = new LinkedHashMap<>(node.asMap());
            List<String> labels = new ArrayList<>();
            node.labels().forEach(labels::add);
            map.put("_labels", labels);
            return map;
        }
        if (value.hasType(types.RELATIONSHIP())) {
            Relationship rel = value.asRelationship();
            Map<String, Object> map = new LinkedHashMap<>(rel.asMap());
            map.put("_type", rel.type());
            return map;
        }
        if (value.hasType(types.MAP())) {
            Map<String, Object> map = new LinkedHashMap<>();
            value.keys().forEach(key -> map.put(key, normalize(value.get(key))));
            return map;
        }
        if (value.hasType(types.LIST())) {
            List<Object> list = new ArrayList<>();
            value.values().forEach(item -> list.add(normalize(item)));
            return list;
        }
        Object raw = value.asObject();
        if (raw instanceof TemporalAccessor) {
            return raw.toString();
        }
        return raw;
    }

    public static Map<String, Object> recordToMap(org.neo4j.driver.Record record) {
        Map<String, Object> map = new LinkedHashMap<>();
        record.keys().forEach(key -> map.put(key, normalize(record.get(key))));
        return map;
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return (List<Map<String, Object>>) (List<?>) list;
    }
}
