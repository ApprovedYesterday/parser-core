package com.tracker.infrastructure.scraping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ConsistentHashRingTest {

    @Test
    void shouldReturnNullWhenEmpty() {
        var ring = new ConsistentHashRing<String>(100);
        assertNull(ring.getNode("any-key"));
    }

    @Test
    void shouldReturnSameNodeForSameKey() {
        var ring = new ConsistentHashRing<String>(100);
        ring.addNode("node-A");
        ring.addNode("node-B");
        ring.addNode("node-C");

        String first = ring.getNode("seller-123");
        String second = ring.getNode("seller-123");
        assertEquals(first, second);
    }

    @Test
    void shouldDistributeDifferentKeys() {
        var ring = new ConsistentHashRing<String>(100);
        ring.addNode("node-A");
        ring.addNode("node-B");

        String result1 = ring.getNode("key-1");
        String result2 = ring.getNode("key-2");
        assertNotNull(result1);
        assertNotNull(result2);
    }

    @Test
    void shouldReturnOnlyAddedNode() {
        var ring = new ConsistentHashRing<String>(10);
        ring.addNode("the-only-node");

        assertEquals("the-only-node", ring.getNode("anything"));
    }
}
