# SmartCityX – Viva Questions and Answers

Short, simple answers you can say out loud. Each one points to the part of the program that demonstrates it.

---

## Project Questions

**Why did you choose SmartCityX?**
Cities produce a lot of text (complaints, emergency calls, maintenance logs, documents) and have limited resources. That makes the city a natural place to apply string algorithms, suffix structures and dynamic programming together in one realistic project instead of nine unrelated exercises.

**What is the problem statement?**
Build a command-line analytics system that searches and analyses city text data, detects emergencies, corrects citizen queries, and optimizes resource dispatch and analytics pipelines, using efficient algorithms implemented from scratch.

**What are the inputs and outputs?**
Inputs: citizen reports, emergency reports and keywords, service requests, misspelled queries, infrastructure identifiers and a maintenance log, infrastructure documents, resources with a budget, and analytics operations as matrix sizes (all in `data/CityData.java`).
Outputs: match positions and counts, LPS / Z / hash / LCP tables, detected keywords, sorted suffixes, common phrases, the DP matrix and corrected query, the best resource bitmask with dispatch order, and the optimal parenthesization with its minimum cost.

**How does each algorithm relate to the project?**
| Algorithm | City task |
|---|---|
| KMP | Search citizen reports for an issue ("leakage") |
| Z-Function | Find repeated words and patterns in a report ("traffic" repeats) |
| Rabin-Karp | Find infrastructure IDs ("WTR-1024") in logs |
| Aho-Corasick | Detect all emergency keywords in one pass |
| Suffix Array | Index documents for fast substring search |
| LCP / Kasai | Find shared service descriptions between documents |
| Levenshtein | Correct "pipline leakge" to "pipeline leakage" |
| Bitmask DP | Choose resources and dispatch order within a budget |
| Matrix Chain | Order analytics operations for minimum cost |

**Why Java, and why no external libraries?**
Java runs anywhere with just a JDK, and the brief was to write the algorithms ourselves. We never call `indexOf`, `contains`, regular expressions or any pattern-matching or DSA library. The tests use a small hand-written runner, so even JUnit is not needed.

**How do you know the code is correct?**
There are 71 automated tests (`./run.sh --test`). Each algorithm has a normal case, a multiple-match or complex case, and edge cases, including invalid input that must be rejected. Several tests compare against a brute-force answer: KMP against checking every position, Z against KMP, the suffix array against naive sorting, Kasai against direct LCP, Bitmask DP against trying every order, and MCM against plain recursion.

**How is the code organized?**
`Main.java` is the entry point: it reads the command-line options, shows the menu and runs the nine demonstrations. Each algorithm is one class in `algorithms/` and does not depend on the menu or on the other algorithms, so it can be tested alone. `data/CityData.java` holds the sample datasets, `ui/Terminal.java` handles prompts, input checks and formatting, and `tests/TestAlgorithms.java` holds the tests.

**How do you run it?**
`./run.sh` (or `run.bat` on Windows) compiles the program and opens the menu. `./run.sh --all` runs all nine demonstrations, `./run.sh --demo 4` runs one, and `./run.sh --test` runs the tests.

**What happens if the user types something wrong?**
The program never crashes on bad input. A prompt that needs a number or a short text explains what it expects and asks again, and pressing Enter always uses the sample value. The algorithm classes also check their own input and throw `IllegalArgumentException` for values they cannot handle, such as a zero matrix dimension or a prerequisite that does not exist.

**What was your individual contribution?**
*Answer this honestly in your own words.* Name the parts you personally designed, coded, tested or documented (for example: "I implemented the Aho-Corasick trie and failure links and wrote its tests"), say what you learned, and name one difficulty you solved. Be ready to explain any file you claim.

---

## KMP

**What is KMP?**
A single-pattern search algorithm that never moves backwards in the text. After a mismatch it uses a precomputed table to decide how much of the pattern is still matched.

