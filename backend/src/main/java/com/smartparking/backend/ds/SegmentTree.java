package com.smartparking.backend.ds;

import java.util.*;

/**
 * Segment Tree for zone-level availability tracking.
 *
 * Stores the count of FREE slots per zone.
 * Supports:
 *   - Query: how many free slots are in a zone?
 *   - Update: decrement when a slot is booked, increment when freed
 *   - Range query: total free slots across a range of zones
 *
 * The zones are indexed 0 to n-1.
 * The tree is stored as an array (classic segment tree style).
 */
public class SegmentTree {

    private int[] tree;      // the segment tree array
    private int[] freeCount; // free slot count per zone (source of truth)
    private int n;           // number of zones

    private List<String> zoneNames;               // index → zone name
    private Map<String, Integer> zoneIndex;       // zone name → index

    public SegmentTree(List<String> zones) {
        this.zoneNames = new ArrayList<>(zones);
        this.zoneIndex = new HashMap<>();
        this.n = zones.size();
        this.freeCount = new int[n];
        this.tree = new int[4 * n]; // standard segment tree size

        for (int i = 0; i < n; i++) {
            zoneIndex.put(zones.get(i), i);
        }
    }

    // --- Set initial free count for a zone ---
    public void setZoneCount(String zone, int count) {
        int idx = zoneIndex.get(zone);
        freeCount[idx] = count;
    }

    // --- Build the tree after all zones are initialized ---
    public void build() {
        build(1, 0, n - 1);
    }

    private void build(int node, int start, int end) {
        if (start == end) {
            tree[node] = freeCount[start];
        } else {
            int mid = (start + end) / 2;
            build(2 * node, start, mid);
            build(2 * node + 1, mid + 1, end);
            tree[node] = tree[2 * node] + tree[2 * node + 1];
        }
    }

    // --- Update: a slot in a zone was booked (delta = -1) or freed (delta = +1) ---
    public void update(String zone, int delta) {
        int idx = zoneIndex.getOrDefault(zone, -1);
        if (idx == -1) return;
        freeCount[idx] += delta;
        update(1, 0, n - 1, idx, freeCount[idx]);
    }

    private void update(int node, int start, int end, int idx, int value) {
        if (start == end) {
            tree[node] = value;
        } else {
            int mid = (start + end) / 2;
            if (idx <= mid) {
                update(2 * node, start, mid, idx, value);
            } else {
                update(2 * node + 1, mid + 1, end, idx, value);
            }
            tree[node] = tree[2 * node] + tree[2 * node + 1];
        }
    }

    // --- Query: free slots in a single zone ---
    public int getFreeCount(String zone) {
        int idx = zoneIndex.getOrDefault(zone, -1);
        if (idx == -1) return 0;
        return freeCount[idx];
    }

    // --- Query: total free slots across ALL zones (range query on full range) ---
    public int getTotalFree() {
        return tree[1]; // root of segment tree = sum of everything
    }

    // --- Congestion check: is this zone more than 80% full? ---
    public boolean isCongested(String zone, int totalSlotsInZone) {
        int free = getFreeCount(zone);
        int occupied = totalSlotsInZone - free;
        return (double) occupied / totalSlotsInZone > 0.8;
    }

    // --- Get all zone names sorted by free slots (most free first) ---
    // Used by Dijkstra to find best alternative zone
    public List<String> getZonesSortedByAvailability() {
        List<String> sorted = new ArrayList<>(zoneNames);
        sorted.sort((a, b) -> getFreeCount(b) - getFreeCount(a));
        return sorted;
    }

    // --- Debug: print all zone counts ---
    public void printZones() {
        for (String zone : zoneNames) {
            System.out.println(zone + " → " + getFreeCount(zone) + " free slots");
        }
    }
}
// ```

// ---

// **What this code does, simply:**

// The segment tree array stores sums. If you have 10 zones, the tree looks like this internally:
// ```
//           [total free: all zones]
//          /                       \
//   [zones 0-4 free]          [zones 5-9 free]
//    /          \               /           \
// [0-2]        [3-4]        [5-7]          [8-9]
//   ...          ...          ...            ...