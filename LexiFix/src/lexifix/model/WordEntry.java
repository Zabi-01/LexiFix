package lexifix.model;

/**
 * WordEntry - Model class representing a word with its frequency/score
 * OOP Concept: Encapsulation + Comparable for sorting
 * DATA STRUCTURE: Used in arrays/lists for ranking
 */
public class WordEntry implements Comparable<WordEntry> {
    private String word;
    private int frequency;
    private double relevanceScore;

    public WordEntry(String word, int frequency) {
        this.word = word;
        this.frequency = frequency;
        this.relevanceScore = frequency;
    }

    public WordEntry(String word, int frequency, double relevanceScore) {
        this.word = word;
        this.frequency = frequency;
        this.relevanceScore = relevanceScore;
    }

    public String getWord() { return word; }
    public int getFrequency() { return frequency; }
    public double getRelevanceScore() { return relevanceScore; }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
        this.relevanceScore = frequency;
    }

    // OOP: Overriding compareTo for sorting by frequency (descending)
    @Override
    public int compareTo(WordEntry other) {
        return Double.compare(other.relevanceScore, this.relevanceScore);
    }

    @Override
    public String toString() {
        return word + " (freq: " + frequency + ")";
    }
}
