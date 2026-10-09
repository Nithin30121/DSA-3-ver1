# SMARTCITYX – Integrated Smart-City Analytics Platform

A command-line **Java** program that applies classic Data Structures and Algorithms to smart-city data: citizen reports, emergency reports, public-service requests, infrastructure documents, infrastructure identifiers and city resources.

It has no frontend, backend, database, APIs or external libraries. It is plain Java that you compile and run from a terminal.

> DSA Review-1 · Modules 1–3 · CO1–CO3

## Quick Start

You need a JDK (Java 11 or newer). From the `smartcityx` folder:

```bash
./run.sh
```

On Windows use `run.bat`. The script compiles the program and opens the menu. Type a number to run an algorithm, and press Enter at any question to use the sample value.

| Command | What it does |
|---|---|
| `./run.sh` | Interactive menu |
| `./run.sh --all` | Run all nine demonstrations with the sample inputs |
| `./run.sh --demo 4` | Run one demonstration (1–9) |
| `./run.sh --test` | Run the 71 automated tests |
| `./run.sh --data` | Print the sample city datasets |
| `./run.sh --help` | List every option |

---

## 1. Project Title

**SmartCityX – Integrated Smart-City Analytics Platform**

## 2. Problem Statement

A city receives a steady stream of text: citizen complaints, emergency calls, service requests, maintenance logs and infrastructure documents. It also has limited resources and a pipeline of analytics jobs. Officials need to:

- find reports that mention a given issue,
- spot repeated patterns and shared descriptions across documents,
- detect emergencies instantly from many keywords at once,
- look up infrastructure identifiers in logs,
- understand misspelled citizen queries,
- choose the best set of emergency resources within a budget, and
- run analytics operations in the cheapest order.

SmartCityX solves each of these with a specific algorithm implemented from scratch, and shows how each algorithm works and what it costs.

## 3. Motivation

Smart-city platforms are text-heavy and resource-constrained. Naive approaches, such as scanning text once per keyword, trying every resource order, or multiplying in a fixed order, waste time as the data grows. This project shows how string algorithms, suffix structures and dynamic programming give provably efficient solutions to realistic city problems.

## 4. Objectives

1. Implement **KMP, Z-Function, Rabin-Karp and Aho-Corasick** from scratch for pattern searching and multiple keyword detection (Module 1).
2. Implement a **Suffix Array** and **Kasai's LCP** algorithm for document indexing and common-substring analysis (Module 2).
3. Implement **Levenshtein distance, Bitmask DP and Matrix-Chain Multiplication** for query correction, resource optimization and analytics optimization (Module 3).
4. Show every algorithm's input, output, intermediate tables and complexity in a menu-driven program.
5. Verify every algorithm with automated tests (normal, complex and edge cases).

## 5. Inputs

All sample data lives in [`data/CityData.java`](data/CityData.java):

| Dataset | Example | Used by |
|---|---|---|
| 10 citizen reports | "Water pipeline leakage reported near Central Park" | KMP, Z, Aho-Corasick, Levenshtein |
| 5 emergency reports | "Major accident reported near hospital. Ambulance required immediately." | Aho-Corasick |
| 7 emergency keywords | fire, flood, accident, ambulance, gas leak, earthquake, collapse | Aho-Corasick |
| 10 public-service requests | "pipeline leakage", "power outage" | Levenshtein |
| 5 misspelled queries | "pipline leakge", "powr outag" | Levenshtein |
| 5 infrastructure IDs + maintenance log | WTR-1024, WTR-2034, TRF-3011, ENR-5012, BRG-4015 | Rabin-Karp |
| 6 infrastructure documents | "Water pipeline maintenance required near Central Park" | Suffix Array, Kasai |
| 4 city resources + budget | Water Tank, Ambulance, Fire Truck, Generator; budget 10 | Bitmask DP |
| 4 analytics operations | Data Filtering 40×20 → Data Transformation 20×30 → Document Indexing 30×10 → Similarity Analysis 10×30 | Matrix Chain |

