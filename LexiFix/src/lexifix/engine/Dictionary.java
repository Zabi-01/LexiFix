package lexifix.engine;

import lexifix.model.WordEntry;

import java.io.*;
import java.util.*;

/**
 * Dictionary - Manages word loading, insertion, deletion
 * Bridges the Trie and SpellChecker engines
 *
 * OOP Concepts: Encapsulation, Composition (has-a Trie, has-a SpellChecker)
 * DATA STRUCTURE: Trie (autocomplete) + Hash Table (spell check) + ArrayList (history)
 */
public class Dictionary {

    // OOP: Composition - Dictionary HAS-A Trie and HAS-A SpellChecker
    private Trie trie;
    private SpellChecker spellChecker;

    // DATA STRUCTURE: Linear - ArrayList for search history (LIFO-ish display)
    private LinkedList<String> searchHistory;

    // DATA STRUCTURE: Linear - ArrayList for recently added words
    private ArrayList<String> recentWords;

    private static final int MAX_HISTORY = 50;
    private String dictionaryFilePath;

    public Dictionary() {
        this.trie = new Trie();
        this.spellChecker = new SpellChecker();
        this.searchHistory = new LinkedList<>();
        this.recentWords = new ArrayList<>();
    }

    /**
     * Load dictionary from a .txt file (one word per line)
     * Inserts into both Trie and HashMap
     */
    public int loadFromFile(String filePath) throws IOException {
        this.dictionaryFilePath = filePath;
        int count = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String word = line.trim().toLowerCase();
                if (!word.isEmpty() && word.matches("[a-z]+")) {
                    trie.insert(word);
                    spellChecker.loadWord(word);
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * Add a word manually to dictionary
     * OOP: public interface hiding internal complexity
     */
    public void addWord(String word) {
        word = word.toLowerCase().trim();
        if (word.isEmpty() || !word.matches("[a-z]+")) return;

        trie.insert(word);
        spellChecker.loadWord(word);
        recentWords.add(0, word); // add to front of list
        if (recentWords.size() > 100) recentWords.remove(recentWords.size() - 1);
    }

    /**
     * Get autocomplete suggestions from Trie
     */
    public List<WordEntry> getSuggestions(String prefix, int maxResults) {
        // Record search history
        addToHistory(prefix);
        return trie.getSuggestions(prefix, maxResults);
    }

    /**
     * Get spelling corrections from SpellChecker
     */
    public List<WordEntry> getCorrections(String word, int maxResults) {
        return spellChecker.getCorrections(word, maxResults);
    }

    /**
     * Check if word is correctly spelled
     */
    public boolean isCorrect(String word) {
        return spellChecker.isCorrect(word);
    }

    /**
     * Called when user selects a word - boosts its frequency
     */
    public void wordSelected(String word) {
        trie.incrementFrequency(word);
        spellChecker.incrementFrequency(word);
        addToHistory(word);
    }

    /**
     * Add to search history (LinkedList - supports efficient front insertion)
     */
    private void addToHistory(String term) {
        if (term == null || term.isEmpty()) return;
        searchHistory.remove(term); // remove duplicate
        searchHistory.addFirst(term);
        if (searchHistory.size() > MAX_HISTORY) {
            searchHistory.removeLast();
        }
    }

    public LinkedList<String> getSearchHistory() {
        return searchHistory;
    }

    public ArrayList<String> getRecentWords() {
        return recentWords;
    }

    public int getDictionarySize() {
        return trie.getWordCount();
    }

    public Trie getTrie() {
        return trie;
    }

    public SpellChecker getSpellChecker() {
        return spellChecker;
    }

    public String getDictionaryFilePath() {
        return dictionaryFilePath;
    }
}
