package lexifix.engine;

import lexifix.model.WordEntry;

import java.util.*;

/**
 * BigramGraph - Next-word (phrase) suggestion using Graph + HashMap
 *
 * DATA STRUCTURE: Graph (directed weighted) implemented as
 *                 HashMap<String, HashMap<String, Integer>>
 *
 *   Graph nodes  = words
 *   Graph edges  = "word A is followed by word B"
 *   Edge weight  = how many times this pair has occurred
 *
 * Example graph after loading phrases:
 *   "data"       → { "structures": 5, "science": 3, "types": 2 }
 *   "binary"     → { "search": 4, "tree": 6 }
 *   "linked"     → { "list": 8 }
 *
 * When user types "data str..." → autocomplete gives "structures"
 * Then next-word suggestion shows: "science", "types" (what usually follows "data")
 *
 * OOP: Encapsulation of graph structure
 *      Sorting applied to edge weights for ranked suggestions
 */
public class BigramGraph {

    // DATA STRUCTURE: Graph as Adjacency Map
    // word → { nextWord → frequency }
    private HashMap<String, HashMap<String, Integer>> graph;

    // Total bigram pairs loaded
    private int totalBigrams;

    public BigramGraph() {
        this.graph = new HashMap<>();
        this.totalBigrams = 0;
    }

    /**
     * Add a bigram pair (word1 → word2) to the graph
     * Called when loading text or when user selects a word after another
     *
     * Graph edge insertion: O(1) average
     */
    public void addBigram(String word1, String word2) {
        if (word1 == null || word2 == null) return;
        word1 = word1.toLowerCase().trim();
        word2 = word2.toLowerCase().trim();
        if (word1.isEmpty() || word2.isEmpty()) return;
        if (!word1.matches("[a-z]+") || !word2.matches("[a-z]+")) return;

        // Get or create adjacency map for word1
        graph.putIfAbsent(word1, new HashMap<>());
        HashMap<String, Integer> neighbors = graph.get(word1);

        // Increment edge weight
        neighbors.put(word2, neighbors.getOrDefault(word2, 0) + 1);
        totalBigrams++;
    }

    /**
     * Learn bigrams from a sentence or phrase
     * Splits into words and records consecutive pairs
     */
    public void learnFromText(String text) {
        if (text == null || text.trim().isEmpty()) return;
        String[] words = text.toLowerCase().trim().split("\\s+");

        for (int i = 0; i < words.length - 1; i++) {
            String w1 = words[i].replaceAll("[^a-z]", "");
            String w2 = words[i + 1].replaceAll("[^a-z]", "");
            if (!w1.isEmpty() && !w2.isEmpty()) {
                addBigram(w1, w2);
            }
        }
    }

    /**
     * Get next-word suggestions for a given word
     * Returns top suggestions ranked by frequency
     *
     * Graph traversal: O(degree of node) = O(neighbors)
     * Sorting: O(N log N)
     */
    public List<WordEntry> getNextWords(String word, int maxResults) {
        List<WordEntry> results = new ArrayList<>();
        if (word == null || word.isEmpty()) return results;

        word = word.toLowerCase().trim();
        HashMap<String, Integer> neighbors = graph.get(word);

        if (neighbors == null || neighbors.isEmpty()) return results;

        // Convert to WordEntry list for sorting
        for (Map.Entry<String, Integer> entry : neighbors.entrySet()) {
            results.add(new WordEntry(entry.getKey(), entry.getValue()));
        }

        // SORTING: Sort by frequency descending
        Collections.sort(results);

        return results.subList(0, Math.min(maxResults, results.size()));
    }

    /**
     * Get phrase suggestions: given word + partial next word
     * e.g. word="data", partial="str" → filters next-words starting with "str"
     */
    public List<WordEntry> getPhraseSuggestions(String word, String partial, int maxResults) {
        List<WordEntry> all = getNextWords(word, 50);
        List<WordEntry> filtered = new ArrayList<>();

        for (WordEntry e : all) {
            if (partial.isEmpty() || e.getWord().startsWith(partial.toLowerCase())) {
                filtered.add(e);
            }
        }

        return filtered.subList(0, Math.min(maxResults, filtered.size()));
    }

    /**
     * Called when user types a space — learn the last two words as a bigram
     */
    public void learnFromUserInput(String previousWord, String currentWord) {
        addBigram(previousWord, currentWord);
    }

    /**
     * Load default common phrase patterns for CS/technical context
     */
    public void loadDefaultPhrases() {
        String[] phrases = {
            "data structures algorithms",
            "data types include",
            "data structure used",
            "binary search tree",
            "binary tree traversal",
            "linked list implementation",
            "linked list node",
            "hash table lookup",
            "hash map contains",
            "stack overflow error",
            "stack push pop",
            "queue data structure",
            "queue first last",
            "object oriented programming",
            "object class method",
            "public static void",
            "void main method",
            "return type value",
            "array list size",
            "array index length",
            "sort algorithm complexity",
            "sort array elements",
            "search binary linear",
            "search result found",
            "insert delete update",
            "time complexity space",
            "input output stream",
            "file read write",
            "class object instance",
            "method parameter return",
            "private public protected",
            "inheritance polymorphism encapsulation",
            "abstract class interface",
            "for loop iteration",
            "while condition true",
            "if else condition",
            "null pointer exception",
            "memory allocation size",
            "recursive function call",
            "depth first search",
            "breadth first search",
            "graph node edge",
            "tree root leaf",
            "prefix search trie",
            "spell check correction",
            "word frequency count",
            "user input text",
            "system design pattern",
            "software development process",
            "program execution output",
        };

        for (String phrase : phrases) {
            learnFromText(phrase);
        }
    }

    public boolean hasWord(String word) {
        return graph.containsKey(word.toLowerCase().trim());
    }

    public int getTotalBigrams() { return totalBigrams; }
    public int getVocabularySize() { return graph.size(); }

    // For visualization
    public HashMap<String, HashMap<String, Integer>> getGraph() { return graph; }
}
