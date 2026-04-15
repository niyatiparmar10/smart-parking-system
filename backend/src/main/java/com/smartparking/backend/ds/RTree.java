package com.smartparking.backend.ds;

import com.smartparking.backend.model.ParkingSlot;
import java.util.*;

/**
 * R-Tree for radius-based spatial search.
 * 
 * Each node stores a bounding box (MBR - Minimum Bounding Rectangle).
 * Leaf nodes hold actual parking slots.
 * Internal nodes hold children whose MBRs are contained within the parent's MBR.
 *
 * Used for: "Find all slots within X meters of this location"
 * Different from KD-Tree which finds K nearest points.
 */
public class RTree {

    private static final int MAX_ENTRIES = 4; // max slots per leaf node

    // --- Bounding Box (MBR) ---
    private static class BoundingBox {
        double minLat, maxLat, minLng, maxLng;

        BoundingBox(double minLat, double maxLat, double minLng, double maxLng) {
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.minLng = minLng;
            this.maxLng = maxLng;
        }

        // Create a bounding box from a single point
        static BoundingBox fromPoint(double lat, double lng) {
            return new BoundingBox(lat, lat, lng, lng);
        }

        // Expand this box to also contain another box
        BoundingBox expand(BoundingBox other) {
            return new BoundingBox(
                Math.min(this.minLat, other.minLat),
                Math.max(this.maxLat, other.maxLat),
                Math.min(this.minLng, other.minLng),
                Math.max(this.maxLng, other.maxLng)
            );
        }

        // Check if a circle (lat, lng, radiusDeg) overlaps this box
        boolean overlapsCircle(double lat, double lng, double radiusDeg) {
            // Find the closest point in this box to the circle center
            double closestLat = Math.max(minLat, Math.min(lat, maxLat));
            double closestLng = Math.max(minLng, Math.min(lng, maxLng));

            double dLat = lat - closestLat;
            double dLng = lng - closestLng;
            double dist = Math.sqrt(dLat * dLat + dLng * dLng);

            return dist <= radiusDeg;
        }
    }

    // --- Tree Node ---
    private static class RNode {
        BoundingBox box;
        List<ParkingSlot> slots;   // only used if leaf
        List<RNode> children;      // only used if internal
        boolean isLeaf;

        // Leaf node
        RNode() {
            this.isLeaf = true;
            this.slots = new ArrayList<>();
        }

        // Internal node
        RNode(List<RNode> children) {
            this.isLeaf = false;
            this.children = children;
            this.box = computeMBR(children);
        }

        static BoundingBox computeMBR(List<RNode> nodes) {
            BoundingBox mbr = nodes.get(0).box;
            for (int i = 1; i < nodes.size(); i++) {
                mbr = mbr.expand(nodes.get(i).box);
            }
            return mbr;
        }
    }

    private RNode root;

    // --- Build tree from list of slots ---
    public void build(List<ParkingSlot> slots) {
        root = buildRecursive(new ArrayList<>(slots));
    }

    private RNode buildRecursive(List<ParkingSlot> slots) {
        // Base case: small enough to be a leaf
        if (slots.size() <= MAX_ENTRIES) {
            RNode leaf = new RNode();
            for (ParkingSlot s : slots) {
                leaf.slots.add(s);
            }
            // Compute bounding box for this leaf
            double minLat = slots.stream().mapToDouble(s -> s.lat).min().getAsDouble();
            double maxLat = slots.stream().mapToDouble(s -> s.lat).max().getAsDouble();
            double minLng = slots.stream().mapToDouble(s -> s.lng).min().getAsDouble();
            double maxLng = slots.stream().mapToDouble(s -> s.lng).max().getAsDouble();
            leaf.box = new BoundingBox(minLat, maxLat, minLng, maxLng);
            return leaf;
        }

        // Split by whichever axis has more spread (lat or lng)
        double latSpread = slots.stream().mapToDouble(s -> s.lat).max().getAsDouble()
                         - slots.stream().mapToDouble(s -> s.lat).min().getAsDouble();
        double lngSpread = slots.stream().mapToDouble(s -> s.lng).max().getAsDouble()
                         - slots.stream().mapToDouble(s -> s.lng).min().getAsDouble();

        if (latSpread >= lngSpread) {
            slots.sort(Comparator.comparingDouble(s -> s.lat));
        } else {
            slots.sort(Comparator.comparingDouble(s -> s.lng));
        }

        // Split into two halves and recurse
        int mid = slots.size() / 2;
        RNode leftChild  = buildRecursive(new ArrayList<>(slots.subList(0, mid)));
        RNode rightChild = buildRecursive(new ArrayList<>(slots.subList(mid, slots.size())));

        return new RNode(Arrays.asList(leftChild, rightChild));
    }

    // --- Find all slots within radiusMeters of (lat, lng) ---
    public List<ParkingSlot> findWithinRadius(double lat, double lng, double radiusMeters) {
        double radiusDeg = radiusMeters / 111000.0; // convert meters to degrees
        List<ParkingSlot> result = new ArrayList<>();
        searchRadius(root, lat, lng, radiusDeg, result);
        return result;
    }

    private void searchRadius(RNode node, double lat, double lng,
                               double radiusDeg, List<ParkingSlot> result) {
        if (node == null) return;

        // Prune: if this node's box doesn't overlap the circle, skip entire subtree
        if (!node.box.overlapsCircle(lat, lng, radiusDeg)) return;

        if (node.isLeaf) {
            // Check each slot in this leaf
            for (ParkingSlot slot : node.slots) {
                double dLat = slot.lat - lat;
                double dLng = slot.lng - lng;
                double dist = Math.sqrt(dLat * dLat + dLng * dLng);
                if (dist <= radiusDeg) {
                    result.add(slot);
                }
            }
        } else {
            // Recurse into children
            for (RNode child : node.children) {
                searchRadius(child, lat, lng, radiusDeg, result);
            }
        }
    }
}