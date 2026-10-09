package algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Suffix Array
 * ============
 *
 * Use in SmartCityX: index infrastructure documents so any substring
 * ("maintenance", "near Central") can be located with binary search.
 *
 * Definitions:
 *   Suffix       : text[i..] for some start index i.
 *   Suffix array : start indices of all suffixes, in sorted (lexicographic)
 *                  order of the suffixes.
 *
 *   Example: text = "pipeline"
 *     sorted suffixes: e, eline, ine, ipeline, line, ne, peline, pipeline
 *     suffix array   : [7, 3, 5, 1, 4, 6, 2, 0]
 *
 * Construction method used: PREFIX DOUBLING (Manber-Myers idea)
 *   Round k sorts the suffixes by their first 2k characters.
 *   - rank[i] = rank of suffix i by its first k characters.
 *   - The first 2k characters of suffix i are described by the pair
 *     (rank[i], rank[i + k])   (-1 if i + k runs past the end).
 *   - Sort by these pairs; equal pairs get equal new ranks.
 *   - Stop when all ranks are different (every suffix is distinguished).
 *   There are at most log2(n) + 1 rounds and each round uses a comparison
 *   sort (O(n log n)).
 *
 * Complexity (honest, for THIS implementation):
 *   build      : Time O(n log^2 n), Space O(n)
 *   buildNaive : Time O(n^2 log n)  (each comparison can cost O(n)); reference only
 *   search     : Time O(m log n + occ), Space O(1) extra
 *   This is NOT the linear-time SA-IS / DC3 algorithm.
 */
public final class SuffixArray {

    // The Unicode private-use block: one separator character per document.
    private static final char FIRST_SEPARATOR = 0xE000;
    private static final char LAST_SEPARATOR = 0xF8FF;

    /** Most documents one DocumentIndex can hold (one separator each). */
    public static final int MAX_DOCUMENTS = LAST_SEPARATOR - FIRST_SEPARATOR + 1;

    private SuffixArray() {
    }

    /**
     * Unique separator for document k when several documents are joined.
     * Private-use Unicode characters never occur in normal city text, and
     * because every separator is different no common prefix can cross one.
     */
    public static char separator(int k) {
        if (k < 0 || k >= MAX_DOCUMENTS) {
            throw new IllegalArgumentException("document number must be between 0 and " + (MAX_DOCUMENTS - 1));
        }
        return (char) (FIRST_SEPARATOR + k);
    }

    public static boolean isSeparator(char ch) {
        return ch >= FIRST_SEPARATOR && ch <= LAST_SEPARATOR;
    }

