package lexifix.model;

import java.util.Map;
import java.util.TreeMap;

/**
 * TrieNode - Represents a single node in the Trie (Prefix Tree)
 * DATA STRUCTURE: Non-Linear - Trie Node
 *
 * FIX: Children stored in TreeMap instead of HashMap so DFS visits
 * characters in sorted (alphabetical) order, giving consistent
 * alphabetical tie-breaking when two words have equal frequency.
 */
public class TrieNode {

    private Map<Character, TrieNode> children;
    private boolean isEndOfWord;
    private int frequency;

    public TrieNode() {
        this.children = new TreeMap<>();  // TreeMap: sorted key order
        this.isEndOfWord = false;
        this.frequency = 0;
    }

    public Map<Character, TrieNode> getChildren() { return children; }

    public boolean isEndOfWord() { return isEndOfWord; }
    public void setEndOfWord(boolean endOfWord) { isEndOfWord = endOfWord; }

    public int getFrequency() { return frequency; }
    public void incrementFrequency() { this.frequency++; }
    public void setFrequency(int frequency) { this.frequency = frequency; }

    public boolean hasChild(char c) { return children.containsKey(c); }
    public TrieNode getChild(char c) { return children.get(c); }
    public void addChild(char c, TrieNode node) { children.put(c, node); }
}
