package algorithms;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aho-Corasick Multi-Pattern Search
 * =================================
 *
 * Use in SmartCityX: detect many emergency keywords ("fire", "flood",
 * "gas leak", ...) in a report with ONE pass over the text.
 *
 * Three parts, all built by hand:
 *   1. Trie (goto function) - every keyword is inserted into one prefix tree.
 *   2. Failure links        - fail[v] is the node for the longest proper
 *                             suffix of v's string that is also in the trie.
 *                             On a mismatch we follow failure links instead of
 *                             restarting (the multi-pattern version of KMP's LPS).
 *   3. Output links         - outLink[v] is the nearest node on v's failure
 *                             chain where a keyword ends. It reports keywords
 *                             that end inside a longer match (e.g. "he" inside
 *                             "she") without walking the whole failure chain.
 *
 * Complexity (L = total keyword length, n = text length, z = matches):
 *   Build  O(L)        (alphabet treated as a constant)
 *   Search O(n + z)
 *   Total  O(n + L + z)
 *   Space  O(L)        (at most L + 1 trie nodes)
 */
public final class AhoCorasick {

    /** One keyword occurrence in the text. */
    public static final class Match {
        public final int position;
        public final String keyword;

        public Match(int position, String keyword) {
            this.position = position;
            this.keyword = keyword;
        }

        @Override
        public String toString() {
            return "'" + keyword + "' at " + position;
        }
    }

    private final boolean ignoreCase;
    private final List<String> keywords = new ArrayList<>();

    // Node 0 is the root. Parallel lists store each node's data.
    private final List<Map<Character, Integer>> children = new ArrayList<>(); // trie edges
    private final List<Integer> fail = new ArrayList<>();                     // failure link
    private final List<Integer> outLink = new ArrayList<>();                  // output link (-1 = none)
    private final List<Integer> wordEnd = new ArrayList<>();                  // keyword index ending here (-1 = none)
    private final List<String> label = new ArrayList<>();                     // root-to-node string (display)

    public AhoCorasick(List<String> keywords) {
        this(keywords, true);
    }

    public AhoCorasick(List<String> keywordList, boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
        newNode("");
        for (String k : keywordList) {
            keywords.add(normalize(k));
        }
        for (int index = 0; index < keywords.size(); index++) {
            if (!keywords.get(index).isEmpty()) {
                insert(keywords.get(index), index);
            }
        }
        buildLinks();
    }

    /** Lower-case one character at a time so positions never shift. */
    private String normalize(String s) {
        if (!ignoreCase) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            sb.append(Character.toLowerCase(s.charAt(i)));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ trie
    private int newNode(String nodeLabel) {
        children.add(new LinkedHashMap<>());
        fail.add(0);
        outLink.add(-1);
        wordEnd.add(-1);
        label.add(nodeLabel);
        return children.size() - 1;
    }

    private void insert(String word, int index) {
        int node = 0;
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            Integer next = children.get(node).get(ch);
            if (next == null) {
                next = newNode(label.get(node) + ch);
                children.get(node).put(ch, next);
            }
            node = next;
        }
        if (wordEnd.get(node) == -1) { // ignore duplicate keywords
            wordEnd.set(node, index);
        }
    }

    // --------------------------------------------------------- failure links
    /** Breadth-first search, so a parent's link is ready before its children. */
    private void buildLinks() {
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int child : children.get(0).values()) {
            fail.set(child, 0); // depth-1 nodes fail to the root
            queue.add(child);
        }

        while (!queue.isEmpty()) {
            int node = queue.poll();
            for (Map.Entry<Character, Integer> edge : children.get(node).entrySet()) {
                char ch = edge.getKey();
                int child = edge.getValue();

                // Walk the parent's failure chain until `ch` can be followed.
                int f = fail.get(node);
                while (f != 0 && !children.get(f).containsKey(ch)) {
                    f = fail.get(f);
                }
                int target = children.get(f).getOrDefault(ch, 0);
                fail.set(child, target);

                // Output link: the fail node if a keyword ends there,
                // otherwise the fail node's own output link.
                outLink.set(child, wordEnd.get(target) != -1 ? target : outLink.get(target));
                queue.add(child);
            }
        }
    }

    // ---------------------------------------------------------------- search
    /** Every keyword occurrence, in the order the occurrences end in the text. */
    public List<Match> search(String text) {
        String source = normalize(text);
        List<Match> matches = new ArrayList<>();
        int node = 0;
        for (int i = 0; i < source.length(); i++) {
            char ch = source.charAt(i);
            // Follow failure links until a transition on ch exists.
            while (node != 0 && !children.get(node).containsKey(ch)) {
                node = fail.get(node);
            }
            node = children.get(node).getOrDefault(ch, 0);

            // Report the keyword ending here, then everything on the output links.
            int hit = wordEnd.get(node) != -1 ? node : outLink.get(node);
            while (hit != -1) {
                String word = keywords.get(wordEnd.get(hit));
                matches.add(new Match(i - word.length() + 1, word));
                hit = outLink.get(hit);
            }
        }
        return matches;
    }

    /** Distinct keywords found, in order of first appearance in the text. */
    public List<String> detect(String text) {
        List<Match> matches = search(text);
        matches.sort((a, b) -> a.position != b.position
                ? Integer.compare(a.position, b.position)
                : a.keyword.compareTo(b.keyword));
        List<String> found = new ArrayList<>();
        for (Match match : matches) {
            if (!found.contains(match.keyword)) {
                found.add(match.keyword);
            }
        }
        return found;
    }

    // ---------------------------------------------------- getters for display
    public int nodeCount() {
        return children.size();
    }

    public String nodeLabel(int node) {
        return label.get(node);
    }

    public int failLink(int node) {
        return fail.get(node);
    }

    public int outputLink(int node) {
        return outLink.get(node);
    }

    /** Keyword that ends at this node, or null. */
    public String keywordAt(int node) {
        int index = wordEnd.get(node);
        return index == -1 ? null : keywords.get(index);
    }
}
