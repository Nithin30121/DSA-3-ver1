package tests;

import algorithms.AhoCorasick;
import algorithms.BitmaskDP;
import algorithms.BitmaskDP.Resource;
import algorithms.KMP;
import algorithms.LcpKasai;
import algorithms.Levenshtein;
import algorithms.MatrixChain;
import algorithms.RabinKarp;
import algorithms.SuffixArray;
import algorithms.ZFunction;
import data.CityData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import ui.Terminal;

/**
 * Automated tests for every SmartCityX algorithm.
 *
 * Uses a tiny self-contained test runner (no JUnit jar needed), so it runs
 * with nothing but the JDK:
 *
 *   javac -d out -sourcepath . Main.java
 *   java -cp out tests.TestAlgorithms
 *
 * Each algorithm has at least: a normal case, a multiple-match / complex
 * case, and edge cases (empty input, not found, single character, invalid
 * input, ...). Several tests compare the fast algorithm with a brute-force
 * answer.
 */
public final class TestAlgorithms {

    private TestAlgorithms() {
    }

    // ================================================================ runner
    private interface TestBody {
        void run() throws Exception;
    }

    private static final class TestCase {
        final String group;
        final String name;
        final TestBody body;

        TestCase(String group, String name, TestBody body) {
            this.group = group;
            this.name = name;
            this.body = body;
        }
    }

    private static final List<TestCase> TESTS = new ArrayList<>();
    private static String currentGroup = "";

    private static void group(String name) {
        currentGroup = name;
    }

    private static void test(String name, TestBody body) {
        TESTS.add(new TestCase(currentGroup, name, body));
    }

    /** Run every test; returns true when all pass. */
    public static boolean runAll(boolean verbose) {
        TESTS.clear();
        registerAll();
        long startTime = System.nanoTime();
        int passed = 0;
        List<String> failures = new ArrayList<>();
        String lastGroup = null;
        for (TestCase t : TESTS) {
            if (verbose && !t.group.equals(lastGroup)) {
                System.out.println();
                System.out.println("  " + Terminal.bold(t.group));
                lastGroup = t.group;
            }
            try {
                t.body.run();
                passed++;
                if (verbose) {
                    System.out.println("    " + Terminal.green("[PASS]") + " " + t.name);
                }
            } catch (Throwable e) {
                String message = e.getMessage() == null ? e.toString() : e.getMessage();
                failures.add(t.group + " > " + t.name + " : " + message);
                if (verbose) {
                    System.out.println("    " + Terminal.red("[FAIL]") + " " + t.name + "  ->  " + message);
                }
            }
        }
        System.out.println();
        System.out.println("  ------------------------------------------------------------");
        System.out.println("  Ran " + TESTS.size() + " tests: " + passed + " passed, " + failures.size() + " failed");
        System.out.println("  Time: " + (System.nanoTime() - startTime) / 1_000_000 + " ms");
        for (String f : failures) {
            System.out.println("  " + Terminal.red("FAILED: " + f));
        }
        System.out.println("  " + (failures.isEmpty()
                ? Terminal.green("OK - all tests passed")
                : Terminal.red("SOME TESTS FAILED")));
        return failures.isEmpty();
    }

    public static void main(String[] args) {
        System.out.println("SmartCityX - automated test suite");
        boolean ok = runAll(true);
        System.exit(ok ? 0 : 1);
    }

