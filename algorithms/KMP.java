package algorithms;

import java.util.ArrayList;
import java.util.List;

/**
 * KMP (Knuth-Morris-Pratt) String Search
 * ======================================
 *
 * Use in SmartCityX: search citizen reports for a word or phrase.
 *
 * Idea:
 *   When a mismatch happens after matching j characters of the pattern, we
 *   never move the text pointer back. The LPS table tells us how many
 *   characters of the pattern are still matched, and we continue from there.
 *
 * LPS (Longest Proper Prefix which is also a Suffix):
 *   lps[i] = length of the longest proper prefix of pattern[0..i]
 *            that is also a suffix of pattern[0..i].
 *   Example: pattern "ababc" -> lps = [0, 0, 1, 2, 0]
 *
 * Complexity (n = text length, m = pattern length):
 *   buildLps : Time O(m),     Space O(m)
 *   search   : Time O(n + m), Space O(m)
 *   The text index i never moves backwards, and every fallback j = lps[j-1]
 *   is paid for by an earlier forward step, so the total work is linear.
 */
public final class KMP {

    private KMP() {
    }

    /** Everything one KMP search produces (for display and testing). */
    public static final class Result {
        public final String text;
        public final String pattern;
        public final int[] lps;
        public final List<Integer> positions = new ArrayList<>();
        public int comparisons; // number of character comparisons made

        Result(String text, String pattern, int[] lps) {
            this.text = text;
            this.pattern = pattern;
            this.lps = lps;
        }

        public boolean found() {
            return !positions.isEmpty();
        }

        public int count() {
            return positions.size();
        }
    }

    /** Build the LPS (prefix function) table of the pattern. */
    public static int[] buildLps(String pattern) {
        int m = pattern.length();
        int[] lps = new int[m];
        int length = 0; // length of the current longest prefix-suffix
        int i = 1;
        while (i < m) {
            if (pattern.charAt(i) == pattern.charAt(length)) {
                length++;
                lps[i] = length;
                i++;
            } else if (length != 0) {
                // Fall back to the next shorter prefix-suffix; i stays.
                length = lps[length - 1];
            } else {
                lps[i] = 0;
                i++;
            }
        }
        return lps;
    }

    /**
     * Find every (possibly overlapping) occurrence of pattern in text.
     * Positions are 0-based. An empty pattern gives no matches.
     */
    public static Result search(String text, String pattern) {
        int[] lps = buildLps(pattern);
        Result result = new Result(text, pattern, lps);
        int n = text.length();
        int m = pattern.length();
        if (m == 0 || m > n) {
            return result;
        }

        int i = 0; // index in text
        int j = 0; // index in pattern
        while (i < n) {
            result.comparisons++;
            if (text.charAt(i) == pattern.charAt(j)) {
                i++;
                j++;
                if (j == m) {
                    result.positions.add(i - m);
                    j = lps[j - 1]; // keep going to find overlapping matches
                }
            } else if (j != 0) {
                j = lps[j - 1]; // reuse the matched prefix; i does not move
            } else {
                i++;
            }
        }
        return result;
    }
}
