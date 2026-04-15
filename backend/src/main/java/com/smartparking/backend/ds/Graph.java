package com.smartparking.backend.ds;

import java.util.*;

/**
 * Graph of Pune zones with Dijkstra's algorithm.
 *
 * Zones are nodes. Edges represent adjacency between zones
 * with weights = approximate road distance in kilometers.
 *
 * Used for: Congestion-aware zone routing.
 * When the nearest zone is >80% full, Dijkstra finds the
 * closest non-congested zone to redirect the driver to.
 *
 * Zone indices:
 *  0 = Koregaon Park
 *  1 = Shivajinagar
 *  2 = FC Road
 *  3 = Kothrud
 *  4 = Hadapsar
 *  5 = Viman Nagar
 *  6 = Aundh
 *  7 = Baner
 *  8 = Camp
 *  9 = Deccan
 */
public class Graph {

    private int numZones;
    private Map<String, Integer> zoneToIndex;
    private Map<Integer, String> indexToZone;

    // Adjacency list: each zone → list of (neighborIndex, distance)
    private List<List<int[]>> adjList;

    public Graph() {
        zoneToIndex = new HashMap<>();
        indexToZone = new HashMap<>();
        adjList = new ArrayList<>();

        // Register all zones
        String[] zones = {
            "Koregaon Park", "Shivajinagar", "FC Road", "Kothrud",
            "Hadapsar", "Viman Nagar", "Aundh", "Baner", "Camp", "Deccan"
        };

        numZones = zones.length;
        for (int i = 0; i < numZones; i++) {
            zoneToIndex.put(zones[i], i);
            indexToZone.put(i, zones[i]);
            adjList.add(new ArrayList<>());
        }

        // Build edges (bidirectional, weight = approx km between zones)
        // Based on real Pune geography
        addEdge("Koregaon Park", "Viman Nagar",   3);
        addEdge("Koregaon Park", "Camp",           4);
        addEdge("Koregaon Park", "Hadapsar",       6);
        addEdge("Koregaon Park", "Shivajinagar",   5);
        addEdge("Shivajinagar", "FC Road",         2);
        addEdge("Shivajinagar", "Deccan",          2);
        addEdge("Shivajinagar", "Camp",            3);
        addEdge("FC Road",      "Deccan",          1);
        addEdge("FC Road",      "Kothrud",         4);
        addEdge("Kothrud",      "Deccan",          4);
        addEdge("Kothrud",      "Baner",           5);
        addEdge("Kothrud",      "Aundh",           6);
        addEdge("Aundh",        "Baner",           3);
        addEdge("Aundh",        "Shivajinagar",    5);
        addEdge("Baner",        "Shivajinagar",    7);
        addEdge("Camp",         "Hadapsar",        5);
        addEdge("Camp",         "Deccan",          3);
        addEdge("Viman Nagar",  "Hadapsar",        5);
        addEdge("Viman Nagar",  "Camp",            4);
        addEdge("Deccan",       "Aundh",           5);
    }

    private void addEdge(String zone1, String zone2, int distKm) {
        int i = zoneToIndex.get(zone1);
        int j = zoneToIndex.get(zone2);
        adjList.get(i).add(new int[]{j, distKm});
        adjList.get(j).add(new int[]{i, distKm});
    }