    /** Sort the suffixes directly. Simple but slow; used to cross-check. */
    public static int[] buildNaive(String text) {
        Integer[] order = new Integer[text.length()];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> text.substring(a).compareTo(text.substring(b)));
        return toIntArray(order);
    }

    /** Build the suffix array by prefix doubling. */
    public static int[] build(String text) {
        final int n = text.length();
        if (n == 0) {
            return new int[0];
        }

        Integer[] sa = new Integer[n];
        int[] rank = new int[n];
        for (int i = 0; i < n; i++) {
            sa[i] = i;
            rank[i] = text.charAt(i); // round 0: rank by the first character
        }

        for (int k = 1; ; k *= 2) {
            final int[] r = rank;
            final int step = k;
            // Compare suffixes a and b by (rank of first k chars, rank of next k chars).
            Comparator<Integer> byPair = (a, b) -> {
                if (r[a] != r[b]) {
                    return Integer.compare(r[a], r[b]);
                }
                int secondA = a + step < n ? r[a + step] : -1;
                int secondB = b + step < n ? r[b + step] : -1;
                return Integer.compare(secondA, secondB);
            };
            Arrays.sort(sa, byPair);

            // Re-rank: equal pairs get equal ranks.
            int[] newRank = new int[n];
            for (int i = 1; i < n; i++) {
                newRank[sa[i]] = newRank[sa[i - 1]] + (byPair.compare(sa[i - 1], sa[i]) < 0 ? 1 : 0);
            }
            rank = newRank;

            if (rank[sa[n - 1]] == n - 1) { // all ranks distinct -> fully sorted
                break;
            }
        }
        return toIntArray(sa);
    }

    private static int[] toIntArray(Integer[] values) {
        int[] out = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = values[i];
        }
        return out;
    }

    /**
     * Compare suffix text[start..] with pattern, looking at no more than
     * pattern.length() characters.
     * Returns -1 if suffix < pattern, 0 if pattern is a prefix of the suffix,
     * +1 if suffix > pattern.
     */
    private static int comparePrefix(String text, int start, String pattern) {
        for (int k = 0; k < pattern.length(); k++) {
            if (start + k >= text.length()) {
                return -1; // suffix ended first, so it is smaller
            }
            char a = text.charAt(start + k);
            char b = pattern.charAt(k);
            if (a < b) {
                return -1;
            }
            if (a > b) {
                return 1;
            }
        }
        return 0;
    }

    /**
     * All start positions of pattern in text, found with two binary searches.
     * Every suffix that starts with the pattern lies in one contiguous block
     * of the suffix array.
     */
    public static List<Integer> search(String text, int[] sa, String pattern) {
        List<Integer> positions = new ArrayList<>();
        if (pattern.isEmpty() || sa.length == 0) {
            return positions;
        }

        // first suffix >= pattern
        int lo = 0;
        int hi = sa.length;
        while (lo < hi) {
            int mid = (lo + hi) / 2;
            if (comparePrefix(text, sa[mid], pattern) < 0) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        int first = lo;

        // first suffix > pattern (i.e. no longer starts with the pattern)
        hi = sa.length;
        while (lo < hi) {
            int mid = (lo + hi) / 2;
            if (comparePrefix(text, sa[mid], pattern) <= 0) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }

        for (int i = first; i < lo; i++) {
            positions.add(sa[i]);
        }
        Collections.sort(positions);
        return positions;
    }

    /**
     * Suffix-array index over several documents. The documents are joined as
     * doc0 SEP0 doc1 SEP1 ... and one suffix array is built over the result.
     */
    public static final class DocumentIndex {
        public final List<String> documents;
        public final String text;
        public final int[] sa;
        private final int[] docStart; // where each document begins in `text`
        private final int[] docOf;    // which document each position belongs to

        public DocumentIndex(List<String> documents) {
            this.documents = new ArrayList<>(documents);
            StringBuilder sb = new StringBuilder();
            docStart = new int[documents.size()];
            for (int d = 0; d < documents.size(); d++) {
                docStart[d] = sb.length();
                sb.append(documents.get(d)).append(separator(d));
            }
            text = sb.toString();
            docOf = new int[text.length()];
            for (int d = 0; d < documents.size(); d++) {
                int end = docStart[d] + documents.get(d).length();
                for (int p = docStart[d]; p <= end; p++) {
                    docOf[p] = d;
                }
            }
            sa = build(text);
        }

        /** Each match as {document number, offset inside that document}. */
        public List<int[]> search(String pattern) {
            List<int[]> hits = new ArrayList<>();
            for (int pos : SuffixArray.search(text, sa, pattern)) {
                int d = docOf[pos];
                hits.add(new int[] {d, pos - docStart[d]});
            }
            return hits;
        }

        public int documentOf(int position) {
            return docOf[position];
        }

        /** Suffix starting at pos, cut at its document's end, for display. */
        public String suffixPreview(int pos, int width) {
            StringBuilder out = new StringBuilder();
            for (int p = pos; p < text.length() && out.length() < width; p++) {
                char ch = text.charAt(p);
                if (isSeparator(ch)) {
                    out.append('$');
                    break;
                }
                out.append(ch);
            }
            return out.toString();
        }
    }
}
