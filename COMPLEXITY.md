# SmartCityX – Complexity Analysis

Every entry below matches the code as written. Nothing is copied from a textbook without checking the implementation.

**Notation.** n = text length (or number of matrices or resources, as stated), m = pattern length, L = total length of all keywords, z / occ = number of matches reported.

## Summary Table

| Algorithm | Purpose | Time Complexity | Space Complexity |
|---|---|---|---|
| KMP | Pattern Search | O(n + m) | O(m) |
| Z-Function | Pattern Analysis | O(n); search on `P$T` is O(n + m) | O(n); search O(n + m) |
| Rabin-Karp | Identifier Search | Average O(n + m), Worst O(nm) | O(1) extra |
| Aho-Corasick | Multi-pattern Search | O(n + L + z) | O(L) |
| Suffix Array | Document Indexing | Build O(n log² n) (prefix doubling); search O(m log n + occ) | O(n) |
| Kasai | LCP Construction | O(n) | O(n) |
| Levenshtein | Query Correction | O(nm) | O(nm) |
| Bitmask DP | Resource Optimization | O(2ⁿ · n), n = number of resources | O(2ⁿ) |
| Matrix Chain | Operation Optimization | O(n³), n = number of matrices | O(n²) |

---

## Why Each Bound Holds

### 1. KMP – `algorithms/KMP.java`
- **`buildLps`: O(m).** `i` only moves forward. `length` goes up by at most 1 per step of `i` and every fallback lowers it, so there are at most 2m iterations.
- **`search`: O(n).** Each loop iteration either moves `i` forward (at most n times) or lowers `j` through `lps`. `j` can only fall as much as it has risen, and it rises at most n times, so there are at most 2n iterations. The demo prints the comparison count to show this (51 comparisons for n = 49).
- **Space: O(m)** for the LPS array. The positions list is output.

### 2. Z-Function – `algorithms/ZFunction.java`
- **O(n).** Each successful character comparison in the `while` loop pushes `right` further right, and `right` never exceeds n. Every other step is O(1) per index.
- `search` runs the Z-function on `pattern + '\0' + text`, a string of length n + m + 1, so it costs **O(n + m)** time and space.

### 3. Rabin-Karp – `algorithms/RabinKarp.java`
- The pattern hash and the first window hash take O(m). Each of the n − m + 1 roll steps is O(1).
- A hash match is verified character by character in O(m).
- **Average: O(n + m).** With modulus 1,000,000,007, hash matches are almost always real matches.
- **Worst case: O(nm).** If every window's hash matches, every window is verified. This happens with text "aaaa…a" and pattern "aa…a", or with a tiny modulus (the demo uses M = 13 and gets 19 spurious hits).
- **Space: O(1) extra.** Only a few `long` variables. `windowHashes` returns O(n) hashes, but that is a display helper, not part of the search.

### 4. Aho-Corasick – `algorithms/AhoCorasick.java`
- **Trie build: O(L).** One node per keyword character at most. Children are stored in a `HashMap`, and the alphabet is treated as a constant.
- **Failure links: O(L)** in total, using the standard amortized argument. Along each keyword's path, the failure depth rises by at most 1 per character and each `while` step lowers it.
- **Search: O(n + z).** Each failure-link step lowers the current depth, which rose at most once per text character. Output links jump straight to the next node that ends a keyword, so every step on them reports a match.
- **Space: O(L)** for at most L + 1 nodes, plus their links.
- `detect()` also sorts the matches to list distinct keywords in order, which adds O(z log z). The core `search()` stays O(n + z).

### 5. Suffix Array – `algorithms/SuffixArray.java`
- **Method: prefix doubling.** Each round sorts by `(rank[i], rank[i+k])` with `Arrays.sort` (O(n log n) comparisons, each O(1)) and then re-ranks in O(n). There are at most ⌈log₂ n⌉ + 1 rounds, so the build is **O(n log² n)**.
- **Not SA-IS or DC3.** Those run in O(n), and this project does not claim that.
- `buildNaive` sorts the suffixes directly. It makes O(n log n) comparisons that can each cost O(n), so it is O(n² log n). It is used only to cross-check the fast version in the tests.
- **Search: O(m log n + occ).** Two binary searches, each with O(log n) steps, compare at most m characters per step.
- **Space: O(n)** for `sa`, `rank`, `newRank`, and the `docOf` / `docStart` arrays of `DocumentIndex`.

### 6. LCP / Kasai – `algorithms/LcpKasai.java`
- **`build`: O(n).** `h` is at most n and drops by at most 1 per iteration of the outer loop (n iterations). So `h` can go up at most 2n times in total, and the inner `while` loop runs O(n) times over the whole run.
- **Space: O(n)** for `rank` and `lcp`.
- The helpers `longestRepeatedSubstring`, `longestCommonSubstring` and `commonPhrases` first build a suffix array. They therefore cost **O(n log² n)** in total, dominated by construction, not by Kasai.

### 7. Levenshtein – `algorithms/Levenshtein.java`
- **`table` / `distance`: O(nm) time and O(nm) space.** It fills an (n+1) × (m+1) table with O(1) work per cell. The full table is kept so it can be printed and backtracked.
- **`operations`: O(nm)** to build the table, plus O(n + m) to backtrack.
- **`correctQuery`:** for W query words and a vocabulary of V words, the cost is O(W · V · k²), where k is the word length. Every candidate is compared with one Levenshtein call.
- A two-row version would use O(min(n, m)) space. See Future Scope in the README.

### 8. Bitmask DP – `algorithms/BitmaskDP.java` (n = number of resources)
- **`cost[mask]`: O(2ⁿ)** in total. Each cost comes from `mask` without its lowest set bit, in O(1).
- **Main DP: O(2ⁿ · n).** For each of the 2ⁿ masks it tries n resources, each check being O(1) with the prerequisite bitmask.
- **Picking the best mask: O(2ⁿ).** Rebuilding the dispatch order: O(n).
- **Space: O(2ⁿ)** for `dp`, `cost`, `parent` and `last`.
- For comparison, brute force over ordered selections tries Σₖ n!/(n−k)! sequences. That is 65 for n = 4 and about 3.5 × 10¹² for n = 15. The DP handles n = 15 in about 491,000 steps.

### 9. Matrix Chain – `algorithms/MatrixChain.java` (n = number of matrices)
- **Time: O(n³).** There are O(n²) (i, j) pairs, and each tries up to n − 1 split points k with O(1) work.
- **Space: O(n²)** for the `cost` and `split` tables.
- `parenthesization` visits every node of the split tree once, so it is O(n).
