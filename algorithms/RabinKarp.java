package algorithms;

import java.util.ArrayList;
import java.util.List;

/**
 * Rabin-Karp String Search
 * ========================
 *
 * Use in SmartCityX: search infrastructure / service identifiers such as
 * "WTR-1024" or "TRF" inside identifier lists and maintenance logs.
 *
 * Idea:
 *   Treat every length-m window of the text as a number in base B, reduced
 *   modulo a prime M. Compare the window hash with the pattern hash. Only
 *   when the hashes are equal do we compare characters (collision check).
 *
 * Polynomial hash (Horner's rule):
 *   h(s[0..m-1]) = (s[0]*B^(m-1) + s[1]*B^(m-2) + ... + s[m-1]) mod M
 *
 * Rolling hash (slide the window one step right):
 *   h_new = ((h_old - s[i] * B^(m-1)) * B + s[i+m]) mod M
 *
 * Collision:
 *   Two different strings can have the same hash. Such a "spurious hit" is
 *   rejected by the character-by-character check, so results are always
 *   correct.
 *
 * Complexity:
 *   Average O(n + m)  - few hash matches, each verified in O(m)
 *   Worst   O(n * m)  - every window's hash matches (e.g. text "aaaa...a")
 *   Space   O(1) extra (besides the list of match positions)
 */
public final class RabinKarp {

    public static final long DEFAULT_BASE = 256;
    public static final long DEFAULT_MOD = 1_000_000_007L; // large prime

    private RabinKarp() {
    }

    /** Output of one search, with statistics for the demo. */
    public static final class Result {
        public final String pattern;
        public long patternHash;
        public final List<Integer> positions = new ArrayList<>();
        public int hashComparisons; // windows whose hash was compared
        public int hashMatches;     // windows whose hash equalled the pattern hash
        public int spuriousHits;    // hash matched but the characters did not

        Result(String pattern) {
            this.pattern = pattern;
        }

        public boolean found() {
            return !positions.isEmpty();
        }

        public int count() {
            return positions.size();
        }
    }

    /**
     * Reject a base or modulus the arithmetic cannot handle. Keeping both
     * below 2^31 means hash * base + character always fits in a long.
     */
    private static void checkParameters(long base, long mod) {
        if (base < 1 || base > Integer.MAX_VALUE || mod < 1 || mod > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("base and modulus must be between 1 and 2^31 - 1");
        }
    }

    /** Hash of a whole string using Horner's rule. */
    public static long polynomialHash(String s, long base, long mod) {
        checkParameters(base, mod);
        long h = 0;
        for (int i = 0; i < s.length(); i++) {
            h = (h * base + s.charAt(i)) % mod;
        }
        return h;
    }

    /** base^(m-1) mod M: the weight of the left-most character in a window. */
    private static long highPower(int m, long base, long mod) {
        long high = 1;
        for (int i = 0; i < m - 1; i++) {
            high = (high * base) % mod;
        }
        return high;
    }

    /** Slide the window: remove `outgoing`, append `incoming`. */
    private static long roll(long hash, char outgoing, char incoming, long high, long base, long mod) {
        hash = (hash - (outgoing * high) % mod + mod) % mod; // keep it non-negative
        return (hash * base + incoming) % mod;
    }

    /** Character-by-character verification (protects against collisions). */
    private static boolean matchesAt(String text, String pattern, int start) {
        for (int k = 0; k < pattern.length(); k++) {
            if (text.charAt(start + k) != pattern.charAt(k)) {
                return false;
            }
        }
        return true;
    }

    public static Result search(String text, String pattern) {
        return search(text, pattern, DEFAULT_BASE, DEFAULT_MOD);
    }

    /** Find every occurrence of pattern in text using a rolling hash. */
    public static Result search(String text, String pattern, long base, long mod) {
        checkParameters(base, mod);
        Result result = new Result(pattern);
        int n = text.length();
        int m = pattern.length();
        if (m == 0 || m > n) {
            return result;
        }

        long high = highPower(m, base, mod);
        long patternHash = polynomialHash(pattern, base, mod);
        long windowHash = polynomialHash(text.substring(0, m), base, mod);
        result.patternHash = patternHash;

        for (int start = 0; start + m <= n; start++) {
            result.hashComparisons++;
            if (windowHash == patternHash) {
                result.hashMatches++;
                if (matchesAt(text, pattern, start)) {
                    result.positions.add(start);
                } else {
                    result.spuriousHits++;
                }
            }
            if (start + m < n) {
                windowHash = roll(windowHash, text.charAt(start), text.charAt(start + m), high, base, mod);
            }
        }
        return result;
    }

    /** Hash of every length-m window, computed with the rolling formula. */
    public static long[] windowHashes(String text, int m, long base, long mod) {
        checkParameters(base, mod);
        int n = text.length();
        if (m <= 0 || m > n) {
            return new long[0];
        }
        long[] hashes = new long[n - m + 1];
        long high = highPower(m, base, mod);
        hashes[0] = polynomialHash(text.substring(0, m), base, mod);
        for (int start = 1; start + m <= n; start++) {
            hashes[start] = roll(hashes[start - 1], text.charAt(start - 1), text.charAt(start + m - 1), high, base, mod);
        }
        return hashes;
    }

    /** Every identifier that contains the pattern. */
    public static List<String> searchIdentifiers(List<String> identifiers, String pattern) {
        List<String> found = new ArrayList<>();
        for (String id : identifiers) {
            if (search(id, pattern).found()) {
                found.add(id);
            }
        }
        return found;
    }
}