    /**
     * Dijkstra's algorithm.
     * Finds shortest distance from startZone to ALL other zones.
     * Returns map of zoneName → distance from start.
     */
    public Map<String, Integer> dijkstra(String startZone) {
        int start = zoneToIndex.getOrDefault(startZone, -1);
        if (start == -1) return new HashMap<>();

        // dist[i] = shortest known distance from start to zone i
        int[] dist = new int[numZones];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[start] = 0;

        // Min-heap: [distance, zoneIndex]
        PriorityQueue<int[]> pq = new PriorityQueue<>(
            Comparator.comparingInt(a -> a[0])
        );
        pq.offer(new int[]{0, start});

        while (!pq.isEmpty()) {
            int[] current = pq.poll();
            int currentDist = current[0];
            int currentZone = current[1];

            // Skip if we already found a shorter path
            if (currentDist > dist[currentZone]) continue;

            // Relax all neighbors
            for (int[] neighbor : adjList.get(currentZone)) {
                int neighborZone = neighbor[0];
                int edgeWeight   = neighbor[1];
                int newDist = dist[currentZone] + edgeWeight;

                if (newDist < dist[neighborZone]) {
                    dist[neighborZone] = newDist;
                    pq.offer(new int[]{newDist, neighborZone});
                }
            }
        }

        // Convert result array to readable map
        Map<String, Integer> result = new HashMap<>();
        for (int i = 0; i < numZones; i++) {
            if (dist[i] != Integer.MAX_VALUE) {
                result.put(indexToZone.get(i), dist[i]);
            }
        }
        return result;
    }

    /**
     * Find the nearest non-congested zone to the driver's current zone.
     * congested = set of zone names that are >80% full.
     *
     * Returns the best alternative zone name.
     * Returns null if all zones are congested (edge case).
     */
    public String findNearestAvailableZone(String currentZone,
                                            Set<String> congestedZones) {
        Map<String, Integer> distances = dijkstra(currentZone);

        return distances.entrySet().stream()
            .filter(e -> !congestedZones.contains(e.getKey()))
            .filter(e -> !e.getKey().equals(currentZone))
            .min(Comparator.comparingInt(Map.Entry::getValue))
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    /**
     * Get distance in km between two zones.
     */
    public int getDistance(String from, String to) {
        Map<String, Integer> distances = dijkstra(from);
        return distances.getOrDefault(to, Integer.MAX_VALUE);
    }

    // --- Print the graph (for debugging) ---
    public void printGraph() {
        System.out.println("Zone Graph:");
        for (int i = 0; i < numZones; i++) {
            System.out.print(indexToZone.get(i) + " → ");
            for (int[] neighbor : adjList.get(i)) {
                System.out.print(indexToZone.get(neighbor[0])
                    + "(" + neighbor[1] + "km) ");
            }
            System.out.println();
        }
    }
}
// ```

// ---

// **What this code does, simply:**

// The 10 Pune zones are nodes. Edges are real approximate road distances between them. It looks like this:
// ```
// Koregaon Park ──3km── Viman Nagar
//       │                    │
//      4km                  4km
//       │                    │
//     Camp ────4km──── Viman Nagar
//       │
//      3km
//       │
//    Deccan ──1km── FC Road ──2km── Shivajinagar
//                       │
//                      4km
//                       │
//                    Kothrud ──5km── Baner ──3km── Aundh
// ```

// **Dijkstra step by step** (what happens internally):
// ```
// Start: Koregaon Park (dist = 0)
// Heap: [(0, KP)]

// Pop (0, KP) → relax neighbors:
//   Viman Nagar → 3, Camp → 4, Hadapsar → 6, Shivajinagar → 5
// Heap: [(3,VN), (4,Camp), (5,Shiv), (6,Had)]

// Pop (3, VN) → relax its neighbors...
// ...and so on until all zones have shortest distance
// ```

// **`findNearestAvailableZone()`** is the key method your service calls:
// - Takes the driver's current zone + set of congested zones
// - Runs Dijkstra from current zone
// - Filters out congested ones
// - Returns the closest non-congested zone

// **Viva point — why Dijkstra here:**
// > "We use Dijkstra because edges have different weights — road distances between zones vary. BFS would only work if all edges were equal weight. Dijkstra guarantees the shortest weighted path."

// ---

// **All 8 DS classes are now complete.** Here is the full checklist:
// ```
// ✅ KDTree.java
// ✅ RTree.java
// ✅ SegmentTree.java
// ✅ IntervalTree.java
// ✅ SkipList.java
// ✅ MinHeap.java
// ✅ Trie.java
// ✅ Graph.java