package com.smartparking.backend.ds;

import java.util.*;

public class IntervalTree {

    // --- One time interval ---
    public static class Interval {
        int start, end;

        public Interval(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    // --- Tree Node ---
    private static class Node {
        Interval interval;
        int maxEnd;       // max end time in this subtree (the augmentation)
        Node left, right;

        Node(Interval interval) {
            this.interval = interval;
            this.maxEnd = interval.end;
        }
    }

    // One tree per slot: slotId → root node
    private Map<String, Node> slotTrees = new HashMap<>();

    // --- Insert a booking interval for a slot ---
    public void insert(String slotId, int start, int end) {
        Interval interval = new Interval(start, end);
        Node existing = slotTrees.getOrDefault(slotId, null);
        slotTrees.put(slotId, insert(existing, interval));
    }

    private Node insert(Node node, Interval interval) {
        if (node == null) return new Node(interval);

        // Insert like a BST, sorted by start time
        if (interval.start < node.interval.start) {
            node.left = insert(node.left, interval);
        } else {
            node.right = insert(node.right, interval);
        }

        // Update maxEnd going back up
        node.maxEnd = Math.max(node.maxEnd, interval.end);
        return node;
    }

    // --- Check if a slot has ANY conflict with [reqStart, reqEnd] ---
    public boolean hasConflict(String slotId, int reqStart, int reqEnd) {
        Node root = slotTrees.getOrDefault(slotId, null);
        return hasConflict(root, reqStart, reqEnd);
    }

    private boolean hasConflict(Node node, int reqStart, int reqEnd) {
        if (node == null) return false;

        // Key pruning: if max end in this subtree < reqStart, no conflict possible
        if (node.maxEnd < reqStart) return false;

        // Check current node: two intervals overlap if start1 < end2 AND start2 < end1
        if (node.interval.start < reqEnd && reqStart < node.interval.end) {
            return true;
        }

        // Search left subtree first (it might prune faster)
        if (node.left != null && node.left.maxEnd >= reqStart) {
            if (hasConflict(node.left, reqStart, reqEnd)) return true;
        }

        return hasConflict(node.right, reqStart, reqEnd);
    }

    // --- Get all conflicting intervals for a slot (useful for debugging) ---
    public List<Interval> getConflicts(String slotId, int reqStart, int reqEnd) {
        List<Interval> conflicts = new ArrayList<>();
        Node root = slotTrees.getOrDefault(slotId, null);
        collectConflicts(root, reqStart, reqEnd, conflicts);
        return conflicts;
    }

    private void collectConflicts(Node node, int reqStart, int reqEnd,
                                   List<Interval> result) {
        if (node == null) return;
        if (node.maxEnd < reqStart) return;

        if (node.interval.start < reqEnd && reqStart < node.interval.end) {
            result.add(node.interval);
        }

        if (node.left != null && node.left.maxEnd >= reqStart) {
            collectConflicts(node.left, reqStart, reqEnd, result);
        }
        collectConflicts(node.right, reqStart, reqEnd, result);
    }

    // --- Remove a booking (e.g. on cancellation) ---
    public void remove(String slotId, int start, int end) {
        Node root = slotTrees.getOrDefault(slotId, null);
        slotTrees.put(slotId, remove(root, start, end));
    }

    private Node remove(Node node, int start, int end) {
        if (node == null) return null;

        if (start < node.interval.start) {
            node.left = remove(node.left, start, end);
        } else if (start > node.interval.start) {
            node.right = remove(node.right, start, end);
        } else if (node.interval.end == end) {
            // Found it — remove by replacing with in-order successor
            if (node.right == null) return node.left;
            Node successor = findMin(node.right);
            node.interval = successor.interval;
            node.right = remove(node.right, successor.interval.start, successor.interval.end);
        } else {
            node.right = remove(node.right, start, end);
        }

        // Recalculate maxEnd
        node.maxEnd = node.interval.end;
        if (node.left != null)  node.maxEnd = Math.max(node.maxEnd, node.left.maxEnd);
        if (node.right != null) node.maxEnd = Math.max(node.maxEnd, node.right.maxEnd);

        return node;
    }

    private Node findMin(Node node) {
        while (node.left != null) node = node.left;
        return node;
    }

    // --- Helper: convert "HH:MM" string to minutes from midnight ---
    public static int toMinutes(String time) {
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    // --- Helper: convert minutes back to "HH:MM" for display ---
    public static String toTimeString(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        return String.format("%02d:%02d", h, m);
    }
}