    // ============================================================ assertions
    private static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertArrayEquals(int[] expected, int[] actual) {
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError("expected " + Arrays.toString(expected) + " but was " + Arrays.toString(actual));
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertThrows(TestBody body) {
        try {
            body.run();
        } catch (IllegalArgumentException expected) {
            return;
        } catch (Exception other) {
            throw new AssertionError("wrong exception type: " + other);
        }
        throw new AssertionError("expected IllegalArgumentException, nothing was thrown");
    }

    /** Reference answer: check every start position directly. */
    private static List<Integer> bruteForceSearch(String text, String pattern) {
        List<Integer> positions = new ArrayList<>();
        if (pattern.isEmpty()) {
            return positions;
        }
        for (int i = 0; i + pattern.length() <= text.length(); i++) {
            boolean ok = true;
            for (int k = 0; k < pattern.length() && ok; k++) {
                ok = text.charAt(i + k) == pattern.charAt(k);
            }
            if (ok) {
                positions.add(i);
            }
        }
        return positions;
    }

    private static final String TRAFFIC_TEXT = "traffic congestion traffic signals traffic";
    private static final String IDS_TEXT = String.join(" ", CityData.INFRASTRUCTURE_IDS);

    // ================================================================= tests
    private static void registerAll() {
        registerKmp();
        registerZ();
        registerRabinKarp();
        registerAhoCorasick();
        registerSuffixArray();
        registerKasai();
        registerLevenshtein();
        registerBitmask();
        registerMatrixChain();
    }

    // ------------------------------------------------------------------ KMP
    private static void registerKmp() {
        group("KMP");
        test("normal: 'leakage' in citizen report 1", () -> {
            KMP.Result r = KMP.search(CityData.CITIZEN_REPORTS.get(0), "leakage");
            assertTrue(r.found(), "pattern should be found");
            assertEquals(List.of(15), r.positions);
            assertEquals(1, r.count());
        });
        test("multiple matches: 'traffic' appears 3 times", () ->
                assertEquals(List.of(0, 19, 35), KMP.search(TRAFFIC_TEXT, "traffic").positions));
        test("overlapping matches: 'aa' in 'aaaa'", () ->
                assertEquals(List.of(0, 1, 2), KMP.search("aaaa", "aa").positions));
        test("LPS table for 'ababaca' and 'aabaaab'", () -> {
            assertArrayEquals(new int[] {0, 0, 1, 2, 3, 0, 1}, KMP.buildLps("ababaca"));
            assertArrayEquals(new int[] {0, 1, 0, 1, 2, 2, 3}, KMP.buildLps("aabaaab"));
        });
        test("edge: pattern not found", () -> {
            KMP.Result r = KMP.search("Traffic congestion near City Center", "flood");
            assertTrue(!r.found(), "should not be found");
            assertEquals(0, r.count());
        });
        test("edge: empty text and empty pattern", () -> {
            assertEquals(List.of(), KMP.search("", "water").positions);
            assertEquals(List.of(), KMP.search("water", "").positions);
        });
        test("edge: pattern equals entire text", () ->
                assertEquals(List.of(0), KMP.search("pipeline", "pipeline").positions));
        test("edge: single character", () -> {
            assertEquals(List.of(0), KMP.search("a", "a").positions);
            assertEquals(List.of(1, 3, 5), KMP.search("banana", "a").positions);
        });
        test("case-sensitive: 'water' is not 'Water'", () -> {
            assertEquals(List.of(), KMP.search("Water pipeline", "water").positions);
            assertEquals(List.of(0), KMP.search("Water pipeline", "Water").positions);
        });
        test("linear: comparisons <= 2n and results match brute force", () -> {
            String text = "abababababcabababab";
            KMP.Result r = KMP.search(text, "ababc");
            assertEquals(bruteForceSearch(text, "ababc"), r.positions);
            assertTrue(r.comparisons <= 2 * text.length(), "too many comparisons: " + r.comparisons);
        });
    }

    // ----------------------------------------------------------- Z-Function
    private static void registerZ() {
        group("Z-Function");
        test("normal: Z array of 'aabxaab'", () ->
                assertArrayEquals(new int[] {7, 1, 0, 0, 3, 1, 0}, ZFunction.compute("aabxaab")));
        test("repeated pattern: prefix 'traffic' repeats at 19 and 35", () -> {
            int[] z = ZFunction.compute(TRAFFIC_TEXT);
            assertEquals(8, z[19]); // "traffic " (with the space)
            assertEquals(7, z[35]); // "traffic" at the end
            List<int[]> repeats = ZFunction.prefixRepeats(TRAFFIC_TEXT, 7);
            assertEquals(2, repeats.size());
            assertArrayEquals(new int[] {19, 8}, repeats.get(0));
            assertArrayEquals(new int[] {35, 7}, repeats.get(1));
        });
        test("multiple matches via pattern$text", () ->
                assertEquals(List.of(0, 19, 35), ZFunction.search(TRAFFIC_TEXT, "traffic")));
        test("repeated characters: 'aaaaa'", () ->
                assertArrayEquals(new int[] {5, 4, 3, 2, 1}, ZFunction.compute("aaaaa")));
        test("edge: empty string and single character", () -> {
            assertArrayEquals(new int[] {}, ZFunction.compute(""));
            assertArrayEquals(new int[] {1}, ZFunction.compute("a"));
            assertEquals(List.of(), ZFunction.search("", "a"));
        });
        test("edge: not found / pattern longer than text / equals text", () -> {
            assertEquals(List.of(), ZFunction.search("road repair", "flood"));
            assertEquals(List.of(), ZFunction.search("road", "road repair"));
            assertEquals(List.of(0), ZFunction.search("road", "road"));
        });
        test("separator: a '$' in the text or pattern cannot cause a false match", () -> {
            assertEquals(List.of(1, 3), ZFunction.search("a$b$b", "$b"));
            assertEquals(bruteForceSearch("cost $5 and $50", "$5"), ZFunction.search("cost $5 and $50", "$5"));
        });
        test("agrees with KMP on every citizen report", () -> {
            for (String report : CityData.CITIZEN_REPORTS) {
                for (String p : List.of("near", "e", "reported", "Road")) {
                    assertEquals(KMP.search(report, p).positions, ZFunction.search(report, p));
                }
            }
        });
    }

    // ----------------------------------------------------------- Rabin-Karp
    private static void registerRabinKarp() {
        group("Rabin-Karp");
        test("normal: find 'TRF-3011' in identifier list", () -> {
            RabinKarp.Result r = RabinKarp.search(IDS_TEXT, "TRF-3011");
            assertEquals(List.of(18), r.positions);
            assertEquals(0, r.spuriousHits);
        });
        test("multiple matches: prefix 'WTR' and repeated id in log", () -> {
            assertEquals(List.of(0, 9), RabinKarp.search(IDS_TEXT, "WTR").positions);
            assertEquals(2, RabinKarp.search(CityData.INFRASTRUCTURE_LOG, "WTR-1024").count());
            assertEquals(List.of("WTR-1024", "WTR-2034"),
                    RabinKarp.searchIdentifiers(CityData.INFRASTRUCTURE_IDS, "WTR"));
        });
        test("collisions: tiny modulus still gives correct answer", () -> {
            RabinKarp.Result small = RabinKarp.search(CityData.INFRASTRUCTURE_LOG, "WTR-1024", 256, 3);
            RabinKarp.Result big = RabinKarp.search(CityData.INFRASTRUCTURE_LOG, "WTR-1024");
            assertEquals(big.positions, small.positions);
            assertTrue(small.spuriousHits > 0, "mod 3 should cause spurious hits");
        });
        test("rolling hash equals direct hash for every window", () -> {
            String text = CityData.INFRASTRUCTURE_LOG;
            long[] rolled = RabinKarp.windowHashes(text, 8, 256, 101);
            for (int i = 0; i < rolled.length; i++) {
                assertEquals(RabinKarp.polynomialHash(text.substring(i, i + 8), 256, 101), rolled[i]);
            }
        });
        test("worst-case style input: 'aa' in 'aaaaa'", () ->
                assertEquals(List.of(0, 1, 2, 3), RabinKarp.search("aaaaa", "aa").positions));
        test("edge: empty pattern, not found, longer pattern, equals text", () -> {
            assertEquals(List.of(), RabinKarp.search(IDS_TEXT, "").positions);
            assertEquals(List.of(), RabinKarp.search(IDS_TEXT, "SWR-9999").positions);
            assertEquals(List.of(), RabinKarp.search("WTR", "WTR-1024").positions);
            assertEquals(List.of(0), RabinKarp.search("WTR-1024", "WTR-1024").positions);
            assertEquals(List.of(), RabinKarp.search("", "W").positions);
        });
        test("edge: invalid base or modulus is rejected", () -> {
            assertThrows(() -> RabinKarp.search("WTR-1024", "WTR", 256, 0));
            assertThrows(() -> RabinKarp.search("WTR-1024", "WTR", 0, 101));
            assertThrows(() -> RabinKarp.windowHashes("WTR-1024", 3, 256, -7));
        });
    }

    // --------------------------------------------------------- Aho-Corasick
    private static void registerAhoCorasick() {
        group("Aho-Corasick");
        AhoCorasick emergency = new AhoCorasick(CityData.EMERGENCY_KEYWORDS);
        test("normal: accident + ambulance detected", () ->
                assertEquals(List.of("accident", "ambulance"),
                        emergency.detect(CityData.EMERGENCY_REPORTS.get(0))));
        test("multiple keywords incl. phrase 'gas leak' and mixed case", () ->
                assertEquals(List.of("gas leak", "fire"), emergency.detect("GAS LEAK near FIRE station")));
        test("overlapping keywords use output links ('ushers')", () -> {
            AhoCorasick ac = new AhoCorasick(List.of("he", "she", "his", "hers"));
            List<String> found = new ArrayList<>();
            for (AhoCorasick.Match m : ac.search("ushers")) {
                found.add(m.keyword + "@" + m.position);
            }
            assertEquals(List.of("she@1", "he@2", "hers@2"), found);
        });
        test("repeated keyword counted every time", () ->
                assertEquals(3, emergency.search("fire fire fire").size()));
        test("agrees with KMP counts for every keyword", () -> {
            String text = "flood and fire; another fire; ambulance after accident; flood";
            List<AhoCorasick.Match> matches = emergency.search(text);
            for (String k : CityData.EMERGENCY_KEYWORDS) {
                int count = 0;
                for (AhoCorasick.Match m : matches) {
                    if (m.keyword.equals(k)) {
                        count++;
                    }
                }
                assertEquals(KMP.search(text, k).count(), count);
            }
        });
        test("edge: no emergency keyword / empty text", () -> {
            assertEquals(List.of(), emergency.detect("Streetlight repair pending in Ward 4."));
            assertEquals(List.of(), emergency.detect(""));
        });
        test("case-sensitive mode ignores duplicate and empty keywords", () -> {
            AhoCorasick exact = new AhoCorasick(List.of("Fire", "", "Fire"), false);
            assertEquals(5, exact.nodeCount()); // root + F, Fi, Fir, Fire
            assertEquals(List.of("Fire"), exact.detect("Fire near fire station"));
            assertEquals(1, exact.search("Fire near fire station").size());
        });
        test("edge: empty keyword list builds only the root", () -> {
            AhoCorasick empty = new AhoCorasick(List.of());
            assertEquals(1, empty.nodeCount());
            assertEquals(List.of(), empty.detect("fire"));
        });
    }

    // --------------------------------------------------------- Suffix Array
    private static void registerSuffixArray() {
        group("Suffix Array");
        test("normal: 'banana' and 'pipeline'", () -> {
            assertArrayEquals(new int[] {5, 3, 1, 0, 4, 2}, SuffixArray.build("banana"));
            assertArrayEquals(new int[] {7, 3, 5, 1, 4, 6, 2, 0}, SuffixArray.build("pipeline"));
        });
        test("prefix doubling agrees with naive sort (repeated patterns)", () -> {
            for (String s : List.of("mississippi", "aaaaaaa", "abababab", CityData.DOCUMENT_A,
                    CityData.INFRASTRUCTURE_LOG)) {
                assertArrayEquals(SuffixArray.buildNaive(s), SuffixArray.build(s));
            }
        });
        test("substring search with binary search", () -> {
            int[] sa = SuffixArray.build("banana");
            assertEquals(List.of(1, 3), SuffixArray.search("banana", sa, "ana"));
            assertEquals(List.of(0), SuffixArray.search("banana", sa, "banana"));
            assertEquals(List.of(), SuffixArray.search("banana", sa, "nab"));
            assertEquals(List.of(), SuffixArray.search("banana", sa, "bananas"));
        });
        test("document index: 'maintenance' in 4 documents", () -> {
            SuffixArray.DocumentIndex index = new SuffixArray.DocumentIndex(CityData.INFRASTRUCTURE_DOCUMENTS);
            List<Integer> docs = new ArrayList<>();
            for (int[] hit : index.search("maintenance")) {
                docs.add(hit[0]);
            }
            assertEquals(List.of(0, 1, 2, 5), docs);
            // "Water pipeline " is 15 characters, so the first hit is doc 0, offset 15
            assertArrayEquals(new int[] {0, 15}, index.search("maintenance").get(0));
        });
        test("document index offsets are relative to each document", () -> {
            SuffixArray.DocumentIndex index = new SuffixArray.DocumentIndex(CityData.INFRASTRUCTURE_DOCUMENTS);
            for (int[] hit : index.search("near")) {
                String doc = CityData.INFRASTRUCTURE_DOCUMENTS.get(hit[0]);
                assertEquals("near", doc.substring(hit[1], hit[1] + 4));
            }
            assertEquals(4, index.search("near").size());
        });
        test("edge: empty text, single character, empty document", () -> {
            assertArrayEquals(new int[] {}, SuffixArray.build(""));
            assertArrayEquals(new int[] {0}, SuffixArray.build("x"));
            SuffixArray.DocumentIndex index = new SuffixArray.DocumentIndex(List.of("", "road", ""));
            assertEquals(1, index.search("road").size());
            assertEquals(List.of(), SuffixArray.search("", new int[0], "a"));
        });
        test("edge: document numbers without a separator are rejected", () -> {
            assertTrue(SuffixArray.isSeparator(SuffixArray.separator(SuffixArray.MAX_DOCUMENTS - 1)),
                    "the last separator must be valid");
            assertThrows(() -> SuffixArray.separator(-1));
            assertThrows(() -> SuffixArray.separator(SuffixArray.MAX_DOCUMENTS));
        });
    }

    // ------------------------------------------------------------ LCP/Kasai
    private static void registerKasai() {
        group("LCP / Kasai");
        test("normal: LCP of 'banana'", () ->
                assertArrayEquals(new int[] {0, 1, 3, 0, 0, 2}, LcpKasai.build("banana", SuffixArray.build("banana"))));
        test("matches brute-force LCP on several strings", () -> {
            for (String s : List.of("mississippi", "aaaaa", "pipeline pipe", CityData.DOCUMENT_B)) {
                int[] sa = SuffixArray.build(s);
                int[] lcp = LcpKasai.build(s, sa);
                for (int i = 1; i < sa.length; i++) {
                    int h = 0;
                    while (sa[i] + h < s.length() && sa[i - 1] + h < s.length()
                            && s.charAt(sa[i] + h) == s.charAt(sa[i - 1] + h)) {
                        h++;
                    }
                    assertEquals(h, lcp[i]);
                }
            }
        });
        test("longest repeated substring", () -> {
            assertEquals("ana", LcpKasai.longestRepeatedSubstring("banana"));
            // "traffic " and " traffic" both repeat with length 8 (a tie); either is correct.
            String repeated = LcpKasai.longestRepeatedSubstring(TRAFFIC_TEXT);
            assertEquals(8, repeated.length());
            assertTrue(KMP.search(TRAFFIC_TEXT, repeated).count() >= 2, "must occur at least twice");
        });
        test("common service description between Document A and B", () -> {
            assertEquals("Water pipeline maintenance ",
                    LcpKasai.longestCommonSubstring(CityData.DOCUMENT_A, CityData.DOCUMENT_B));
            assertTrue(LcpKasai.commonPhrases(CityData.DOCUMENT_A, CityData.DOCUMENT_B, 5)
                    .contains("Water pipeline maintenance"), "phrase list should contain the description");
        });
        test("multiple shared phrases between full documents", () -> {
            List<String> phrases = LcpKasai.commonPhrases(
                    CityData.INFRASTRUCTURE_DOCUMENTS.get(0), CityData.INFRASTRUCTURE_DOCUMENTS.get(1), 8);
            assertEquals(List.of("Water pipeline maintenance", "ed near Central Park"), phrases);
        });
        test("common substring is found in the middle, in either order", () -> {
            String a = "road repair pending";
            String b = "pending road repair";
            assertEquals("road repair", LcpKasai.longestCommonSubstring(a, b));
            assertEquals("road repair", LcpKasai.longestCommonSubstring(b, a));
        });
        test("edge: empty, single character, nothing in common", () -> {
            assertArrayEquals(new int[] {}, LcpKasai.build("", new int[0]));
            assertArrayEquals(new int[] {0}, LcpKasai.build("a", new int[] {0}));
            assertEquals("", LcpKasai.longestCommonSubstring("", "road"));
            assertEquals("", LcpKasai.longestCommonSubstring("abc", "xyz"));
            assertEquals("", LcpKasai.longestRepeatedSubstring("abc"));
        });
    }

    // ---------------------------------------------------------- Levenshtein
    private static void registerLevenshtein() {
        group("Levenshtein");
        test("normal: classic 'kitten' -> 'sitting' = 3", () ->
                assertEquals(3, Levenshtein.distance("kitten", "sitting")));
        test("citizen words: 'pipline' and 'leakge' are 1 edit away", () -> {
            assertEquals(1, Levenshtein.distance("pipline", "pipeline"));
            assertEquals(1, Levenshtein.distance("leakge", "leakage"));
        });
        test("query correction: 'pipline leakge' -> 'pipeline leakage'", () -> {
            List<String> vocabulary = Levenshtein.buildVocabulary(CityData.SERVICE_REQUESTS);
            assertEquals("pipeline leakage", Levenshtein.correctQuery("pipline leakge", vocabulary).corrected);
        });
        test("several misspelled queries corrected", () -> {
            List<String> texts = new ArrayList<>(CityData.CITIZEN_REPORTS);
            texts.addAll(CityData.SERVICE_REQUESTS);
            List<String> vocabulary = Levenshtein.buildVocabulary(texts);
            assertEquals("garbage collection", Levenshtein.correctQuery("garbge colection", vocabulary).corrected);
            assertEquals("traffic signal repair", Levenshtein.correctQuery("trafic signl repair", vocabulary).corrected);
            assertEquals("power outage", Levenshtein.correctQuery("powr outag", vocabulary).corrected);
        });
        test("edit script length equals the distance", () -> {
            int edits = 0;
            for (Levenshtein.Operation op : Levenshtein.operations("pipline leakge", "pipeline leakage")) {
                if (!op.type.equals("keep")) {
                    edits++;
                }
            }
            assertEquals(Levenshtein.distance("pipline leakge", "pipeline leakage"), edits);
        });
        test("DP base cases: first row and column", () -> {
            int[][] dp = Levenshtein.table("abc", "wxyz");
            for (int i = 0; i <= 3; i++) {
                assertEquals(i, dp[i][0]);
            }
            for (int j = 0; j <= 4; j++) {
                assertEquals(j, dp[0][j]);
            }
        });
        test("edge: empty strings, identical strings, single character", () -> {
            assertEquals(3, Levenshtein.distance("", "abc"));
            assertEquals(3, Levenshtein.distance("abc", ""));
            assertEquals(0, Levenshtein.distance("", ""));
            assertEquals(0, Levenshtein.distance("road", "road"));
            assertEquals(1, Levenshtein.distance("a", "b"));
        });
        test("edge: upper-case query, and a query with no letters", () -> {
            List<String> vocabulary = Levenshtein.buildVocabulary(CityData.SERVICE_REQUESTS);
            assertEquals("pipeline leakage", Levenshtein.correctQuery("PIPLINE Leakge", vocabulary).corrected);
            assertEquals("", Levenshtein.correctQuery("123 !?", vocabulary).corrected);
        });
        test("edge: unknown word with no close match is kept", () ->
                assertEquals("xyzzyq", Levenshtein.correctQuery("xyzzyq",
                        Levenshtein.buildVocabulary(CityData.SERVICE_REQUESTS)).corrected));
    }

    // ----------------------------------------------------------- Bitmask DP
    /** Brute force over every ordered selection (for checking the DP). */
    private static int bruteForceBest(List<Resource> resources, int budget, int used, int spent) {
        int best = 0;
        int slot = BitmaskDP.countBits(used);
        for (int r = 0; r < resources.size(); r++) {
            Resource res = resources.get(r);
            if ((used & (1 << r)) != 0 || spent + res.cost > budget) {
                continue;
            }
            boolean ready = true;
            for (int p : res.requires) {
                ready &= (used & (1 << p)) != 0;
            }
            if (ready) {
                int value = BitmaskDP.gain(res, slot) + bruteForceBest(resources, budget, used | (1 << r), spent + res.cost);
                best = Math.max(best, value);
            }
        }
        return best;
    }

    private static void registerBitmask() {
        group("Bitmask DP");
        test("normal: city resources, budget 10 -> mask 1101, benefit 105", () -> {
            BitmaskDP.Result r = BitmaskDP.optimize(CityData.CITY_RESOURCES, CityData.RESOURCE_BUDGET);
            assertEquals("1101", BitmaskDP.toBinary(r.bestMask, 4));
            assertEquals(List.of(0, 2, 3), r.order); // Water Tank, Fire Truck, Generator
            assertEquals(10, r.totalCost);
            assertEquals(105, r.totalBenefit);
        });
        test("prerequisite respected: Fire Truck never before Water Tank", () -> {
            BitmaskDP.Result r = BitmaskDP.optimize(CityData.CITY_RESOURCES, 100);
            int waterTank = r.order.indexOf(0);
            int fireTruck = r.order.indexOf(2);
            assertTrue(fireTruck == -1 || (waterTank != -1 && waterTank < fireTruck), "order " + r.order);
            assertTrue(!r.reachable(0b0100), "Fire Truck alone must be unreachable");
        });
        test("DP equals brute force for several budgets", () -> {
            for (int budget = 0; budget <= 16; budget++) {
                BitmaskDP.Result r = BitmaskDP.optimize(CityData.CITY_RESOURCES, budget);
                assertEquals(bruteForceBest(CityData.CITY_RESOURCES, budget, 0, 0), r.totalBenefit);
            }
        });
        test("larger set (6 resources) equals brute force", () -> {
            List<Resource> six = List.of(
                    new Resource("Water Tank", 3, 30, 5),
                    new Resource("Ambulance", 4, 60, 15),
                    new Resource("Fire Truck", 5, 70, 10, 0),
                    new Resource("Generator", 2, 25, 5),
                    new Resource("Tow Truck", 3, 20, 2),
                    new Resource("Rescue Team", 6, 80, 20, 1, 3));
            for (int budget : new int[] {5, 10, 15, 23}) {
                assertEquals(bruteForceBest(six, budget, 0, 0), BitmaskDP.optimize(six, budget).totalBenefit);
            }
        });
        test("small resource set: single resource in / over budget", () -> {
            List<Resource> one = List.of(new Resource("Ambulance", 4, 60, 15));
            assertEquals(60, BitmaskDP.optimize(one, 4).totalBenefit);
            assertEquals(0, BitmaskDP.optimize(one, 3).totalBenefit);
            assertEquals(0, BitmaskDP.optimize(one, 3).bestMask);
        });
        test("edge: no resources", () -> {
            BitmaskDP.Result r = BitmaskDP.optimize(List.of(), 10);
            assertEquals(0, r.bestMask);
            assertEquals(List.of(), r.order);
            assertEquals(0, r.totalBenefit);
        });
        test("edge: a budget of zero or less dispatches nothing", () -> {
            for (int budget : new int[] {0, -5}) {
                BitmaskDP.Result r = BitmaskDP.optimize(CityData.CITY_RESOURCES, budget);
                assertEquals(0, r.bestMask);
                assertEquals(List.of(), r.order);
                assertEquals(0, r.totalBenefit);
            }
        });
        test("edge: invalid prerequisite or negative cost is rejected", () -> {
            assertThrows(() -> BitmaskDP.optimize(List.of(new Resource("Fire Truck", 5, 70, 10, 3)), 10));
            assertThrows(() -> BitmaskDP.optimize(List.of(new Resource("Fire Truck", 5, 70, 10, 0)), 10));
            assertThrows(() -> BitmaskDP.optimize(List.of(new Resource("Generator", -2, 25, 5)), 10));
        });
        test("bit helpers", () -> {
            assertEquals(3, BitmaskDP.countBits(0b1101));
            assertEquals("0101", BitmaskDP.toBinary(5, 4));
            assertEquals(List.of("Water Tank", "Fire Truck"), BitmaskDP.maskToNames(0b0101, CityData.CITY_RESOURCES));
        });
    }

    // --------------------------------------------------------- Matrix Chain
    /** Plain recursion over every split (exponential) to check the DP. */
    private static long bruteForceChain(int[] dims, int i, int j) {
        if (i == j) {
            return 0;
        }
        long best = Long.MAX_VALUE;
        for (int k = i; k < j; k++) {
            best = Math.min(best, bruteForceChain(dims, i, k) + bruteForceChain(dims, k + 1, j)
                    + (long) dims[i] * dims[k + 1] * dims[j + 1]);
        }
        return best;
    }

    private static void registerMatrixChain() {
        group("Matrix Chain");
        test("normal: analytics pipeline 40x20x30x10x30 -> 26000", () -> {
            int[] dims = MatrixChain.dimsFromShapes(CityData.ANALYTICS_OPERATIONS, CityData.ANALYTICS_SHAPES);
            assertArrayEquals(new int[] {40, 20, 30, 10, 30}, dims);
            MatrixChain.Result r = MatrixChain.solve(dims);
            assertEquals(26000L, r.minimumCost());
            assertEquals("((A1 x (A2 x A3)) x A4)",
                    MatrixChain.parenthesization(r.split, 0, 3, MatrixChain.defaultNames(4)));
            assertEquals(48000L, MatrixChain.leftToRightCost(dims));
        });
        test("complex: CLRS 6-matrix chain -> 15125", () -> {
            int[] dims = {30, 35, 15, 5, 10, 20, 25};
            MatrixChain.Result r = MatrixChain.solve(dims);
            assertEquals(15125L, r.minimumCost());
            assertEquals("((A1 x (A2 x A3)) x ((A4 x A5) x A6))",
                    MatrixChain.parenthesization(r.split, 0, 5, MatrixChain.defaultNames(6)));
        });
        test("DP equals brute-force recursion", () -> {
            int[][] chains = {{5, 10, 3, 12, 5, 50, 6}, {10, 20, 30, 40, 30}, {2, 3, 4}};
            for (int[] dims : chains) {
                assertEquals(bruteForceChain(dims, 0, dims.length - 2), MatrixChain.minimumCost(dims));
            }
        });
        test("small chain: one and two matrices", () -> {
            assertEquals(0L, MatrixChain.minimumCost(new int[] {5, 10}));
            assertEquals("A1", MatrixChain.parenthesization(MatrixChain.solve(new int[] {5, 10}).split, 0, 0,
                    MatrixChain.defaultNames(1)));
            assertEquals(6000L, MatrixChain.minimumCost(new int[] {10, 20, 30}));
        });
        test("edge: invalid input is rejected", () -> {
            assertThrows(() -> MatrixChain.solve(new int[] {5}));
            assertThrows(() -> MatrixChain.dimsFromShapes(new String[] {"A", "B"}, new int[][] {{2, 3}, {4, 5}}));
        });
        test("edge: zero, negative and oversized dimensions are rejected", () -> {
            assertThrows(() -> MatrixChain.solve(new int[] {5, 0, 3}));
            assertThrows(() -> MatrixChain.solve(new int[] {5, -2, 3}));
            assertThrows(() -> MatrixChain.solve(new int[] {5, MatrixChain.MAX_DIMENSION + 1}));
            assertThrows(() -> MatrixChain.leftToRightCost(new int[] {5, 0, 3}));
            assertThrows(() -> MatrixChain.dimsFromShapes(new String[] {"A"}, new int[][] {{2, 3}, {3, 5}}));
        });
    }
}
