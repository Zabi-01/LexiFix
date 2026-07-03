package lexifix.engine;

import lexifix.model.TrieNode;
import lexifix.model.WordEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Trie - Prefix Tree implementation for fast autocomplete
 * DATA STRUCTURE: Non-Linear - Trie (Prefix Tree)
 *
 * Time Complexity:
 *   Insert:  O(L) where L = word length
 *   Search:  O(L)
 *   Prefix:  O(L + N) where N = number of results
 *
 * OOP Concepts: Encapsulation, Recursion
 */
public class Trie {
    private TrieNode root;
    private int wordCount;

    public Trie() {
        this.root = new TrieNode();
        this.wordCount = 0;
    }

    /**
     * Insert a word into the Trie
     * O(L) time complexity
     */
    public void insert(String word) {
        if (word == null || word.isEmpty()) return;
        word = word.toLowerCase().trim();

        TrieNode current = root;
        for (char c : word.toCharArray()) {
            if (!current.hasChild(c)) {
                current.addChild(c, new TrieNode());
            }
            current = current.getChild(c);
        }

        if (!current.isEndOfWord()) {
            current.setEndOfWord(true);
            wordCount++;
            current.incrementFrequency(); // only count frequency on first insertion
        }
    }

    /**
     * Search for exact word in Trie
     * O(L) time complexity
     */
    public boolean search(String word) {
        if (word == null || word.isEmpty()) return false;
        word = word.toLowerCase().trim();

        TrieNode current = root;
        for (char c : word.toCharArray()) {
            if (!current.hasChild(c)) return false;
            current = current.getChild(c);
        }
        return current.isEndOfWord();
    }

    /**
     * Get all autocomplete suggestions for a given prefix
     * Uses DFS traversal of Trie
     * O(L + N) time complexity
     */
    public List<WordEntry> getSuggestions(String prefix, int maxResults) {
        List<WordEntry> results = new ArrayList<>();
        if (prefix == null || prefix.isEmpty()) return results;

        prefix = prefix.toLowerCase().trim();

        // Navigate to prefix node
        TrieNode current = root;
        for (char c : prefix.toCharArray()) {
            if (!current.hasChild(c)) return results; // prefix not found
            current = current.getChild(c);
        }

        // DFS from prefix node to collect all words
        collectWords(current, new StringBuilder(prefix), results);

        // SORTING: Sort by frequency (descending) then alphabetically
        Collections.sort(results);

        // Return top maxResults
        return results.subList(0, Math.min(maxResults, results.size()));
    }

    /**
     * DFS helper to collect all words from a given node
     * Recursive method - OOP + Algorithm concept
     */
    private void collectWords(TrieNode node, StringBuilder current, List<WordEntry> results) {
        if (node.isEndOfWord()) {
            results.add(new WordEntry(current.toString(), node.getFrequency()));
        }

        for (char c : node.getChildren().keySet()) {
            current.append(c);
            collectWords(node.getChild(c), current, results);
            current.deleteCharAt(current.length() - 1); // backtrack
        }
    }

    /**
     * Increment frequency of a word (when user selects it)
     */
    public void incrementFrequency(String word) {
        if (word == null || word.isEmpty()) return;
        word = word.toLowerCase().trim();

        TrieNode current = root;
        for (char c : word.toCharArray()) {
            if (!current.hasChild(c)) return;
            current = current.getChild(c);
        }
        if (current.isEndOfWord()) {
            current.incrementFrequency();
        }
    }

    /**
     * Get frequency of a word
     */
    public int getFrequency(String word) {
        if (word == null || word.isEmpty()) return 0;
        word = word.toLowerCase().trim();

        TrieNode current = root;
        for (char c : word.toCharArray()) {
            if (!current.hasChild(c)) return 0;
            current = current.getChild(c);
        }
        return current.isEndOfWord() ? current.getFrequency() : 0;
    }

    public int getWordCount() {
        return wordCount;
    }

    public TrieNode getRoot() {
        return root;
    }
}
