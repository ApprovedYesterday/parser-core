package com.tracker.infrastructure.scraping;

import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistentHashRing<T> {

    private final SortedMap<Integer, T> ring = new TreeMap<>();
    private final int virtualNodes;

    public ConsistentHashRing(int virtualNodes) {
        this.virtualNodes = virtualNodes;
    }

    public void addNode(T node) {
        for (int i = 0; i < virtualNodes; i++) {
            ring.put(hash(node.toString() + ":" + i), node);
        }
    }

    public T getNode(String key) {
        if (ring.isEmpty()) return null;
        int hash = hash(key);
        SortedMap<Integer, T> tailMap = ring.tailMap(hash);
        Integer nodeHash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        return ring.get(nodeHash);
    }

    private int hash(String key) {
        return key.hashCode() & 0x7fffffff;
    }
}
