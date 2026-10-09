package algorithms;

import java.util.ArrayList;
import java.util.List;

/**
 * Z-Function (Z Algorithm)
 * ========================
 *
 * Use in SmartCityX: find repeated patterns in city reports, and search a
 * keyword with the "pattern + separator + text" trick.
 *
 * Definition:
 *   Z[i] = length of the longest substring starting at i that is also a
 *          prefix of the whole string.
 *   By definition Z[0] = n (the whole string matches itself).
 *
 * Z-box:
 *   [left, right) is the right-most segment found so far that matches a
 *   prefix. If i is inside the box, s[i..right) equals s[i-left..right-left),
 *   so Z[i] can start at min(right - i, Z[i - left]) instead of 0. Only the
 *   part beyond `right` needs fresh character comparisons.
 *
 * Complexity:
 *   compute : Time O(n),     Space O(n)
 *             `right` only moves forward, so all successful extensions
 *             together are at most n.
 *   search  : Time O(n + m), Space O(n + m)
 */
public final class ZFunction {

    /** A character that never appears in normal city text. */
    public static final char SEPARATOR = '\u0000';

    private ZFunction() {
    }

    /** Return the Z array of s. */
    public static int[] compute(String s) {
        int n = s.length();
        int[] z = new int[n];
        if (n == 0) {
            return z;
        }
        z[0] = n;
        int left = 0;
        int right = 0; // current Z-box is s[left .. right-1]
        for (int i = 1; i < n; i++) {
            if (i < right) {
                // Reuse what we already know from inside the Z-box.
                z[i] = Math.min(right - i, z[i - left]);
            }
            // Extend the match character by character.
            while (i + z[i] < n && s.charAt(z[i]) == s.charAt(i + z[i])) {
                z[i]++;
            }
            // Move the Z-box if this match reaches further right.
            if (i + z[i] > right) {
                left = i;
                right = i + z[i];
            }
        }
        return z;
    }

    /**
     * Find all occurrences of pattern in text using the Z array of
     * pattern + SEPARATOR + text. Any index i in that combined string with
     * Z[i] >= m is a match starting at text index i - m - 1.
     */
    public static List<Integer> search(String text, String pattern) {
        List<Integer> positions = new ArrayList<>();
        int m = pattern.length();
        if (m == 0 || m > text.length()) {
            return positions;
        }
        String combined = pattern + SEPARATOR + text;
        int[] z = compute(combined);
        for (int i = m + 1; i < combined.length(); i++) {
            if (z[i] >= m) {
                positions.add(i - m - 1);
            }
        }
        return positions;
    }

    /**
     * Places where the text's own prefix appears again: every i > 0 with
     * Z[i] >= minLength. Each entry is {position, length}.
     */
    public static List<int[]> prefixRepeats(String text, int minLength) {
        int[] z = compute(text);
        List<int[]> repeats = new ArrayList<>();
        for (int i = 1; i < text.length(); i++) {
            if (z[i] >= minLength) {
                repeats.add(new int[] {i, z[i]});
            }
        }
        return repeats;
    }
}
