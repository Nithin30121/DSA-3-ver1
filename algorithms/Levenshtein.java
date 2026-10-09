package algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

/**
 * Levenshtein (Edit) Distance
 * ===========================
 *
 * Use in SmartCityX: correct misspelled citizen queries, e.g.
 * "pipline leakge" -> "pipeline leakage".
 *
 * DP state:
 *   dp[i][j] = minimum number of edits to turn the first i characters of
 *              source into the first j characters of target.
 *
 * Base cases:
 *   dp[i][0] = i   (delete all i characters)
 *   dp[0][j] = j   (insert all j characters)
 *
 * Recurrence:
 *   if source[i-1] == target[j-1]:
 *       dp[i][j] = dp[i-1][j-1]                      (no cost)
 *   else:
 *       dp[i][j] = 1 + min(dp[i-1][j],               delete source[i-1]
 *                          dp[i][j-1],               insert target[j-1]
 *                          dp[i-1][j-1])             substitute
 *   Answer: dp[n][m]
 *
 * Complexity:
 *   Time O(n * m), Space O(n * m)
 *   (the full table is kept so it can be printed and back-tracked)
 */
public final class Levenshtein {

    private Levenshtein() {
    }

    /** One step of an edit script. */
    public static final class Operation {
        public final String type; // "keep", "substitute", "insert", "delete"
        public final String from;
        public final String to;

        Operation(String type, String from, String to) {
            this.type = type;
            this.from = from;
            this.to = to;
        }

        @Override
        public String toString() {
            switch (type) {
                case "keep":
                    return "keep '" + from + "'";
                case "substitute":
                    return "substitute '" + from + "' -> '" + to + "'";
                case "insert":
                    return "insert '" + to + "'";
                default:
                    return "delete '" + from + "'";
            }
        }
    }

    /** A suggested word and how many edits away it is. */
    public static final class Suggestion {
        public final String word;
        public final int distance;

        Suggestion(String word, int distance) {
            this.word = word;
            this.distance = distance;
        }
    }

    /** Result of correcting a whole query word by word. */
    public static final class Correction {
        public final String corrected;
        public final List<String> originalWords;
        public final List<Suggestion> choices; // one per original word

        Correction(String corrected, List<String> originalWords, List<Suggestion> choices) {
            this.corrected = corrected;
            this.originalWords = originalWords;
            this.choices = choices;
        }
    }

    /** Fill and return the full (n+1) x (m+1) DP table. */
    public static int[][] table(String source, String target) {
        int n = source.length();
        int m = target.length();
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= m; j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                if (source.charAt(i - 1) == target.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1],       // substitution
                                   Math.min(dp[i - 1][j],           // deletion
                                            dp[i][j - 1]));         // insertion
                }
            }
        }
        return dp;
    }

    public static int distance(String source, String target) {
        return table(source, target)[source.length()][target.length()];
    }

    /** Walk back from dp[n][m] to dp[0][0] to recover one optimal edit script. */
    public static List<Operation> operations(String source, String target) {
        int[][] dp = table(source, target);
        int i = source.length();
        int j = target.length();
        List<Operation> ops = new ArrayList<>();
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && source.charAt(i - 1) == target.charAt(j - 1)
                    && dp[i][j] == dp[i - 1][j - 1]) {
                ops.add(new Operation("keep", str(source, i - 1), str(target, j - 1)));
                i--;
                j--;
            } else if (i > 0 && j > 0 && dp[i][j] == dp[i - 1][j - 1] + 1) {
                ops.add(new Operation("substitute", str(source, i - 1), str(target, j - 1)));
                i--;
                j--;
            } else if (j > 0 && dp[i][j] == dp[i][j - 1] + 1) {
                ops.add(new Operation("insert", "", str(target, j - 1)));
                j--;
            } else {
                ops.add(new Operation("delete", str(source, i - 1), ""));
                i--;
            }
        }
        Collections.reverse(ops);
        return ops;
    }

    private static String str(String s, int index) {
        return String.valueOf(s.charAt(index));
    }

    // ------------------------------------------------------------ correction
    /** Split text into lower-case words made of letters only (no regex). */
    public static List<String> words(String text) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i <= text.length(); i++) {
            char ch = i < text.length() ? text.charAt(i) : ' ';
            if (Character.isLetter(ch)) {
                current.append(Character.toLowerCase(ch));
            } else if (current.length() > 0) {
                result.add(current.toString());
                current.setLength(0);
            }
        }
        return result;
    }

    /** Distinct words (2+ letters) from the given texts, sorted. */
    public static List<String> buildVocabulary(List<String> texts) {
        TreeSet<String> vocabulary = new TreeSet<>();
        for (String text : texts) {
            for (String word : words(text)) {
                if (word.length() >= 2) {
                    vocabulary.add(word);
                }
            }
        }
        return new ArrayList<>(vocabulary);
    }

    /** Candidate with the smallest edit distance (first one wins ties). */
    public static Suggestion closest(String word, List<String> candidates) {
        String bestWord = word;
        int bestDistance = Integer.MAX_VALUE;
        for (String candidate : candidates) {
            int d = distance(word, candidate);
            if (d < bestDistance) {
                bestWord = candidate;
                bestDistance = d;
            }
        }
        return new Suggestion(bestWord, bestDistance);
    }

    /** Allow roughly one typo per three letters (at least one). */
    public static int maxAllowedEdits(String word) {
        return Math.max(1, word.length() / 3);
    }

    /**
     * Correct a query word by word. Words already in the vocabulary, or with
     * no close enough match, are kept unchanged (distance 0).
     */
    public static Correction correctQuery(String query, List<String> vocabulary) {
        TreeSet<String> known = new TreeSet<>(vocabulary);
        List<String> original = words(query);
        List<Suggestion> choices = new ArrayList<>();
        StringBuilder corrected = new StringBuilder();
        for (String word : original) {
            Suggestion choice = new Suggestion(word, 0);
            if (!known.contains(word)) {
                Suggestion best = closest(word, vocabulary);
                if (best.distance <= maxAllowedEdits(word)) {
                    choice = best;
                }
            }
            choices.add(choice);
            if (corrected.length() > 0) {
                corrected.append(' ');
            }
            corrected.append(choice.word);
        }
        return new Correction(corrected.toString(), original, choices);
    }

    /** Known service request closest to the whole (lower-cased) query. */
    public static Suggestion bestMatchingQuery(String query, List<String> knownQueries) {
        return closest(query.toLowerCase(), knownQueries);
    }
}
