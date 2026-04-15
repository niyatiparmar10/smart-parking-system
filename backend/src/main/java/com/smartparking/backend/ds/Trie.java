package com.smartparking.backend.ds;

import java.util.*;

/**
 * Trie for zone name autocomplete.
 *
 * Stores zone names character by character in a tree.
 * When a user types "Ko", the Trie instantly returns:
 *   → ["Koregaon Park"]
 * When they type "Ba", it returns:
 *   → ["Baner"]
 *
 * Each node represents one character.
 * Each path from root to a marked node spells out a complete zone name.
 *
 * Visual example for "Camp" and "Camp Road":
 *
 * root
 *  └── C
 *       └── a
 *            └── m
 *                 └── p (isEnd=true) "Camp"
 *                      └── (space)
 *                            └── R
 *                                 └── o
 *                                      └── a
 *                                           └── d (isEnd=true) "Camp Road"
 */
public class Trie {

    // --- One node in the Trie ---
    private static class TrieNode {
        Map<Character, TrieNode> children;
        boolean isEnd;          // true if a complete zone name ends here
        String fullName;        // store full name at end node for easy retrieval

        TrieNode() {
            this.children = new HashMap<>();
            this.isEnd = false;
            this.fullName = null;
        }
    }

    private TrieNode root;

    public Trie() {
        this.root = new TrieNode();
    }

    // --- Insert a zone name into the Trie ---
    public void insert(String zoneName) {
        TrieNode current = root;
        String lower = zoneName.toLowerCase(); // case-insensitive

        for (char c : lower.toCharArray()) {
            current.children.putIfAbsent(c, new TrieNode());
            current = current.children.get(c);
        }

        current.isEnd = true;
        current.fullName = zoneName; // store original casing
    }

    // --- Search: return all zone names that start with the given prefix ---
    public List<String> autocomplete(String prefix) {
        List<String> results = new ArrayList<>();
        TrieNode current = root;
        String lower = prefix.toLowerCase();

        // Walk down the trie following the prefix
        for (char c : lower.toCharArray()) {
            if (!current.children.containsKey(c)) {
                return results; // prefix not found, return empty
            }
            current = current.children.get(c);
        }

        // From this node, collect all complete zone names below
        collectAllNames(current, results);
        return results;
    }

    // --- Recursively collect all zone names from a given node downward ---
    private void collectAllNames(TrieNode node, List<String> results) {
        if (node.isEnd) {
            results.add(node.fullName);
        }
        for (TrieNode child : node.children.values()) {
            collectAllNames(child, results);
        }
    }

    // --- Check if an exact zone name exists ---
    public boolean exists(String zoneName) {
        TrieNode current = root;
        String lower = zoneName.toLowerCase();

        for (char c : lower.toCharArray()) {
            if (!current.children.containsKey(c)) return false;
            current = current.children.get(c);
        }

        return current.isEnd;
    }

    // --- Delete a zone name (rarely needed but good to have) ---
    public void delete(String zoneName) {
        delete(root, zoneName.toLowerCase(), 0);
    }

    private boolean delete(TrieNode node, String word, int depth) {
        if (node == null) return false;

        if (depth == word.length()) {
            if (!node.isEnd) return false;
            node.isEnd = false;
            node.fullName = null;
            return node.children.isEmpty(); // true = safe to delete this node
        }

        char c = word.charAt(depth);
        TrieNode child = node.children.get(c);
        boolean shouldDelete = delete(child, word, depth + 1);

        if (shouldDelete) {
            node.children.remove(c);
            return !node.isEnd && node.children.isEmpty();
        }

        return false;
    }

    // --- Get all zone names in the trie (for initializing frontend dropdown) ---
    public List<String> getAllZones() {
        List<String> all = new ArrayList<>();
        collectAllNames(root, all);
        Collections.sort(all);
        return all;
    }
}
// ```

// ---

// **What this code does, simply:**

// Every character of a zone name becomes a node. The path through nodes spells the word:
// ```
// Inserting "Aundh":
// root → 'a' → 'u' → 'n' → 'd' → 'h' (isEnd = true, fullName = "Aundh")

// Inserting "Baner":
// root → 'b' → 'a' → 'n' → 'e' → 'r' (isEnd = true, fullName = "Baner")
