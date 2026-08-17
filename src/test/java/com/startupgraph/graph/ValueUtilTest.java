package com.startupgraph.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;

class ValueUtilTest {

    @Test
    void normalizesNull() {
        assertNull(ValueUtil.normalize(Values.NULL));
        assertNull(ValueUtil.normalize(null));
    }

    @Test
    void normalizesPrimitives() {
        assertEquals(42L, ValueUtil.normalize(Values.value(42L)));
        assertEquals("Stripe", ValueUtil.normalize(Values.value("Stripe")));
        assertEquals(true, ValueUtil.normalize(Values.value(true)));
        assertEquals(1.5, ValueUtil.normalize(Values.value(1.5)));
    }

    @Test
    void normalizesListAndMap() {
        Value value = Values.value(List.of(Map.of("name", "Stripe", "foundedYear", 2010L)));
        Object normalized = ValueUtil.normalize(value);
        assertEquals(List.of(Map.of("name", "Stripe", "foundedYear", 2010L)), normalized);
    }
}