**Why do we need LPS?**
`lps[i]` is the length of the longest proper prefix of `pattern[0..i]` that is also a suffix of it. After a mismatch at pattern index j, the last `lps[j-1]` matched characters already form a prefix of the pattern, so we continue from there instead of from 0. For example, "pipe pipeline" has `lps[8] = 4`, because "pipe" is both a prefix and a suffix of "pipe pipe".

**Why is KMP O(n + m)?**
Building the LPS table is O(m). During the search the text index i only moves forward (n steps), and each fallback lowers j, which can only fall as much as it rose. So there are at most 2n steps. The demo shows 51 comparisons for a 49-character report.

---

## Z-Function

**What does Z[i] represent?**
The length of the longest substring starting at position i that matches the beginning (prefix) of the string. In "traffic congestion traffic signals traffic", Z[19] = 8 because "traffic " at position 19 matches the prefix.

**How does the Z-box work?**
We keep [left, right), the right-most window known to match the prefix. For i inside the box, s[i..right) is a copy of s[i−left..right−left), so we start with Z[i] = min(right − i, Z[i − left]) and only compare new characters beyond `right`. Because `right` only moves forward, the whole algorithm is O(n).

**How do you search with Z?**
Build `pattern + separator + text`. Any position with Z ≥ pattern length is a match. The separator is a character that cannot occur in the text, so no match can run past the pattern. Textbooks write it as `$`; the code uses `\0`, because a real report could contain a `$` sign.

---

## Rabin-Karp

**What is rolling hash?**
The hash of a window is updated in O(1) when the window slides one step, instead of being recomputed in O(m):
`h_new = ((h_old − s[i]·B^(m−1))·B + s[i+m]) mod M`. We use B = 256 and M = 1,000,000,007.

**What is a hash collision?**
Two different strings with the same hash value. With a tiny modulus (M = 13) the demo gets 19 collisions ("spurious hits") while searching for "WTR-1024".

**Why verify matches?**
A matching hash only means the strings *might* be equal. Comparing the characters removes false positives, so the answer is always correct. That verification is also why the worst case is O(nm).

---

## Aho-Corasick

**Why use a trie?**
It stores all keywords together and shares their common prefixes ("accident" and "ambulance" share "a"). One walk over the text then follows all keywords at the same time.

**What are failure links?**
For a trie node spelling string s, the failure link points to the node for the longest proper suffix of s that is also a prefix in the trie. On a mismatch we follow it instead of restarting, which works like KMP's LPS but for many patterns. For example, "gas lea" fails to "ea" (from "earthquake").

**What are output links?**
They point to the nearest node on the failure chain where a keyword ends. They report keywords hidden inside longer matches. With keywords he/she/hers and the text "ushers", we find "she", "he" and "hers".

**Why is it useful for multiple patterns?**
Running KMP once per keyword costs O(k·n) for k keywords. Aho-Corasick costs O(n + L + z) no matter how many keywords there are: one pass over the text reports all of them.

---

## Suffix Array

**What is a suffix?**
The part of a string from some index to the end. The suffixes of "pipeline" are pipeline, ipeline, peline, eline, line, ine, ne, e.

**What is a suffix array?**
The starting indices of all suffixes in sorted order. For "pipeline" it is [7, 3, 5, 1, 4, 6, 2, 0] (e, eline, ine, ipeline, line, ne, peline, pipeline). All occurrences of a substring sit in one contiguous block, so binary search finds them in O(m log n).

**How is it constructed?**
By prefix doubling. First rank the suffixes by their first character. In each round, sort by the pair (rank[i], rank[i+k]), which orders them by their first 2k characters, then re-rank. Stop when every rank is different. There are at most log n rounds, each an O(n log n) sort, so it is O(n log² n). This is not SA-IS.

**How do you index several documents?**
Join them with a different separator character after each one, then build one suffix array. The separators are unique, so no match can cross from one document into the next.

---

## LCP / Kasai

**What is LCP?**
`lcp[i]` is the length of the longest common prefix of the suffixes at `sa[i-1]` and `sa[i]`, which are neighbours in sorted order. For "banana" the LCP array is [0, 1, 3, 0, 0, 2]. The 3 comes from "ana" / "anana".

