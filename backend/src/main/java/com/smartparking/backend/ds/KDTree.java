package com.smartparking.backend.ds;

import com.smartparking.backend.model.ParkingSlot;
import java.util.*;

/**
 * KD-Tree for 2D spatial search.
 * Each node splits space alternately by latitude (depth 0,2,4...) 
 * and longitude (depth 1,3,5...)
 * This lets us find the K nearest parking slots efficiently.
 */
public class KDTree {

    // --- Inner Node class ---
    private static class Node {
        ParkingSlot slot;
        Node left, right;

        Node(ParkingSlot slot) {
            this.slot = slot;
        }
    }

    private Node root;

    // --- Build the tree from a list of slots ---
    public void build(List<ParkingSlot> slots) {
        root = buildRecursive(new ArrayList<>(slots), 0);
    }

    private Node buildRecursive(List<ParkingSlot> slots, int depth) {
        if (slots.isEmpty()) return null;

        // Even depth → split by latitude, Odd depth → split by longitude
        int axis = depth % 2;

        // Sort by the current axis
        if (axis == 0) {
            slots.sort(Comparator.comparingDouble(s -> s.lat));
        } else {
            slots.sort(Comparator.comparingDouble(s -> s.lng));
        }

        // Median becomes the node
        int mid = slots.size() / 2;
        Node node = new Node(slots.get(mid));

        // Recurse left and right
        node.left  = buildRecursive(new ArrayList<>(slots.subList(0, mid)), depth + 1);
        node.right = buildRecursive(new ArrayList<>(slots.subList(mid + 1, slots.size())), depth + 1);

        return node;
    }

    // --- Find K nearest slots to a given (lat, lng) ---
    public List<ParkingSlot> findKNearest(double lat, double lng, int k) {
        // Max-heap: keeps the K closest (furthest at top so we can evict it)
        PriorityQueue<ParkingSlot> heap = new PriorityQueue<>(
            (a, b) -> Double.compare(distance(b, lat, lng), distance(a, lat, lng))
        );

        searchKNearest(root, lat, lng, k, heap, 0);

        List<ParkingSlot> result = new ArrayList<>(heap);
        result.sort(Comparator.comparingDouble(s -> distance(s, lat, lng)));
        return result;
    }

    private void searchKNearest(Node node, double lat, double lng, int k,
                                 PriorityQueue<ParkingSlot> heap, int depth) {
        if (node == null) return;

        double dist = distance(node.slot, lat, lng);

        // Add to heap if we have room, or if this node is closer than the farthest in heap
        if (heap.size() < k) {
            heap.add(node.slot);
        } else if (dist < distance(heap.peek(), lat, lng)) {
            heap.poll();
            heap.add(node.slot);
        }

        // Decide which side to search first (the side the query point is on)
        int axis = depth % 2;
        double axisDiff = (axis == 0) ? (lat - node.slot.lat) : (lng - node.slot.lng);

        Node first  = axisDiff < 0 ? node.left : node.right;
        Node second = axisDiff < 0 ? node.right : node.left;

        searchKNearest(first, lat, lng, k, heap, depth + 1);

        // Only search the other side if it could contain a closer point
        if (heap.size() < k || Math.abs(axisDiff) < distance(heap.peek(), lat, lng)) {
            searchKNearest(second, lat, lng, k, heap, depth + 1);
        }
    }

    // --- Euclidean distance (good enough for nearby coords) ---
    private double distance(ParkingSlot slot, double lat, double lng) {
        double dLat = slot.lat - lat;
        double dLng = slot.lng - lng;
        return Math.sqrt(dLat * dLat + dLng * dLng);
    }

    /**
     * Convert the raw degree-distance to approximate meters.
     * 1 degree lat ≈ 111,000 meters
     */
    public static double toMeters(double degreeDist) {
        return degreeDist * 111000;
    }
}