In the interactive menu, every demonstration also lets you type your own pattern, query, text, budget or matrix dimensions. Press Enter to use the sample value. Input is checked: if you type something that cannot be used (letters for a budget, a text too long for the table, a zero matrix dimension), the program explains what it needs and asks again.

Menu option **12** (or `./run.sh --data`) prints all of these datasets.

## 6. Outputs

- Pattern found or not, the match positions and the match count (KMP, Z, Rabin-Karp)
- LPS table, Z array, pattern and window hashes, spurious-hit counts
- Detected emergency keywords and the trie with its failure links (Aho-Corasick)
- Sorted suffixes, the suffix array, substring search results (Suffix Array)
- The LCP array, longest repeated substring and common phrases between documents (Kasai)
- The DP matrix, minimum edit distance, edit operations and corrected query (Levenshtein)
- The DP table over all masks, best bitmask, selected resources, dispatch order, total cost and benefit (Bitmask DP)
- Cost table, split table, minimum cost and optimal parenthesization (Matrix Chain)
- Time and space complexity for every algorithm

## 7. Use Cases

| Who | Need | Feature |
|---|---|---|
| Helpdesk operator | Find all reports about "pipeline" | KMP / Z search |
| Analyst | Detect repeated phrases in a report | Z-Function |
| Maintenance engineer | Find every log entry for `WTR-1024` | Rabin-Karp |
| Emergency control room | Flag reports with any emergency keyword | Aho-Corasick |
| Records office | Index documents and search any substring | Suffix Array |
| Planner | Find shared service descriptions between documents | LCP / Kasai |
| Citizen app | Correct "pipline leakge" to "pipeline leakage" | Levenshtein |
| Disaster manager | Pick resources and dispatch order within a budget | Bitmask DP |
| Data team | Order analytics operations for minimum cost | Matrix Chain DP |

## 8. DSA Mapping

| Requirement | Algorithm | Module | CO |
|---|---|---|---|
| Citizen report search | KMP | M1 | CO1/CO2 |
| Repeated pattern detection | Z-Function | M1 | CO2 |
| Infrastructure identifier search | Rabin-Karp | M1 | CO2 |
| Emergency keyword detection | Aho-Corasick | M1 | CO2 |
| Document indexing | Suffix Array | M2 | CO2 |
| Common substring analysis | LCP/Kasai | M2 | CO2 |
| Query correction | Levenshtein | M3 | CO3 |
| Resource optimization | Bitmask DP | M3 | CO3 |
| Analytics optimization | Matrix Chain DP | M3 | CO3 |

## 9. Module 1 – String Algorithms (CO1, CO2)

None of these use `String.indexOf`, `contains`, `matches`, `split` or regular expressions. All matching logic is written by hand.

**KMP** ([`algorithms/KMP.java`](algorithms/KMP.java)) searches citizen reports. `buildLps` builds the LPS (prefix) table, where `lps[i]` is the longest proper prefix of `pattern[0..i]` that is also a suffix. On a mismatch the pattern index falls back to `lps[j-1]`, and the text index never moves backwards. The search returns whether the pattern exists, all positions, the match count and the number of character comparisons.

**Z-Function** ([`algorithms/ZFunction.java`](algorithms/ZFunction.java)) finds repeated patterns. `Z[i]` is the length of the longest substring starting at `i` that matches the string's prefix. The Z-box `[left, right)` lets each position reuse earlier work. For `"traffic congestion traffic signals traffic"`, `Z[19] = 8` and `Z[35] = 7` show that the prefix "traffic" repeats. To search for a pattern, it runs Z on `pattern + separator + text`. The separator is the character `\0`, which cannot occur in a report, so a match can never run past the pattern. The demo prints it as `$`.

**Rabin-Karp** ([`algorithms/RabinKarp.java`](algorithms/RabinKarp.java)) searches infrastructure identifiers. It uses a polynomial hash (base 256, modulus 1,000,000,007), computes the pattern hash, and rolls the window hash in O(1) per step. When the hashes match it verifies the characters one by one, which rejects collisions. The demo also reruns the search with modulus 13 to show 19 spurious hits being rejected.

