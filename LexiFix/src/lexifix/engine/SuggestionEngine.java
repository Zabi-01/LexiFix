package lexifix.engine;

import lexifix.model.WordEntry;
import java.util.List;

/**
 * SuggestionEngine - Abstract base class for suggestion strategies
 * OOP Concept: Abstraction + Inheritance + Polymorphism
 *
 * Subclasses implement different suggestion strategies
 */
public abstract class SuggestionEngine {

    protected Dictionary dictionary;
    protected int maxResults;

    public SuggestionEngine(Dictionary dictionary, int maxResults) {
        this.dictionary = dictionary;
        this.maxResults = maxResults;
    }

    /**
     * Abstract method - POLYMORPHISM
     * Each subclass provides its own suggestion strategy
     */
    public abstract List<WordEntry> suggest(String input);

    /**
     * Abstract method - detect if input needs correction
     */
    public abstract boolean needsCorrection(String word);

    // Shared utility - OOP: code reuse via inheritance
    protected String normalize(String input) {
        if (input == null) return "";
        return input.toLowerCase().trim();
    }

    public int getMaxResults() { return maxResults; }
    public void setMaxResults(int maxResults) { this.maxResults = maxResults; }
}
