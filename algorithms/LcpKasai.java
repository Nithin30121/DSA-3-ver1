package algorithms;

import java.util.ArrayList;
import java.util.List;

/**
 * LCP Array with Kasai's Algorithm
 * ================================
 *
 * Use in SmartCityX: find text shared between city documents, e.g. the
 * common service description "Water pipeline maintenance".
 *
 * Definition:
 *   lcp[i] = length of the Longest Common Prefix of the suffixes at
 *            sa[i - 1] and sa[i] (neighbours in sorted order).
 *   lcp[0] = 0 because the first suffix has no previous neighbour.
 *
 * Kasai's idea:
 *   Visit suffixes in TEXT order (i = 0, 1, 2, ...), not in sorted order.
 *   If suffix i shares h characters with the suffix just before it in sorted
 *   order, then suffix i + 1 shares at least h - 1 characters with its own
 *   predecessor (drop the first character of both). So h never restarts from
 *   zero; it drops by at most 1 per step.
 *
 * Complexity:
 *   build : Time O(n), Space O(n)
 *           h is at most n and decreases by at most 1 per step, so it can
 *           increase at most 2n times in total -> the inner loop is O(n).
 *   The helper methods below also build a suffix array first, so they cost
 *   O(n log^2 n) overall because of that construction.
 */
public final class LcpKasai {

    private LcpKasai() {
    }

    /** Build the LCP array from the text and its suffix array. */
    public static int[] build(String text, int[] sa) {
        int n = text.length();
        int[] lcp = new int[n];
        int[] rank = new int[n]; // rank[i] = position of suffix i inside sa
        for (int position = 0; position < n; position++) {
            rank[sa[position]] = position;
        }

        int h = 0;
        for (int i = 0; i < n; i++) {
            if (rank[i] > 0) {
                int j = sa[rank[i] - 1]; // suffix just before suffix i in sorted order
                while (i + h < n && j + h < n && text.charAt(i + h) == text.charAt(j + h)) {
                    h++;
                }
                lcp[rank[i]] = h;
                if (h > 0) {
                    h--; // Kasai's key step: the next suffix keeps at least h - 1
                }
            } else {
                h = 0;
            }
        }
        return lcp;
    }

    /** Longest substring that occurs at least twice (the maximum LCP value). */
    public static String longestRepeatedSubstring(String text) {
        if (text.length() < 2) {
            return "";
        }
        int[] sa = SuffixArray.build(text);
        int[] lcp = build(text, sa);
        int best = 0;
        for (int i = 1; i < lcp.length; i++) {
            if (lcp[i] > lcp[best]) {
                best = i;
            }
        }
        return text.substring(sa[best], sa[best] + lcp[best]);
    }

    /**
     * Join a and b with a unique separator, build SA + LCP, and return every
     * pair of neighbouring suffixes that come from DIFFERENT documents as
     * {position in a, position in b, lcp}.
     */
    public static List<int[]> crossDocumentPairs(String a, String b) {
        String combined = a + SuffixArray.separator(0) + b;
        int[] sa = SuffixArray.build(combined);
        int[] lcp = build(combined, sa);
        int boundary = a.length(); // index of the separator
        List<int[]> pairs = new ArrayList<>();
        for (int i = 1; i < sa.length; i++) {
            int x = sa[i - 1];
            int y = sa[i];
            if (x < boundary && y > boundary) {
                pairs.add(new int[] {x, y - boundary - 1, lcp[i]});
            } else if (y < boundary && x > boundary) {
                pairs.add(new int[] {y, x - boundary - 1, lcp[i]});
            }
        }
        return pairs;
    }

    /**
     * Longest substring present in both a and b: the largest LCP between
     * neighbouring suffixes that come from different documents.
     */
    public static String longestCommonSubstring(String a, String b) {
        if (a.isEmpty() || b.isEmpty()) {
            return "";
        }
        int bestPos = 0;
        int bestLen = 0;
        for (int[] pair : crossDocumentPairs(a, b)) {
            if (pair[2] > bestLen) {
                bestPos = pair[0];
                bestLen = pair[2];
            }
        }
        return a.substring(bestPos, bestPos + bestLen);
    }

    /**
     * Shared phrases between two documents, longest first.
     *
     * A neighbouring cross-document pair with LCP >= minLength gives a shared
     * substring. We keep it only if it is "left-maximal": the characters just
     * before it differ (or it starts a document). Otherwise a longer shared
     * substring starting one character earlier exists, and this one is only
     * its tail.
     */
    public static List<String> commonPhrases(String a, String b, int minLength) {
        List<String> phrases = new ArrayList<>();
        if (a.isEmpty() || b.isEmpty()) {
            return phrases;
        }
        for (int[] pair : crossDocumentPairs(a, b)) {
            int posA = pair[0];
            int posB = pair[1];
            int length = pair[2];
            if (length < minLength) {
                continue;
            }
            if (posA > 0 && posB > 0 && a.charAt(posA - 1) == b.charAt(posB - 1)) {
                continue; // not left-maximal
            }
            String phrase = a.substring(posA, posA + length).trim();
            if (phrase.length() >= minLength && !phrases.contains(phrase)) {
                phrases.add(phrase);
            }
        }
        phrases.sort((p, q) -> p.length() != q.length()
                ? Integer.compare(q.length(), p.length())
                : p.compareTo(q));
        return phrases;
    }
}