**Aho-Corasick** ([`algorithms/AhoCorasick.java`](algorithms/AhoCorasick.java)) detects every emergency keyword in one pass. It builds a trie of all keywords, computes failure links by BFS, and adds output (dictionary) links so a keyword ending inside a longer match is still reported.

## 10. Module 2 – Suffix Structures (CO2)

**Suffix Array** ([`algorithms/SuffixArray.java`](algorithms/SuffixArray.java)) is built by **prefix doubling**. Each round sorts suffixes by the pair `(rank[i], rank[i+k])`, which ranks them by their first 2k characters, and it stops when all ranks differ. That is at most log n rounds of an O(n log n) sort, so construction is **O(n log² n)**. This is not the linear-time SA-IS algorithm. A naive "sort all suffixes" version is kept only to cross-check it in the tests. `DocumentIndex` joins all documents with unique separator characters, builds one suffix array, and finds any substring with two binary searches in O(m log n + occ).

**LCP / Kasai** ([`algorithms/LcpKasai.java`](algorithms/LcpKasai.java)): `lcp[i]` is the common-prefix length of the suffixes at `sa[i-1]` and `sa[i]`. Kasai's algorithm visits suffixes in text order and reuses `h − 1` from the previous suffix, so it runs in **O(n)**. Applications:
- *longest repeated substring* = the maximum LCP value
- *longest common substring* of two documents = the maximum LCP between neighbouring suffixes from different documents. Document A vs B gives **"Water pipeline maintenance"**.
- *shared phrases* = left-maximal cross-document matches

## 11. Module 3 – Advanced Dynamic Programming (CO3)

**Levenshtein** ([`algorithms/Levenshtein.java`](algorithms/Levenshtein.java)): `dp[i][j]` is the minimum number of edits to turn `source[0..i)` into `target[0..j)`.
- Base cases: `dp[i][0] = i` and `dp[0][j] = j`.
- Recurrence: `dp[i][j] = dp[i-1][j-1]` if the characters match. Otherwise it is `1 + min(delete, insert, substitute)`.
- Each word of the query is replaced by its closest vocabulary word, allowing about one typo per three letters.
- The demo prints the DP matrix and the edit operations.

**Bitmask DP** ([`algorithms/BitmaskDP.java`](algorithms/BitmaskDP.java)) chooses which resources to dispatch and in what order.
- Bit *i* of the mask represents resource *i*: `0001` Water Tank, `0010` Ambulance, `0100` Fire Truck, `1000` Generator.
- One resource leaves per time slot. A resource sent in slot *t* gives `value − urgency·t` benefit.
- The Fire Truck must come after the Water Tank, and the total cost must stay within the budget.
- `dp[mask]` is the best benefit after dispatching exactly the set `mask`. The next slot is `popcount(mask)`, so the state needs only the set, not the order.
- Transition: `dp[mask | 1<<r] = max(…, dp[mask] + value[r] − urgency[r]·popcount(mask))`.
- Result: mask **1101** = {Water Tank, Fire Truck, Generator}, cost 10, benefit **105**.

**Matrix-Chain Multiplication** ([`algorithms/MatrixChain.java`](algorithms/MatrixChain.java)) finds the cheapest multiplication order for the analytics pipeline A1…A4 with dims `[40, 20, 30, 10, 30]`.
- Recurrence: `m[i][j] = min over k of m[i][k] + m[k+1][j] + p(i−1)·p(k)·p(j)`, with base case `m[i][i] = 0`.
- The split table rebuilds the order `((A1 x (A2 x A3)) x A4)`, which costs **26,000** multiplications against 48,000 for left-to-right, a 45.8% saving.

## 12. Algorithms Used

