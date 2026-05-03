package com.smartparking.backend.ds;

import java.util.*;

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
            while (current.next[i] != null && current.next[i].endTime < endTime) {
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

    // --- Get expired bookings (O(1) time because they are at the front!) ---
    public List<String[]> getExpired(int currentMin) {
        List<String[]> expired = new ArrayList<>();
        SkipNode current = head.next[0];
        
        // Since list is sorted by endTime, all expired bookings are at the front
        while (current != null && current.endTime <= currentMin) {
            expired.add(new String[]{
                current.slotId, 
                String.valueOf(current.startTime), 
                String.valueOf(current.endTime)
            });
            current = current.next[0];
        }
        return expired;
    }

    // --- Delete a booking (for cancellations) ---
    public void delete(int endTime, String slotId) {
        SkipNode[] update = new SkipNode[MAX_LEVEL + 1];
        SkipNode current = head;

        for (int i = currentLevel; i >= 0; i--) {
            while (current.next[i] != null
                   && (current.next[i].endTime < endTime
                       || (current.next[i].endTime == endTime
                           && !current.next[i].slotId.equals(slotId)))) {
                current = current.next[i];
            }
            update[i] = current;
        }

        current = current.next[0];

        // Only delete if we found the right node
        if (current != null && current.endTime == endTime
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
