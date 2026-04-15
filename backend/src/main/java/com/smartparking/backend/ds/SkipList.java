package com.smartparking.backend.ds;

import java.util.*;

/**
 * Skip List for sorted booking interval search.
 *
 * A Skip List is a probabilistic data structure that works like a
 * sorted linked list but with multiple "express lanes" on top.
 * Each higher level skips over more elements, giving O(log n) search.
 *
 * Visual idea:
 * Level 3: [head] -----------------------> [480] ---------> [null]
 * Level 2: [head] ---------> [360] ------> [480] -> [720] -> [null]
 * Level 1: [head] -> [240] -> [360] ------> [480] -> [600] -> [720] -> [null]
 * Level 0: [head] -> [240] -> [360] -> [420] -> [480] -> [600] -> [720] -> [null]
 *
 * We use it to store booking start times per slot,
 * so we can quickly find "is there any booking starting near time T?"
 * This is a secondary check after the Interval Tree.
 */
public class SkipList {

    private static final int MAX_LEVEL = 4;   // maximum number of levels
    private static final double PROBABILITY = 0.5; // chance of promoting to next level

    // --- One node in the skip list ---
    private static class SkipNode {
        int startTime;   // booking start time (minutes from midnight)
        int endTime;     // booking end time
        String slotId;   // which slot this booking belongs to
        SkipNode[] next; // pointers to next node at each level

        SkipNode(int startTime, int endTime, String slotId, int level) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.slotId = slotId;
            this.next = new SkipNode[level + 1];
        }
    }

    private SkipNode head;    // sentinel head node (startTime = -infinity)
    private int currentLevel; // current highest level in use
    private Random random;

    public SkipList() {
        // Head node has minimum possible value so everything is inserted after it
        this.head = new SkipNode(Integer.MIN_VALUE, Integer.MIN_VALUE, "HEAD", MAX_LEVEL);
        this.currentLevel = 0;
        this.random = new Random();
    }

    // --- Randomly decide how many levels this node gets ---
    // This is the probabilistic part that gives Skip List its O(log n) average
    private int randomLevel() {
        int level = 0;
        while (random.nextDouble() < PROBABILITY && level < MAX_LEVEL) {
            level++;
        }
        return level;
    }

    // --- Insert a booking into the skip list ---
    public void insert(int startTime, int endTime, String slotId) {
        // update[i] = the rightmost node at level i that is to the left of insertion point
        SkipNode[] update = new SkipNode[MAX_LEVEL + 1];
        SkipNode current = head;

        // Find insertion position at each level (top to bottom)
        for (int i = currentLevel; i >= 0; i--) {
            while (current.next[i] != null && current.next[i].startTime < startTime) {
                current = current.next[i];
            }
            update[i] = current;
        }

        int newLevel = randomLevel();

        // If new node has more levels than current tree, update tracking
        if (newLevel > currentLevel) {
            for (int i = currentLevel + 1; i <= newLevel; i++) {
                update[i] = head;
            }
            currentLevel = newLevel;
        }

        // Create the new node and splice it in at all levels
        SkipNode newNode = new SkipNode(startTime, endTime, slotId, newLevel);
        for (int i = 0; i <= newLevel; i++) {
            newNode.next[i] = update[i].next[i];
            update[i].next[i] = newNode;
        }
    }

    // --- Search: find all bookings that overlap with [reqStart, reqEnd] ---
    public List<String> findConflictingSlots(int reqStart, int reqEnd) {
        List<String> conflicting = new ArrayList<>();
        SkipNode current = head.next[0]; // start at level 0 (full list)

        // Use top level to skip ahead to roughly reqStart
        SkipNode fastCurrent = head;
        for (int i = currentLevel; i >= 0; i--) {
            while (fastCurrent.next[i] != null
                   && fastCurrent.next[i].startTime < reqStart - 1) {
                fastCurrent = fastCurrent.next[i];
            }
        }
        current = fastCurrent.next[0];

        // From here, scan forward at level 0 checking overlaps
        // Stop when startTime >= reqEnd (no more possible overlaps)
        while (current != null && current.startTime < reqEnd) {
            // Overlap condition: start1 < end2 AND start2 < end1
            if (current.startTime < reqEnd && reqStart < current.endTime) {
                conflicting.add(current.slotId);
            }
            current = current.next[0];
        }

        return conflicting;
    }

    // --- Search: does a specific slot have any conflict? ---
    public boolean hasConflict(String slotId, int reqStart, int reqEnd) {
        List<String> conflicts = findConflictingSlots(reqStart, reqEnd);
        return conflicts.contains(slotId);
    }

    // --- Delete a booking (for cancellations) ---
    public void delete(int startTime, String slotId) {
        SkipNode[] update = new SkipNode[MAX_LEVEL + 1];
        SkipNode current = head;

        for (int i = currentLevel; i >= 0; i--) {
            while (current.next[i] != null
                   && (current.next[i].startTime < startTime
                       || (current.next[i].startTime == startTime
                           && !current.next[i].slotId.equals(slotId)))) {
                current = current.next[i];
            }
            update[i] = current;
        }

        current = current.next[0];

        // Only delete if we found the right node
        if (current != null && current.startTime == startTime
                && current.slotId.equals(slotId)) {
            for (int i = 0; i <= currentLevel; i++) {
                if (update[i].next[i] != current) break;
                update[i].next[i] = current.next[i];
            }
        }
    }

    // --- Print all bookings (for debugging / console logs) ---
    public void printAll() {
        System.out.println("SkipList bookings (level 0):");
        SkipNode current = head.next[0];
        while (current != null) {
            System.out.println("  Slot " + current.slotId
                + " | " + IntervalTree.toTimeString(current.startTime)
                + " - " + IntervalTree.toTimeString(current.endTime));
            current = current.next[0];
        }
    }
}
// ```

// ---

// **What this code does, simply:**

// Think of a normal sorted linked list — searching it is O(n) because you scan from left to right. A Skip List adds express lanes on top:

// - **Level 0** — every node (full list, slow but complete)
// - **Level 1** — roughly every 2nd node
// - **Level 2** — roughly every 4th node
// - **Level 3+** — fewer and fewer nodes

// When searching, you start at the top level, skip large chunks, drop down a level when you overshoot, and repeat. Average O(log n) just like a balanced tree, but much simpler to implement.

// **The probabilistic part:** When inserting, we flip a coin (50% chance) to decide if a node gets promoted to the next level. This randomness is what keeps the list balanced on average without complex rotation logic like AVL trees need.

// **How it works with IntervalTree in your pipeline:**
// ```
// IntervalTree  → per-slot conflict check  (precise, per slot)
// SkipList      → global cross-slot check  (fast scan across all bookings)