| # | Algorithm | File | Key methods |
|---|---|---|---|
| 1 | KMP | `algorithms/KMP.java` | `buildLps`, `search` |
| 2 | Z-Function | `algorithms/ZFunction.java` | `compute`, `search`, `prefixRepeats` |
| 3 | Rabin-Karp | `algorithms/RabinKarp.java` | `polynomialHash`, `search`, `windowHashes`, `searchIdentifiers` |
| 4 | Aho-Corasick | `algorithms/AhoCorasick.java` | constructor (trie + links), `search`, `detect` |
| 5 | Suffix Array | `algorithms/SuffixArray.java` | `build`, `buildNaive`, `search`, `DocumentIndex` |
| 6 | LCP / Kasai | `algorithms/LcpKasai.java` | `build`, `longestRepeatedSubstring`, `longestCommonSubstring`, `commonPhrases` |
| 7 | Levenshtein | `algorithms/Levenshtein.java` | `table`, `distance`, `operations`, `correctQuery` |
| 8 | Bitmask DP | `algorithms/BitmaskDP.java` | `optimize`, `countBits`, `toBinary` |
| 9 | Matrix Chain | `algorithms/MatrixChain.java` | `solve`, `parenthesization`, `leftToRightCost` |

Each algorithm class is independent of the others and of the menu, and can be tested on its own. Public methods reject input they cannot handle (for example a zero matrix dimension, a prerequisite that does not exist, or a hash modulus of 0) with an `IllegalArgumentException` instead of returning a wrong answer.

## 13. Complexity Analysis

| Algorithm | Purpose | Time | Space |
|---|---|---|---|
| KMP | Pattern search | O(n + m) | O(m) |
| Z-Function | Pattern analysis | O(n) | O(n) |
| Rabin-Karp | Identifier search | Avg O(n + m), Worst O(nm) | O(1) extra |
| Aho-Corasick | Multi-pattern search | O(n + L + z) | O(L) |
| Suffix Array | Document indexing | O(n log² n) build, O(m log n + occ) search | O(n) |
| Kasai | LCP construction | O(n) | O(n) |
| Levenshtein | Query correction | O(nm) | O(nm) |
| Bitmask DP | Resource optimization | O(2ⁿ · n) | O(2ⁿ) |
| Matrix Chain | Operation optimization | O(n³) | O(n²) |

See [COMPLEXITY.md](COMPLEXITY.md) for the reasoning behind every entry.

## 14. How to Install

You need only a **JDK (Java Development Kit) 11 or newer**. JDK 17 or 21 is recommended. There are no other libraries to install.

- **macOS:** `brew install openjdk@21`
- **Windows / Linux:** install Eclipse Temurin JDK 21 from adoptium.net.
- **VS Code (optional):** the *Extension Pack for Java* by Microsoft adds the Run button and syntax help.

Check the install:

```bash
javac -version
```

If `javac` is not found on macOS after a Homebrew install, add this line to `~/.zshrc` and open a new terminal. (`run.sh` already looks in the Homebrew folders by itself.)

```bash
export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"
```

## 15. How to Run

Open a terminal in the `smartcityx` folder. In VS Code: **File → Open Folder…**, choose `smartcityx`, then **Terminal → New Terminal**.

**The easy way** is the launcher, which compiles and starts the program in one step:

```bash
./run.sh
```

On Windows:

```bat
run.bat
```

**By hand**, the same thing is two commands. The first compiles `Main.java` and every class it uses into `out/`; the second starts the menu.

```bash
javac -d out -sourcepath . Main.java
```

```bash
java -cp out Main
```

The menu:

```
============================================================================
  SMARTCITYX
  Integrated Smart-City Analytics Platform
============================================================================
  Module 1 - String Algorithms
    1. KMP Search                         search citizen reports
    2. Z-Function                         find repeated patterns in a report
    3. Rabin-Karp Search                  look up infrastructure identifiers
    4. Aho-Corasick Emergency Detection   detect emergency keywords
  Module 2 - Suffix Structures
    5. Build Suffix Array                 index and search documents
    6. LCP / Kasai Analysis               find phrases shared by documents
  Module 3 - Advanced Dynamic Programming
    7. Levenshtein Query Correction       correct misspelled queries
    8. Bitmask Resource Optimization      choose resources within a budget
    9. Matrix-Chain Optimization          order the analytics pipeline
  Tools
   10. Run All Demonstrations
   11. Run Test Cases
   12. View Sample City Data
    H. Help
    0. Exit
----------------------------------------------------------------------------
Select an option (0-12, H = help):
```

