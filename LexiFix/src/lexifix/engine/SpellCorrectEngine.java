package lexifix.engine;

import lexifix.model.WordEntry;
import java.util.List;
import java.util.ArrayList;

/**
 * SpellCorrectEngine - Uses Edit Distance for correction suggestions
 * OOP Concept: Inheritance (extends SuggestionEngine), Polymorphism (overrides suggest)
 */
public class SpellCorrectEngine extends SuggestionEngine {

    public SpellCorrectEngine(Dictionary dictionary, int maxResults) {
        super(dictionary, maxResults);
    }

    /**
     * OOP: Polymorphism - Override suggest() with spell-correction strategy
     * Uses Levenshtein Edit Distance
     */
    @Override
    public List<WordEntry> suggest(String input) {
        String normalized = normalize(input);
        if (normalized.isEmpty()) return new ArrayList<>();

        // If word is correct, no corrections
        if (!needsCorrection(normalized)) return new ArrayList<>();

        return dictionary.getCorrections(normalized, maxResults);
    }

    /**
     * OOP: Polymorphism - different check than AutocompleteEngine
     */
    @Override
    public boolean needsCorrection(String word) {
        return !dictionary.isCorrect(normalize(word));
    }
}
