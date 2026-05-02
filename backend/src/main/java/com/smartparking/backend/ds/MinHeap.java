package com.smartparking.backend.ds;

import com.smartparking.backend.model.ParkingSlot;
import java.util.*;

public class MinHeap {

    // --- One entry in the heap: a slot + its computed score ---
    public static class HeapEntry {
        public ParkingSlot slot;
        public double score;
        public double distanceMeters;

        public HeapEntry(ParkingSlot slot, double score, double distanceMeters) {
            this.slot = slot;
            this.score = score;
            this.distanceMeters = distanceMeters;
        }
    }

    private List<HeapEntry> heap;

    public MinHeap() {
        this.heap = new ArrayList<>();
    }

    // --- Insert a slot with its score ---
    public void insert(HeapEntry entry) {
        heap.add(entry);
        bubbleUp(heap.size() - 1);
    }

    // --- Remove and return the best slot (lowest score) ---
    public HeapEntry extractMin() {
        if (heap.isEmpty()) return null;

        HeapEntry min = heap.get(0);

        // Move last element to root and bubble down
        HeapEntry last = heap.remove(heap.size() - 1);
        if (!heap.isEmpty()) {
            heap.set(0, last);
            bubbleDown(0);
        }

        return min;
    }

    // --- Peek at best slot without removing ---
    public HeapEntry peek() {
        return heap.isEmpty() ? null : heap.get(0);
    }

    public boolean isEmpty() {
        return heap.isEmpty();
    }

    public int size() {
        return heap.size();
    }

    // --- Move a node UP until heap property is restored ---
    private void bubbleUp(int i) {
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (heap.get(parent).score > heap.get(i).score) {
                swap(i, parent);
                i = parent;
            } else {
                break;
            }
        }
    }

    // --- Move a node DOWN until heap property is restored ---
    private void bubbleDown(int i) {
        int size = heap.size();
        while (true) {
            int left  = 2 * i + 1;
            int right = 2 * i + 2;
            int smallest = i;

            if (left < size && heap.get(left).score < heap.get(smallest).score) {
                smallest = left;
            }
            if (right < size && heap.get(right).score < heap.get(smallest).score) {
                smallest = right;
            }

            if (smallest != i) {
                swap(i, smallest);
                i = smallest;
            } else {
                break;
            }
        }
    }

    private void swap(int i, int j) {
        HeapEntry temp = heap.get(i);
        heap.set(i, heap.get(j));
        heap.set(j, temp);
    }

    // --- Get all entries sorted best to worst (non-destructive) ---
    public List<HeapEntry> getAllSorted() {
        List<HeapEntry> copy = new ArrayList<>(heap);
        copy.sort(Comparator.comparingDouble(e -> e.score));
        return copy;
    }

    // -------------------------------------------------------------------
    // SCORING LOGIC
    // -------------------------------------------------------------------

    /**
     * Compute a priority score for a slot.
     * Lower score = better slot.
     *
     * @param distanceMeters  how far the slot is from the driver
     * @param maxDistance     max distance among all candidates (for normalization)
     * @param slot            the parking slot
     * @param preferredType   what vehicle type the driver has
     * @param isCongested     whether this slot's zone is congested
     */
    public static double computeScore(double distanceMeters, double maxDistance,
                                       ParkingSlot slot, String preferredType,
                                       boolean isCongested) {
        double normalizedDist = (maxDistance > 0) ? distanceMeters / maxDistance : 0;
        double normalizedPrice = slot.pricePerHour / 50.0;
        double typePenalty = slot.type.equals(preferredType) ? 0.0 : 0.3;

        // Distance is most important — 80% weight
        return (0.8 * normalizedDist)
             + (0.1 * normalizedPrice)
             + (0.1 * typePenalty);
    }
}