Using the menu:

- Type an option and press Enter. `0`, `q` or Ctrl+D exits; Ctrl+C stops the program at any time.
- A demonstration asks for its input like this:
  `> Word or phrase to find in report #1 [Enter = leakage]:`
  Press Enter to use the sample value in brackets, or type your own.
- Every demonstration prints its input, the algorithm's working tables, the output, and the time and space complexity.

Command-line options (they work with `./run.sh`, `run.bat` and `java -cp out Main`):

| Option | Effect |
|---|---|
| *(none)* | Start the interactive menu |
| `--all` | Run all nine demonstrations with the sample inputs, without asking questions |
| `--demo <1-9>` | Run one demonstration with the sample inputs |
| `--test` | Run the automated tests. The exit code is 0 when all pass |
| `--data` | Print the sample city datasets |
| `--no-color` | Plain output without colors |
| `--help`, `-h` | Show the options |

Colors are used only in a real terminal. They switch off automatically when the output is piped or saved to a file (`./run.sh --all > output.txt`), or when the `NO_COLOR` environment variable is set.

## 16. Test Commands

The tests are in [`tests/TestAlgorithms.java`](tests/TestAlgorithms.java). They use a small built-in test runner, so no JUnit jar is needed. There are 71 tests, with at least three per algorithm: a normal case, a multiple-match or complex case, and edge cases.

```bash
./run.sh --test
```

Or by hand:

```bash
javac -d out -sourcepath . Main.java
```

```bash
java -cp out tests.TestAlgorithms
```

You can also choose menu option **11**. The exit code is 0 when all tests pass, so the command can be used in a script.

The tests cover: empty strings, pattern not found, pattern equal to the whole text, overlapping and multiple matches, upper and lower case, repeated patterns, a `$` inside the text, several emergency keywords, overlapping keywords (he/she/hers), duplicate and empty keywords, forced hash collisions, empty documents, single-character input, small and empty resource sets, a budget of zero, one- and two-matrix chains, and invalid input (a zero modulus, a missing prerequisite, a negative cost, zero or oversized matrix dimensions). Several tests compare the fast algorithm against a brute-force answer: the DP against every dispatch order, MCM against plain recursion, the suffix array against naive sorting, and Kasai against direct LCP.

## 17. Sample Output

Excerpts from `./run.sh --all` (real output):

```
--- Output ----------------------------------------------------------------
  Pattern exists        : YES
  Matching positions    : [15]
  Number of matches     : 1
  Char comparisons      : 51  (n = 49; KMP never needs more than 2n)
    Water pipeline leakage reported near Central Park
                   ^^^^^^^
```

```
--- Repeated prefix detection ---------------------------------------------
  Prefix word           : "traffic" (length 7)
  Z[19] = 8  ->  prefix repeats at position 19: "traffic "
  Z[35] = 7  ->  prefix repeats at position 35: "traffic"
```

```
--- Collision demo: same search with a tiny modulus M = 13 ----------------
  Pattern               : "WTR-1024"
  M = 13                : hash matches 21, spurious hits 19, real matches [6, 103]
  M = 1e9+7             : hash matches 2, spurious hits 0, real matches [6, 103]
```

```
--- Output ----------------------------------------------------------------
  found "accident" at position 6
  found "ambulance" at position 39
  Detected keywords     : [accident, ambulance]
```

```
  Longest common        : "Water pipeline maintenance " (27 chars)
  Service description   : "Water pipeline maintenance"
```

