package lexifix.engine;

import lexifix.model.WordEntry;

import java.util.*;

/**
 * SpellChecker - Detects and corrects spelling mistakes
 * ALGORITHM: Edit Distance (Levenshtein Distance) - Dynamic Programming
 * DATA STRUCTURE: Hash Table for fast word lookup
 *
 * OOP Concepts: Encapsulation, Single Responsibility Principle
 */
public class SpellChecker {

    // DATA STRUCTURE: Hash Table - O(1) average lookup
    private HashMap<String, Integer> wordFrequencyMap;

    // Max edit distance threshold — 2 covers most single-word typos
    // (one wrong char, one missing char, one extra char, or two transpositions)
    private static final int MAX_EDIT_DISTANCE = 2;

    public SpellChecker() {
        this.wordFrequencyMap = new HashMap<>();
    }

    /** Load words into the hash table. O(1) per insertion. */
    public void loadWord(String word) {
        word = word.toLowerCase().trim();
        wordFrequencyMap.put(word, wordFrequencyMap.getOrDefault(word, 0) + 1);
    }

    /** Check if a word is correctly spelled. O(1) HashMap lookup. */
    public boolean isCorrect(String word) {
        if (word == null || word.isEmpty()) return true;
        return wordFrequencyMap.containsKey(word.toLowerCase().trim());
    }

    /**
     * Get spelling corrections using Levenshtein Edit Distance.
     * ALGORITHM: Dynamic Programming  O(N * L1 * L2)
     *
     * Scoring improvements:
     *  - Distance penalty:  lower distance → higher score
     *  - Length similarity: prefer words whose length is close to the input
     *  - Prefix bonus:      if dict word starts with same chars, rank higher
     *  - Frequency bonus:   more common words rank higher on ties
     */
    public List<WordEntry> getCorrections(String misspelled, int maxResults) {
        if (misspelled == null || misspelled.isEmpty()) return new ArrayList<>();
        misspelled = misspelled.toLowerCase().trim();
        if (isCorrect(misspelled)) return new ArrayList<>();

        // Dynamic threshold: very short words use distance 1,
        // longer words allow up to 2 so we always get useful results.
        int threshold = (misspelled.length() <= 4) ? 1 : MAX_EDIT_DISTANCE;

        List<WordEntry> corrections = new ArrayList<>();

        for (String dictWord : wordFrequencyMap.keySet()) {
            // Fast length pre-filter: skip words whose length differs by more
            // than the threshold — they can't possibly be within threshold edits
            if (Math.abs(dictWord.length() - misspelled.length()) > threshold) continue;

            int distance = editDistance(misspelled, dictWord);
            if (distance > threshold) continue;

            int freq = wordFrequencyMap.get(dictWord);

            // Score = distance penalty + prefix bonus + frequency bonus + length-match bonus
            double distancePenalty = (threshold + 1 - distance) * 20.0;
            double prefixBonus     = commonPrefixLength(misspelled, dictWord) * 5.0;
            double freqBonus       = Math.min(freq * 2.0, 20.0);   // cap so freq doesn't dominate
            double lenBonus        = (dictWord.length() == misspelled.length()) ? 5.0 : 0.0;

            double score = distancePenalty + prefixBonus + freqBonus + lenBonus;
            corrections.add(new WordEntry(dictWord, freq, score));
        }

        Collections.sort(corrections);
        return corrections.subList(0, Math.min(maxResults, corrections.size()));
    }

    /**
     * Levenshtein Edit Distance — Dynamic Programming
     * DATA STRUCTURE: 2D DP table (int[][])
     * TIME:  O(m * n)   SPACE: O(m * n)
     *
     * Operations counted:
     *   insert, delete, substitute  (each costs 1)
     */
    public int editDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();

        // dp[i][j] = min edits to convert s1[0..i-1] to s2[0..j-1]
        int[][] dp = new int[m + 1][n + 1];

        // Base cases: converting to/from empty string
        for (int i = 0; i <= m; i++) dp[i][0] = i;   // delete all chars of s1
        for (int j = 0; j <= n; j++) dp[0][j] = j;   // insert all chars of s2

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    // Characters match — no edit needed
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(
                        dp[i - 1][j],           // delete from s1
                        Math.min(
                            dp[i][j - 1],       // insert into s1
                            dp[i - 1][j - 1]    // substitute
                        )
                    );
                }
            }
        }

        return dp[m][n];
    }

    /** Returns the number of leading characters common to both words. */
    private int commonPrefixLength(String a, String b) {
        int len = Math.min(a.length(), b.length());
        for (int i = 0; i < len; i++) {
            if (a.charAt(i) != b.charAt(i)) return i;
        }
        return len;
    }

    /** Increment frequency when user selects a word. */
    public void incrementFrequency(String word) {
        word = word.toLowerCase().trim();
        wordFrequencyMap.put(word, wordFrequencyMap.getOrDefault(word, 0) + 1);
    }

    public HashMap<String, Integer> getWordFrequencyMap() { return wordFrequencyMap; }
    public int getDictionarySize()                        { return wordFrequencyMap.size(); }
}
