package lexifix.engine;

import lexifix.model.WordEntry;
import java.util.List;
import java.util.ArrayList;

/**
 * AutocompleteEngine - Uses Trie for prefix-based suggestions
 * OOP Concept: Inheritance (extends SuggestionEngine), Polymorphism (overrides suggest)
 */
public class AutocompleteEngine extends SuggestionEngine {

    public AutocompleteEngine(Dictionary dictionary, int maxResults) {
        super(dictionary, maxResults);
    }

    /**
     * OOP: Polymorphism - Override suggest() with autocomplete strategy
     * Uses Trie prefix search O(L + N)
     */
    @Override
    public List<WordEntry> suggest(String input) {
        String normalized = normalize(input);
        if (normalized.isEmpty()) return new ArrayList<>();
        return dictionary.getSuggestions(normalized, maxResults);
    }

    /**
     * Autocomplete never "corrects" - it only extends
     */
    @Override
    public boolean needsCorrection(String word) {
        return false;
    }
}