**Why is Kasai O(n)?**
It visits suffixes in text order. If suffix i shares h characters with its sorted neighbour, suffix i+1 shares at least h − 1, so h never restarts from 0. h is at most n and drops by at most 1 per step, so it rises at most 2n times in total.

**How do you find common substrings?**
- Longest repeated substring: the largest LCP value.
- Longest common substring of two documents: the largest LCP between neighbouring suffixes that come from different documents. For documents A and B this gives "Water pipeline maintenance".

---

## Levenshtein Distance

**What is edit distance?**
The minimum number of single-character insertions, deletions and substitutions needed to turn one string into another. "pipline" → "pipeline" needs 1 (insert 'e').

**What are the DP states?**
`dp[i][j]` = minimum edits to turn the first i characters of the source into the first j characters of the target.

**What are the base cases?**
`dp[i][0] = i` (delete everything) and `dp[0][j] = j` (insert everything).

**What is the recurrence?**
If `s[i-1] == t[j-1]`, then `dp[i][j] = dp[i-1][j-1]`. Otherwise, `dp[i][j] = 1 + min(dp[i-1][j] (delete), dp[i][j-1] (insert), dp[i-1][j-1] (substitute))`. The answer is `dp[n][m]`. Time and space are O(nm).

**How is the query corrected?**
Each word is compared with every word in the city vocabulary. It is replaced by the closest word if the distance is small, at most one edit per three letters.

---

## Bitmask DP

**What is a bitmask?**
An integer whose bits represent a set. Bit i = 1 means item i is in the set. Set operations become fast bit operations: `mask | (1<<r)` adds r, and `mask & (1<<r)` tests for r.

**What does each bit represent?**
One city resource: bit 0 (`0001`) Water Tank, bit 1 (`0010`) Ambulance, bit 2 (`0100`) Fire Truck, bit 3 (`1000`) Generator. The answer `1101` means {Water Tank, Fire Truck, Generator}.

**What is the problem exactly?**
Each resource has a cost, a value, an urgency and prerequisites. One resource leaves per time slot, and a resource sent in slot t gives `value − urgency·t`. The Fire Truck must come after the Water Tank, and total cost must stay at or below the budget. We want the maximum total benefit. The answer is mask 1101 with order Water Tank → Fire Truck → Generator, cost 10 and benefit 105.

**Why use DP?**
The next slot equals the number of resources already sent (`popcount(mask)`). So the best future does not depend on the order of the past, only on the set. DP stores one best value per set:
`dp[mask | 1<<r] = max(dp[mask | 1<<r], dp[mask] + value[r] − urgency[r]·popcount(mask))`.
That is 2ⁿ·n work, instead of the n!-like number of orders brute force would try (65 for n = 4, trillions for n = 15). A test checks that the DP matches brute force.

**What is the complexity?**
O(2ⁿ · n) time and O(2ⁿ) space.

---

## Matrix-Chain Multiplication

**Why is Matrix Chain a DP problem?**
- *Optimal substructure:* the best way to multiply A1..An splits at some k, and both halves must themselves be computed in their best way.
- *Overlapping subproblems:* the same sub-chains (like A2..A3) appear in many splits. We solve each (i, j) once and store it.

**What is the recurrence?**
`m[i][i] = 0`; `m[i][j] = min over i ≤ k < j of m[i][k] + m[k+1][j] + p(i−1)·p(k)·p(j)`. `s[i][j]` stores the best k, and it is used to rebuild the brackets.

**Why is it O(n³)?**
There are O(n²) sub-chains (i, j), and each tries up to n split points, so O(n³) time. The two n×n tables take O(n²) space.

**What is the result for the pipeline?**
Dims [40, 20, 30, 10, 30]. The best order is ((A1 x (A2 x A3)) x A4), that is (Data Filtering × (Data Transformation × Document Indexing)) × Similarity Analysis, costing 26,000 multiplications against 48,000 left-to-right, a 45.8% saving.