```
  "pipline" -> "pipeline"   minimum edit distance = 1
         ""  p  i  p  e  l  i  n  e
      ""  0  1  2  3  4  5  6  7  8
       p  1  0  1  2  3  4  5  6  7
       i  2  1  0  1  2  3  4  5  6
       p  3  2  1  0  1  2  3  4  5
       l  4  3  2  1  1  1  2  3  4
       i  5  4  3  2  2  2  1  2  3
       n  6  5  4  3  3  3  2  1  2
       e  7  6  5  4  3  4  3  2  1
  Edits: [insert 'e']
  ...
  Suggested correction  : "pipeline leakage"
```

```
--- Optimal combination ---------------------------------------------------
  Best bitmask          : 1101 (decimal 13)
  Selected resources    : [Water Tank, Fire Truck, Generator]
  Dispatch order:
    slot 0: Water Tank  benefit 30 - 5*0 = 30
    slot 1: Fire Truck  benefit 70 - 10*1 = 60
    slot 2: Generator   benefit 25 - 5*2 = 15
  Total cost            : 10 of budget 10
  Total benefit         : 105
```

```
--- DP cost table m[i][j]  (minimum scalar multiplications for Ai..Aj) ----
              j=1      j=2      j=3      j=4
  i=1           0    24000    14000    26000
  i=2           -        0     6000    12000
  i=3           -        -        0     9000
  i=4           -        -        -        0
  ...
  Minimum cost          : 26000 scalar multiplications
  Optimal order         : ((A1 x (A2 x A3)) x A4)
  Saving                : 22000 fewer multiplications (45.8%)
```

```
  Ran 71 tests: 71 passed, 0 failed
  Time: 31 ms
  OK - all tests passed
```

## 18. Future Scope

- Replace prefix doubling with **SA-IS** for O(n) suffix-array construction on large archives.
- Add **whole-word matching** to Aho-Corasick so "fire" does not match inside "firewall".
- Use a **space-optimized Levenshtein** (two rows) and a BK-tree for fast lookup in a large vocabulary.
- Add **Damerau-Levenshtein** so swapped letters ("wtaer") cost one edit.
- Extend Bitmask DP to **assign resources to several incidents**, and use meet-in-the-middle for larger n.
- Load reports from CSV or live city feeds, and add a **suffix automaton** for streaming substring queries.

---

## Folder Structure

```
smartcityx/
├── Main.java                  entry point: options, menu and the nine demonstrations
├── algorithms/                one class per algorithm, no dependencies between them
│   ├── KMP.java
│   ├── ZFunction.java
│   ├── RabinKarp.java
│   ├── AhoCorasick.java
│   ├── SuffixArray.java
│   ├── LcpKasai.java
│   ├── Levenshtein.java
│   ├── BitmaskDP.java
│   └── MatrixChain.java
├── data/
│   └── CityData.java          all sample city datasets
├── ui/
│   └── Terminal.java          prompts, input checks, headings and colors
├── tests/
│   └── TestAlgorithms.java    71 automated tests (no JUnit needed)
├── run.sh / run.bat           compile + run in one command
├── .vscode/settings.json      points VS Code at JDK 21
├── README.md
├── COMPLEXITY.md              why every time and space bound holds
└── VIVA.md                    viva questions and answers
```

Compiled classes go to `out/`, which is created by the build and is safe to delete.

## Troubleshooting

| Problem | Fix |
|---|---|
| `javac: command not found` | Install a JDK (section 14), then open a new terminal. |
| `permission denied: ./run.sh` | Run `chmod +x run.sh` once, or start it with `sh run.sh`. |
| `class file has wrong version` while compiling, or `UnsupportedClassVersionError` while running | Classes compiled by a newer JDK are being read by an older one. Run `./run.sh`, which rebuilds everything with the JDK in use. If the error is from the two-command build, delete any `.class` files that sit next to the `.java` files and compile again. |
| Strange characters such as `[1;36m` in the output | Your terminal does not support colors. Add `--no-color`. |
| The menu exits at once when input is piped in | That is expected: the program stops when its input ends. |
