import static ui.Terminal.ask;
import static ui.Terminal.askInt;
import static ui.Terminal.askShort;
import static ui.Terminal.green;
import static ui.Terminal.kv;
import static ui.Terminal.quote;
import static ui.Terminal.red;
import static ui.Terminal.section;
import static ui.Terminal.shorten;
import static ui.Terminal.title;

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
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import tests.TestAlgorithms;
import ui.Terminal;

/**
 * SMARTCITYX - Integrated Smart-City Analytics Platform
 *
 * Entry point of the command-line program: it reads the options, shows the
 * menu and runs the nine demonstrations. The algorithms themselves live in
 * the `algorithms` package; this class only prepares their inputs and prints
 * their results.
 *
 * Compile and run from the project folder (or simply use ./run.sh):
 *   javac -d out -sourcepath . Main.java
 *   java -cp out Main              interactive menu
 *   java -cp out Main --all        every demonstration with the sample inputs
 *   java -cp out Main --demo 4     one demonstration (1-9) with the sample inputs
 *   java -cp out Main --test       the automated tests
 *   java -cp out Main --data       the sample city datasets
 *   java -cp out Main --help       every option
 */
public final class Main {

    /** Menu label and one-line purpose of demonstrations 1 to 9. */
    private static final String[][] MENU = {
        {"KMP Search", "search citizen reports"},
        {"Z-Function", "find repeated patterns in a report"},
        {"Rabin-Karp Search", "look up infrastructure identifiers"},
        {"Aho-Corasick Emergency Detection", "detect emergency keywords"},
        {"Build Suffix Array", "index and search documents"},
        {"LCP / Kasai Analysis", "find phrases shared by documents"},
        {"Levenshtein Query Correction", "correct misspelled queries"},
        {"Bitmask Resource Optimization", "choose resources within a budget"},
        {"Matrix-Chain Optimization", "order the analytics pipeline"}
    };

    private static final Runnable[] DEMOS = {
        Main::demoKmp, Main::demoZFunction, Main::demoRabinKarp, Main::demoAhoCorasick,
        Main::demoSuffixArray, Main::demoLcpKasai, Main::demoLevenshtein, Main::demoBitmask,
        Main::demoMatrixChain
    };

    // Limits on typed input, so the printed tables stay readable.
    private static final int MAX_ROW_TEXT = 80;         // text shown as an index / char / value table
    private static final int MAX_SUFFIX_TEXT = 40;      // text whose suffixes are all listed
    private static final int MAX_BUDGET = 1000;
    private static final int MAX_DEMO_NUMBERS = 12;     // dimensions of at most 11 matrices
    private static final int MAX_DEMO_DIMENSION = 10_000;

    private Main() {
    }

    public static void main(String[] args) {
        String mode = "";
        int demo = 0;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--no-color":
                    Terminal.setColor(false);
                    break;
                case "--all":
                case "--test":
                case "--data":
                case "--help":
                    mode = args[i];
                    break;
                case "-h":
                    mode = "--help";
                    break;
                case "--demo":
                    demo = i + 1 < args.length ? demoNumber(args[i + 1]) : 0;
                    if (demo == 0) {
                        usageError("--demo needs a number from 1 to 9, for example: --demo 4");
                    }
                    mode = args[i];
                    i++;
                    break;
                default:
                    usageError("Unknown option: " + args[i]);
            }
        }

        switch (mode) {
            case "--help":
                printUsage(System.out);
                break;
            case "--test":
                System.exit(TestAlgorithms.runAll(true) ? 0 : 1);
                break;
            case "--data":
                showData();
                break;
            case "--all":
                Terminal.setInteractive(false);
                System.exit(runAllDemos() ? 0 : 1);
                break;
            case "--demo":
                Terminal.setInteractive(false);
                System.exit(runDemo(demo) ? 0 : 1);
                break;
            default:
                menuLoop();
        }
    }

    private static void printUsage(PrintStream out) {
        out.println("  Usage: ./run.sh [options]        (or: java -cp out Main [options])");
        out.println();
        out.println("    (no option)     start the interactive menu");
        out.println("    --all           run all nine demonstrations with the sample inputs");
        out.println("    --demo <1-9>    run one demonstration with the sample inputs");
        out.println("    --test          run the automated tests (exit code 0 when all pass)");
        out.println("    --data          print the sample city datasets");
        out.println("    --no-color      plain output without colors");
        out.println("    --help, -h      show this help");
    }

    /** Report a wrong command-line option and stop with exit code 2. */
    private static void usageError(String message) {
        System.err.println("  " + message);
        System.err.println();
        printUsage(System.err);
        System.exit(2);
    }

    /** "1" to "9" gives 1 to 9; anything else gives 0. */
    private static int demoNumber(String s) {
        return s.length() == 1 && s.charAt(0) >= '1' && s.charAt(0) <= '9' ? s.charAt(0) - '0' : 0;
    }

    // ===================================================================== menu
    private static void menuLoop() {
        boolean showMenu = true;
        while (true) {
            if (showMenu && Terminal.canAsk()) {
                printMenu();
            }
            showMenu = true;
            String choice = Terminal.readLine("Select an option (0-12, H = help): ");
            if (choice == null) { // end of input (Ctrl+D)
                System.out.println("Goodbye!");
                return;
            }
            choice = choice.trim().toLowerCase(Locale.ROOT);
            switch (choice) {
                case "0":
                case "q":
                case "quit":
                case "exit":
                    System.out.println("Goodbye!");
                    return;
                case "10":
                    Terminal.setInteractive(false);
                    runAllDemos();
                    Terminal.setInteractive(true);
                    break;
                case "11":
                    System.out.println();
                    Terminal.rule();
                    System.out.println(Terminal.heading(" RUNNING AUTOMATED TEST CASES"));
                    Terminal.rule();
                    TestAlgorithms.runAll(true);
                    break;
                case "12":
                    showData();
                    break;
                case "h":
                case "help":
                case "?":
                    showHelp();
                    break;
                default:
                    if (demoNumber(choice) == 0) {
                        Terminal.error("Please type a number from 0 to 12, or H for help.");
                        showMenu = false;
                        continue;
                    }
                    runDemo(demoNumber(choice));
            }
            Terminal.pause();
        }
    }

    private static void printMenu() {
        System.out.println();
        Terminal.rule();
        System.out.println(Terminal.heading("  SMARTCITYX"));
        System.out.println(Terminal.heading("  Integrated Smart-City Analytics Platform"));
        Terminal.rule();
        for (int i = 0; i < MENU.length; i++) {
            if (i == 0) {
                System.out.println(Terminal.bold("  Module 1 - String Algorithms"));
            } else if (i == 4) {
                System.out.println(Terminal.bold("  Module 2 - Suffix Structures"));
            } else if (i == 6) {
                System.out.println(Terminal.bold("  Module 3 - Advanced Dynamic Programming"));
            }
            System.out.printf("   %2d. %-34s %s%n", i + 1, MENU[i][0], MENU[i][1]);
        }
        System.out.println(Terminal.bold("  Tools"));
        System.out.println("   10. Run All Demonstrations");
        System.out.println("   11. Run Test Cases");
        System.out.println("   12. View Sample City Data");
        System.out.println("    H. Help");
        System.out.println("    0. Exit");
        Terminal.thinRule();
    }

    /** Run demonstration 1 to 9. A failure is reported instead of ending the program. */
    private static boolean runDemo(int number) {
        try {
            DEMOS[number - 1].run();
            return true;
        } catch (RuntimeException e) {
            System.out.println();
            Terminal.error("Demonstration " + number + " (" + MENU[number - 1][0] + ") stopped unexpectedly: " + e);
            return false;
        }
    }

    private static boolean runAllDemos() {
        boolean allCompleted = true;
        for (int number = 1; number <= DEMOS.length; number++) {
            allCompleted &= runDemo(number);
        }
        System.out.println();
        Terminal.rule();
        System.out.println(allCompleted
                ? Terminal.heading(" All " + DEMOS.length + " demonstrations completed.")
                : red(" Some demonstrations did not complete. See the messages above."));
        Terminal.rule();
        return allCompleted;
    }

    private static void showHelp() {
        section("How to use SmartCityX");
        System.out.println("  - Type the number of a menu option and press Enter.");
        System.out.println("  - Options 1 to 9 each run one algorithm on smart-city data and show its");
        System.out.println("    input, its working tables, its output and its time / space complexity.");
        System.out.println("  - A demonstration asks for its input like this:");
        System.out.println("        > Word or phrase to find in report #1 [Enter = leakage]:");
        System.out.println("    Press Enter to use the sample value in brackets, or type your own.");
        System.out.println("  - Option 10 runs all nine with the sample values, option 11 runs the");
        System.out.println("    automated tests and option 12 lists the sample city data.");
        System.out.println("  - Type 0 (or q) to exit. Ctrl+C stops the program at any time.");
        section("Command-line options");
        printUsage(System.out);
    }

    // ===================================================== sample city data
    private static void showData() {
        System.out.println();
        Terminal.rule();
        System.out.println(Terminal.heading(" SAMPLE CITY DATA"));
        System.out.println("     All datasets are defined in data/CityData.java");
        Terminal.rule();

        printList("Citizen reports  (KMP, Z-Function, Aho-Corasick)", "", CityData.CITIZEN_REPORTS);
        printList("Emergency reports  (Aho-Corasick)", "", CityData.EMERGENCY_REPORTS);
        section("Emergency keywords  (Aho-Corasick)");
        System.out.println("  " + String.join(", ", CityData.EMERGENCY_KEYWORDS));
        printList("Public-service requests  (Levenshtein)", "", CityData.SERVICE_REQUESTS);
        printList("Misspelled citizen queries  (Levenshtein)", "", CityData.MISSPELLED_QUERIES);
        section("Infrastructure identifiers  (Rabin-Karp)");
        System.out.println("  " + String.join(", ", CityData.INFRASTRUCTURE_IDS));

        section("Maintenance log  (Rabin-Karp)");
        StringBuilder entry = new StringBuilder();
        String log = CityData.INFRASTRUCTURE_LOG;
        for (int i = 0; i <= log.length(); i++) {
            if (i == log.length() || log.charAt(i) == ';') { // entries are separated by "; "
                System.out.println("  " + entry.toString().trim());
                entry.setLength(0);
            } else {
                entry.append(log.charAt(i));
            }
        }

        printList("Infrastructure documents  (Suffix Array, LCP / Kasai)", "D", CityData.INFRASTRUCTURE_DOCUMENTS);
        section("City resources  (Bitmask DP), budget " + CityData.RESOURCE_BUDGET + " lakh rupees");
        printResources(CityData.CITY_RESOURCES);
        section("Analytics pipeline  (Matrix Chain)");
        int[] dims = MatrixChain.dimsFromShapes(CityData.ANALYTICS_OPERATIONS, CityData.ANALYTICS_SHAPES);
        printMatrices(MatrixChain.defaultNames(dims.length - 1), CityData.ANALYTICS_OPERATIONS, dims);
    }

    private static void printList(String name, String prefix, List<String> items) {
        section(name);
        for (int i = 0; i < items.size(); i++) {
            System.out.printf("  %3s  %s%n", prefix + (i + 1), items.get(i));
        }
    }

    private static void printResources(List<Resource> resources) {
        int n = resources.size();
        System.out.println("  bit  mask  resource      cost  value  urgency  must come after");
        for (int i = 0; i < n; i++) {
            Resource r = resources.get(i);
            List<String> after = new ArrayList<>();
            for (int p : r.requires) {
                after.add(resources.get(p).name);
            }
            System.out.printf("  %3d  %s  %-12s %5d  %5d  %7d  %s%n", i, BitmaskDP.toBinary(1 << i, n), r.name,
                    r.cost, r.value, r.urgency, after.isEmpty() ? "-" : String.join(", ", after));
        }
    }

    private static void printMatrices(String[] names, String[] operations, int[] dims) {
        for (int i = 0; i < names.length; i++) {
            System.out.printf("  %-4s %-22s %4d x %d%n", names[i], operations == null ? "" : operations[i],
                    dims[i], dims[i + 1]);
        }
    }

    // ============================================================ 1. KMP
    private static void demoKmp() {
        title(1, "KMP SEARCH", "Module 1 | CO1, CO2", "Search citizen reports");
        List<String> reports = CityData.CITIZEN_REPORTS;
        int number = askInt("Citizen report to search (1-" + reports.size() + ")", 1, 1, reports.size());
        String text = reports.get(number - 1);
        String pattern = ask("Word or phrase to find in report #" + number, "leakage");

        section("Input");
        kv("Text", quote(text) + "  (citizen report #" + number + ")");
        kv("Pattern", quote(pattern));
        kv("Matching", "exact characters (case-sensitive)");

        KMP.Result result = KMP.search(text, pattern);
        section("LPS (prefix) table of the pattern");
        printArrayRows(pattern, "lps", result.lps);
        System.out.println();
        String repeating = "pipe pipeline";
        System.out.println("  LPS of a pattern with repeats, " + quote(repeating) + ":");
        printArrayRows(repeating, "lps", KMP.buildLps(repeating));
        System.out.println("  lps[8] = 4 means \"pipe\" is both a prefix and a suffix of \"pipe pipe\",");
        System.out.println("  so after a mismatch KMP resumes at pattern index 4 instead of 0.");

        section("Output");
        kv("Pattern exists", result.found() ? green("YES") : Terminal.yellow("NO"));
        kv("Matching positions", result.positions);
        kv("Number of matches", result.count());
        kv("Char comparisons", result.comparisons + "  (n = " + text.length() + "; KMP never needs more than 2n)");
        if (result.found()) {
            System.out.println("    " + text);
            System.out.println("    " + green(markers(text.length(), result.positions, pattern.length())));
        }

        section("Search all " + reports.size() + " citizen reports (case-insensitive)");
        int matched = 0;
        for (int i = 0; i < reports.size(); i++) {
            String report = reports.get(i);
            KMP.Result r = KMP.search(report.toLowerCase(Locale.ROOT), pattern.toLowerCase(Locale.ROOT));
            if (r.found()) {
                matched++;
                System.out.printf("  Report %2d  positions %-8s %s%n", i + 1, r.positions, report);
            }
        }
        System.out.println("  Reports matched: " + matched + " of " + reports.size());

        section("Multiple-match example");
        String all = String.join(" | ", reports);
        KMP.Result near = KMP.search(all, "near");
        kv("Text", "all " + reports.size() + " reports joined (" + all.length() + " characters)");
        kv("Pattern", quote("near"));
        kv("Matches", near.count() + " at positions " + near.positions);

        complexity("O(n + m)", "O(m)  (LPS table)");
    }

    // ===================================================== 2. Z-Function
    private static void demoZFunction() {
        title(2, "Z-FUNCTION", "Module 1 | CO2", "Repeated patterns in city reports");
        String text = askShort("City report text", "traffic congestion traffic signals traffic", MAX_ROW_TEXT);
        int[] z = ZFunction.compute(text);

        section("Input");
        kv("Text", quote(text));
        kv("Length n", text.length());

        section("Z array   (Z[i] = longest match between text[i..] and the prefix)");
        printArrayRows(text, "Z", z);

        section("Repeated prefix detection");
        String firstWord = firstWord(text);
        List<int[]> repeats = ZFunction.prefixRepeats(text, Math.max(1, firstWord.length()));
        kv("Prefix word", quote(firstWord) + " (length " + firstWord.length() + ")");
        if (repeats.isEmpty()) {
            System.out.println("  The prefix word does not repeat.");
        }
        for (int[] rep : repeats) {
            String repeated = text.substring(rep[0], rep[0] + rep[1]);
            System.out.println("  Z[" + rep[0] + "] = " + rep[1] + "  ->  prefix repeats at position "
                    + rep[0] + ": " + quote(repeated));
        }

        section("Pattern search with Z on  pattern + separator + text");
        String pattern = ask("Pattern to search", firstWord.isEmpty() ? "traffic" : firstWord);
        List<Integer> positions = ZFunction.search(text, pattern);
        kv("Combined string", quote(pattern + "$" + text));
        kv("Separator", "\"$\" stands for \\0, a character no report can contain");
        kv("Rule", "position i with Z[i] >= " + pattern.length() + " is a match at i - " + (pattern.length() + 1));
        kv("Matching positions", positions);
        kv("Number of matches", positions.size());

        section("Reports that repeat / mention the pattern (case-insensitive)");
        int count = 0;
        for (int i = 0; i < CityData.CITIZEN_REPORTS.size(); i++) {
            String report = CityData.CITIZEN_REPORTS.get(i);
            List<Integer> hits = ZFunction.search(report.toLowerCase(Locale.ROOT), pattern.toLowerCase(Locale.ROOT));
            if (!hits.isEmpty()) {
                count++;
                System.out.printf("  Report %2d  positions %-8s %s%n", i + 1, hits, report);
            }
        }
        System.out.println("  Reports matched: " + count);

        complexity("O(n)  (O(n + m) for pattern search)", "O(n)");
    }

    // ===================================================== 3. Rabin-Karp
    private static void demoRabinKarp() {
        title(3, "RABIN-KARP SEARCH", "Module 1 | CO2", "Infrastructure identifier search");
        List<String> ids = CityData.INFRASTRUCTURE_IDS;
        String text = String.join(" ", ids);
        String pattern = ask("Identifier or prefix to search", "WTR").toUpperCase(Locale.ROOT);

        section("Input");
        kv("Identifiers", ids);
        kv("Searched text", quote(text));
        kv("Pattern", quote(pattern));

        long base = RabinKarp.DEFAULT_BASE;
        long mod = RabinKarp.DEFAULT_MOD;
        section("Rolling hash   (base B = " + base + ", modulus M = " + mod + ")");
        System.out.println("  h(window) = (s[0]*B^(m-1) + ... + s[m-1]) mod M");
        System.out.println("  roll:  h_new = ((h_old - s[i]*B^(m-1)) * B + s[i+m]) mod M");
        RabinKarp.Result result = RabinKarp.search(text, pattern);
        kv("Pattern hash", result.patternHash);
        long[] hashes = RabinKarp.windowHashes(text, pattern.length(), base, mod);
        if (hashes.length > 0) {
            System.out.println();
            System.out.println("  start  window        window hash   equal to pattern hash?");
            for (int s = 0; s < hashes.length; s++) {
                boolean equal = hashes[s] == result.patternHash;
                if (s < 5 || equal) {
                    System.out.printf("  %5d  %-12s %12d   %s%n", s,
                            quote(text.substring(s, s + pattern.length())), hashes[s],
                            equal ? "YES -> verify characters" : "no");
                } else if (s == 5) {
                    System.out.println("    ...  (only the first 5 windows and the hash hits are shown)");
                }
            }
        }

        section("Output");
        kv("Matching positions", result.positions);
        kv("Number of matches", result.count());
        kv("Matching identifiers", RabinKarp.searchIdentifiers(ids, pattern));
        kv("Hash comparisons", result.hashComparisons + "  (one per window)");
        kv("Hash matches", result.hashMatches);
        kv("Spurious hits", result.spuriousHits + "  (hash equal but text different)");

        section("Search the maintenance log");
        String log = CityData.INFRASTRUCTURE_LOG;
        RabinKarp.Result logResult = RabinKarp.search(log, pattern);
        kv("Log length", log.length() + " characters");
        kv("Matches", logResult.count() + " at positions " + logResult.positions);
        for (int pos : logResult.positions) {
            int start = Math.max(0, pos - 6);
            int end = Math.min(log.length(), pos + 30);
            System.out.println("    ..." + log.substring(start, end) + "...");
        }

        section("Collision demo: same search with a tiny modulus M = 13");
        String id = "WTR-1024";
        RabinKarp.Result small = RabinKarp.search(log, id, base, 13);
        RabinKarp.Result big = RabinKarp.search(log, id);
        kv("Pattern", quote(id));
        kv("M = 13", "hash matches " + small.hashMatches + ", spurious hits " + small.spuriousHits
                + ", real matches " + small.positions);
        kv("M = 1e9+7", "hash matches " + big.hashMatches + ", spurious hits " + big.spuriousHits
                + ", real matches " + big.positions);
        System.out.println("  Collisions are rejected by character verification, so both give the same answer.");

        complexity("Average O(n + m), worst O(n * m)", "O(1) extra");
    }

    // =================================================== 4. Aho-Corasick
    private static void demoAhoCorasick() {
        title(4, "AHO-CORASICK EMERGENCY DETECTION", "Module 1 | CO2", "Multiple emergency keywords");
        List<String> keywords = CityData.EMERGENCY_KEYWORDS;
        AhoCorasick ac = new AhoCorasick(keywords);

        section("Keywords and automaton");
        kv("Keywords", keywords);
        kv("Trie nodes", ac.nodeCount() + " (root + one node per distinct prefix)");
        System.out.println();
        System.out.println("  Nodes that end a keyword or have a failure link other than the root:");
        System.out.println("  node  string         fail -> string     output link   keyword ends here");
        for (int node = 1; node < ac.nodeCount(); node++) {
            int f = ac.failLink(node);
            String keyword = ac.keywordAt(node);
            if (f == 0 && keyword == null) {
                continue;
            }
            int out = ac.outputLink(node);
            System.out.printf("  %4d  %-14s %4d -> %-11s %-13s %s%n", node, quote(ac.nodeLabel(node)), f,
                    f == 0 ? "(root)" : quote(ac.nodeLabel(f)),
                    out == -1 ? "-" : quote(ac.nodeLabel(out)),
                    keyword == null ? "" : keyword);
        }
        System.out.println("  (Output links are '-' here because no keyword ends inside another one.");
        System.out.println("   With keywords he/she/hers, the node \"she\" gets output link \"he\".)");

        String text = ask("Emergency report text", CityData.EMERGENCY_REPORTS.get(0));
        section("Input");
        kv("Report", quote(text));
        kv("Matching", "case-insensitive, all keywords in one pass");

        section("Output");
        List<AhoCorasick.Match> matches = ac.search(text);
        for (AhoCorasick.Match m : matches) {
            System.out.println("  found " + quote(m.keyword) + " at position " + m.position);
        }
        if (matches.isEmpty()) {
            System.out.println("  No emergency keyword found in this report.");
        }
        kv("Detected keywords", ac.detect(text));
        kv("Total occurrences", matches.size());

        section("Classify every citizen and emergency report (single pass each)");
        List<String> allReports = new ArrayList<>(CityData.CITIZEN_REPORTS);
        allReports.addAll(CityData.EMERGENCY_REPORTS);
        int emergencies = 0;
        for (String report : allReports) {
            List<String> found = ac.detect(report);
            if (!found.isEmpty()) {
                emergencies++;
            }
            String tag = found.isEmpty() ? "[ normal  ]" : red("[EMERGENCY]");
            System.out.println("  " + tag + " " + shorten(report, 52) + (found.isEmpty() ? "" : "  " + found));
        }
        System.out.println("  Emergency reports: " + emergencies + " of " + allReports.size());

        complexity("O(n + L + z)  (text + total keyword length + matches)", "O(L)");
    }

    // =================================================== 5. Suffix Array
    private static void demoSuffixArray() {
        title(5, "BUILD SUFFIX ARRAY", "Module 2 | CO2", "Document indexing");
        String word = askShort("Short text to build a suffix array for", "pipeline", MAX_SUFFIX_TEXT);
        int[] sa = SuffixArray.build(word);

        section("Step 1: all suffixes in original order");
        for (int i = 0; i < word.length(); i++) {
            System.out.printf("  i = %2d   %s%n", i, word.substring(i));
        }

        section("Step 2: suffixes in sorted order (the suffix array)");
        System.out.println("  rank  SA[rank]  suffix");
        for (int r = 0; r < sa.length; r++) {
            System.out.printf("  %4d  %8d  %s%n", r, sa[r], word.substring(sa[r]));
        }
        kv("Suffix array", Arrays.toString(sa));
        kv("Naive sort check", Arrays.equals(sa, SuffixArray.buildNaive(word)) ? green("identical") : red("DIFFERENT"));

        section("Construction method: prefix doubling");
        System.out.println("  Round 0 ranks suffixes by their first character.");
        System.out.println("  Round k sorts by the pair (rank[i], rank[i + k]), i.e. by the first 2k characters,");
        System.out.println("  then re-ranks. It stops when every suffix has a different rank (<= log n rounds).");
        System.out.println("  Each round is a comparison sort, so the total is O(n log^2 n) - not SA-IS.");

        section("Step 3: index all infrastructure documents");
        SuffixArray.DocumentIndex index = new SuffixArray.DocumentIndex(CityData.INFRASTRUCTURE_DOCUMENTS);
        for (int d = 0; d < index.documents.size(); d++) {
            System.out.println("  D" + (d + 1) + ": " + index.documents.get(d));
        }
        kv("Indexed characters", index.text.length() + " (documents joined with unique separators $)");
        kv("Suffixes indexed", index.sa.length);

        section("Step 4: substring search with binary search on the suffix array");
        String query = ask("Substring to search in the documents (case-sensitive)", "maintenance");
        showDocumentSearch(index, query, true);
        for (String extra : new String[] {"near", "inspection"}) {
            if (!extra.equals(query)) {
                showDocumentSearch(index, extra, false);
            }
        }

        complexity("Build O(n log^2 n); search O(m log n + occ)", "O(n)");
    }

    private static void showDocumentSearch(SuffixArray.DocumentIndex index, String query, boolean showBlock) {
        List<int[]> hits = index.search(query);
        System.out.println();
        System.out.println("  Query " + quote(query) + " -> " + hits.size() + " match(es)");
        for (int[] hit : hits) {
            System.out.println("    D" + (hit[0] + 1) + " offset " + hit[1] + ": " + index.documents.get(hit[0]));
        }
        if (showBlock && !hits.isEmpty()) {
            // Show that all matches sit in one contiguous block of the suffix array.
            List<Integer> ranks = new ArrayList<>();
            for (int r = 0; r < index.sa.length; r++) {
                if (index.suffixPreview(index.sa[r], query.length()).equals(query)) {
                    ranks.add(r);
                }
            }
            int from = Math.max(0, ranks.get(0) - 2);
            int to = Math.min(index.sa.length - 1, ranks.get(ranks.size() - 1) + 2);
            System.out.println("  Suffix array rows " + from + ".." + to + " (>> = suffixes starting with the query):");
            for (int r = from; r <= to; r++) {
                int pos = index.sa[r];
                System.out.printf("    %s rank %3d  D%d  %s%n", ranks.contains(r) ? ">>" : "  ", r,
                        index.documentOf(pos) + 1, quote(index.suffixPreview(pos, 40)));
            }
        }
    }

    // ===================================================== 6. LCP / Kasai
    private static void demoLcpKasai() {
        title(6, "LCP / KASAI ANALYSIS", "Module 2 | CO2", "Common substring analysis");
        String text = askShort("Text for the LCP table", "pipeline pipe", MAX_SUFFIX_TEXT);
        int[] sa = SuffixArray.build(text);
        int[] lcp = LcpKasai.build(text, sa);

        section("Suffix array + LCP array   (LCP[i] = common prefix of rows i-1 and i)");
        System.out.println("  rank  SA  LCP  suffix");
        for (int r = 0; r < sa.length; r++) {
            System.out.printf("  %4d  %2d  %3d  %s%n", r, sa[r], lcp[r], quote(text.substring(sa[r])));
        }
        kv("LCP array", Arrays.toString(lcp));
        String repeated = LcpKasai.longestRepeatedSubstring(text);
        kv("Longest repeated", repeated.isEmpty()
                ? "none (no substring occurs twice)"
                : quote(repeated) + "  (maximum LCP value)");

        section("Common service description: Document A vs Document B");
        String a = CityData.DOCUMENT_A;
        String b = CityData.DOCUMENT_B;
        kv("Document A", quote(a));
        kv("Document B", quote(b));
        kv("Combined", quote(a + "#" + b) + "  (# = unique separator)");
        List<int[]> pairs = LcpKasai.crossDocumentPairs(a, b);
        pairs.sort((p, q) -> Integer.compare(q[2], p[2]));
        System.out.println();
        System.out.println("  Neighbouring suffixes from DIFFERENT documents, highest LCP first:");
        System.out.println("   LCP  suffix from A                          suffix from B");
        for (int i = 0; i < Math.min(5, pairs.size()); i++) {
            int[] p = pairs.get(i);
            System.out.printf("  %4d  %-38s %s%n", p[2], quote(shorten(a.substring(p[0]), 35)),
                    quote(shorten(b.substring(p[1]), 35)));
        }
        String lcs = LcpKasai.longestCommonSubstring(a, b);
        System.out.println();
        kv("Longest common", quote(lcs) + " (" + lcs.length() + " chars)");
        kv("Service description", quote(lcs.trim()));
        kv("Shared phrases", LcpKasai.commonPhrases(a, b, 5));

        section("Shared phrases between infrastructure documents (length >= 10)");
        List<String> docs = CityData.INFRASTRUCTURE_DOCUMENTS;
        for (int i = 0; i < docs.size(); i++) {
            for (int j = i + 1; j < docs.size(); j++) {
                String common = LcpKasai.longestCommonSubstring(docs.get(i), docs.get(j)).trim();
                if (common.length() >= 10) {
                    System.out.printf("  D%d & D%d  (%2d chars)  %s%n", i + 1, j + 1, common.length(), quote(common));
                }
            }
        }

        complexity("Kasai O(n)  (after the O(n log^2 n) suffix array)", "O(n)");
    }

    // =================================================== 7. Levenshtein
    private static void demoLevenshtein() {
        title(7, "LEVENSHTEIN QUERY CORRECTION", "Module 3 | CO3", "Correct misspelled citizen queries");
        List<String> sources = new ArrayList<>(CityData.CITIZEN_REPORTS);
        sources.addAll(CityData.SERVICE_REQUESTS);
        sources.addAll(CityData.INFRASTRUCTURE_DOCUMENTS);
        List<String> vocabulary = Levenshtein.buildVocabulary(sources);
        String sample = "pipline leakge";
        String query = ask("Citizen query to correct", sample);
        if (Levenshtein.words(query).isEmpty()) {
            Terminal.note("That query has no letters to correct, so the sample query is used instead.");
            query = sample;
        }

        section("DP definition");
        System.out.println("  dp[i][j] = min edits to turn the first i chars of source into the first j chars of target");
        System.out.println("  Base cases: dp[i][0] = i (deletions), dp[0][j] = j (insertions)");
        System.out.println("  If s[i-1] == t[j-1]: dp[i][j] = dp[i-1][j-1]");
        System.out.println("  Else: dp[i][j] = 1 + min(dp[i-1][j] delete, dp[i][j-1] insert, dp[i-1][j-1] substitute)");

        section("Input");
        kv("Query", quote(query));
        kv("Vocabulary", vocabulary.size() + " words taken from reports, requests and documents");

        Levenshtein.Correction correction = Levenshtein.correctQuery(query, vocabulary);
        section("Word-by-word correction");
        for (int i = 0; i < correction.originalWords.size(); i++) {
            String original = correction.originalWords.get(i);
            Levenshtein.Suggestion choice = correction.choices.get(i);
            if (original.equals(choice.word)) {
                System.out.println("  " + quote(original) + " is fine (known word or no close match)");
                continue;
            }
            System.out.println();
            System.out.println("  " + quote(original) + " -> " + quote(choice.word) + "   minimum edit distance = "
                    + choice.distance);
            printDpMatrix(original, choice.word);
            List<String> edits = new ArrayList<>();
            for (Levenshtein.Operation op : Levenshtein.operations(original, choice.word)) {
                if (!op.type.equals("keep")) {
                    edits.add(op.toString());
                }
            }
            System.out.println("  Edits: " + edits);
        }

        section("Output");
        kv("Suggested correction", green(quote(correction.corrected)));
        kv("Edit distance", Levenshtein.distance(query.toLowerCase(Locale.ROOT), correction.corrected)
                + "  (whole query vs corrected query)");
        Levenshtein.Suggestion known = Levenshtein.bestMatchingQuery(query, CityData.SERVICE_REQUESTS);
        int limit = Levenshtein.maxAllowedEdits(query);
        kv("Closest known request", known.distance <= limit
                ? quote(known.word) + "  (distance " + known.distance + ")"
                : "none within " + limit + " edits of the whole query");

        section("More misspelled citizen queries");
        for (String q : CityData.MISSPELLED_QUERIES) {
            Levenshtein.Correction c = Levenshtein.correctQuery(q, vocabulary);
            System.out.printf("  %-22s -> %-24s (distance %d)%n", quote(q), quote(c.corrected),
                    Levenshtein.distance(q, c.corrected));
        }

        complexity("O(n * m) per comparison", "O(n * m)");
    }

    private static void printDpMatrix(String source, String target) {
        int[][] dp = Levenshtein.table(source, target);
        StringBuilder header = new StringBuilder("         \"\"");
        for (int j = 0; j < target.length(); j++) {
            header.append(String.format("%3s", target.charAt(j)));
        }
        System.out.println(header);
        for (int i = 0; i <= source.length(); i++) {
            StringBuilder row = new StringBuilder(i == 0 ? "      \"\"" : String.format("  %6s", source.charAt(i - 1)));
            for (int j = 0; j <= target.length(); j++) {
                row.append(String.format("%3d", dp[i][j]));
            }
            System.out.println(row);
        }
    }

    // =================================================== 8. Bitmask DP
    private static void demoBitmask() {
        title(8, "BITMASK RESOURCE OPTIMIZATION", "Module 3 | CO3", "Emergency resource dispatch");
        List<Resource> resources = CityData.CITY_RESOURCES;
        int n = resources.size();
        int budget = askInt("Budget in lakh rupees (0-" + MAX_BUDGET + ")", CityData.RESOURCE_BUDGET, 0, MAX_BUDGET);

        section("Resources   (each resource is one bit of the mask)");
        printResources(resources);
        section("Problem");
        System.out.println("  One resource leaves per time slot. Sent in slot t, a resource gives value - urgency * t.");
        System.out.println("  Choose resources AND order: total cost <= budget (" + budget
                + "), prerequisites first, maximum benefit.");
        System.out.println("  dp[mask] = best benefit after sending exactly the resources in mask; next slot = popcount(mask)");
        System.out.println("  dp[mask | 1<<r] = max(dp[mask | 1<<r], dp[mask] + value[r] - urgency[r] * popcount(mask))");

        BitmaskDP.Result result = BitmaskDP.optimize(resources, budget);
        section("DP table over all " + (1 << n) + " masks");
        System.out.println("  mask  resources                                      cost  dp[mask]");
        for (int mask = 0; mask < (1 << n); mask++) {
            String names = mask == 0 ? "(none)" : String.join(", ", BitmaskDP.maskToNames(mask, resources));
            String value;
            if (result.reachable(mask)) {
                value = String.valueOf(result.dp[mask]);
            } else if (result.cost[mask] > budget) {
                value = "-  over budget";
            } else {
                value = "-  prerequisite missing";
            }
            System.out.printf("  %s  %-46s %4d  %s%s%n", BitmaskDP.toBinary(mask, n), names, result.cost[mask], value,
                    mask == result.bestMask ? green("   <= BEST") : "");
        }

        section("Optimal combination");
        kv("Best bitmask", BitmaskDP.toBinary(result.bestMask, n) + " (decimal " + result.bestMask + ")");
        kv("Selected resources", BitmaskDP.maskToNames(result.bestMask, resources));
        if (result.order.isEmpty()) {
            System.out.println("  Dispatch order: nothing can be sent within a budget of " + budget + ".");
        } else {
            System.out.println("  Dispatch order:");
        }
        for (int slot = 0; slot < result.order.size(); slot++) {
            Resource r = resources.get(result.order.get(slot));
            System.out.printf("    slot %d: %-11s benefit %d - %d*%d = %d%n", slot, r.name, r.value, r.urgency, slot,
                    BitmaskDP.gain(r, slot));
        }
        kv("Total cost", result.totalCost + " of budget " + budget);
        kv("Total benefit", green(String.valueOf(result.totalBenefit)));

        section("Why DP?");
        long sequences = 0;
        long perm = 1;
        for (int k = 0; k <= n; k++) {
            sequences += perm;       // n! / (n-k)! ordered selections of size k
            perm *= (n - k);
        }
        kv("DP states", (1 << n) + " masks, " + result.transitions + " valid transitions evaluated");
        kv("Brute force", sequences + " ordered selections would be tried (grows like n!)");

        complexity("O(2^n * n)", "O(2^n)");
    }

    // ================================================== 9. Matrix Chain
    private static void demoMatrixChain() {
        title(9, "MATRIX-CHAIN OPTIMIZATION", "Module 3 | CO3", "Analytics pipeline ordering");
        int[] dims = null;
        while (dims == null && Terminal.canAsk()) {
            String typed = ask("Matrix dimensions such as 10,20,30 (Enter = the analytics pipeline)", "");
            if (typed.isEmpty()) {
                break;
            }
            dims = parseDims(typed); // null (after explaining why) when the input is not usable
        }
        String[] operations = null;
        if (dims == null) {
            dims = MatrixChain.dimsFromShapes(CityData.ANALYTICS_OPERATIONS, CityData.ANALYTICS_SHAPES);
            operations = CityData.ANALYTICS_OPERATIONS;
        }
        int n = dims.length - 1;
        String[] names = MatrixChain.defaultNames(n);

        section("Operations as matrices");
        printMatrices(names, operations, dims);
        kv("dims", Arrays.toString(dims));

        section("Recurrence");
        System.out.println("  m[i][i] = 0");
        System.out.println("  m[i][j] = min over i <= k < j of  m[i][k] + m[k+1][j] + p(i-1) * p(k) * p(j)");
        System.out.println("  s[i][j] = the k that gives the minimum (where the last multiplication happens)");

        MatrixChain.Result result = MatrixChain.solve(dims);
        section("DP cost table m[i][j]  (minimum scalar multiplications for Ai..Aj)");
        printTable(n, (i, j) -> i > j ? "-" : String.valueOf(result.cost[i][j]));
        section("Split table s[i][j]  (1-based k)");
        printTable(n, (i, j) -> i >= j ? "-" : String.valueOf(result.split[i][j] + 1));

        section("Output");
        String order = MatrixChain.parenthesization(result.split, 0, n - 1, names);
        long best = result.minimumCost();
        long naive = MatrixChain.leftToRightCost(dims);
        kv("Minimum cost", green(best + " scalar multiplications"));
        kv("Optimal order", order);
        if (operations != null) {
            String withNames = order;
            for (int i = n - 1; i >= 0; i--) {
                withNames = replaceWord(withNames, names[i], operations[i]);
            }
            kv("In pipeline terms", withNames);
        }
        kv("Left-to-right cost", naive);
        if (naive > 0) {
            kv("Saving", String.format("%d fewer multiplications (%.1f%%)", naive - best, 100.0 * (naive - best) / naive));
        }

        complexity("O(n^3)", "O(n^2)");
    }

    private interface Cell {
        String at(int i, int j);
    }

    /** Print an n x n table; the columns widen to fit the largest value. */
    private static void printTable(int n, Cell cell) {
        int width = 9;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                width = Math.max(width, cell.at(i, j).length() + 2);
            }
        }
        String format = "%" + width + "s";
        StringBuilder header = new StringBuilder("        ");
        for (int j = 0; j < n; j++) {
            header.append(String.format(format, "j=" + (j + 1)));
        }
        System.out.println(header);
        for (int i = 0; i < n; i++) {
            StringBuilder row = new StringBuilder(String.format("  i=%-3d ", i + 1));
            for (int j = 0; j < n; j++) {
                row.append(String.format(format, cell.at(i, j)));
            }
            System.out.println(row);
        }
    }

    /**
     * Read matrix dimensions typed as "10,20,30", "10 20 30" or "10x20x30"
     * (scanned by hand). Returns null, after explaining why, when the input
     * cannot be used.
     */
    private static int[] parseDims(String s) {
        List<Integer> values = new ArrayList<>();
        int current = -1; // -1 = not inside a number
        for (int i = 0; i <= s.length(); i++) {
            char ch = i < s.length() ? s.charAt(i) : ',';
            if (ch >= '0' && ch <= '9') {
                current = Math.max(current, 0) * 10 + (ch - '0');
                if (current > MAX_DEMO_DIMENSION) {
                    Terminal.error("Each dimension must be from 1 to " + MAX_DEMO_DIMENSION + ".");
                    return null;
                }
            } else if (ch == ',' || ch == ' ' || ch == 'x' || ch == 'X') {
                if (current >= 0) {
                    values.add(current);
                    current = -1;
                }
            } else {
                Terminal.error("Please type whole numbers separated by commas, for example 10,20,30.");
                return null;
            }
        }
        if (values.size() < 2 || values.size() > MAX_DEMO_NUMBERS) {
            Terminal.error("Please type 2 to " + MAX_DEMO_NUMBERS + " numbers: n matrices need n + 1 dimensions.");
            return null;
        }
        if (values.contains(0)) {
            Terminal.error("Each dimension must be from 1 to " + MAX_DEMO_DIMENSION + ".");
            return null;
        }
        int[] dims = new int[values.size()];
        for (int i = 0; i < dims.length; i++) {
            dims[i] = values.get(i);
        }
        return dims;
    }

    // ================================================== output helpers
    private static void complexity(String time, String space) {
        section("Complexity");
        kv("Time", time);
        kv("Space", space);
    }

    /** Print index / character / value rows, wrapped every 16 columns. */
    private static void printArrayRows(String s, String label, int[] values) {
        if (s.isEmpty()) {
            System.out.println("  (empty)");
            return;
        }
        for (int start = 0; start < s.length(); start += 16) {
            int end = Math.min(s.length(), start + 16);
            StringBuilder idx = new StringBuilder("  index :");
            StringBuilder chars = new StringBuilder("  char  :");
            StringBuilder vals = new StringBuilder(String.format("  %-6s:", label));
            for (int i = start; i < end; i++) {
                char ch = s.charAt(i);
                idx.append(String.format("%4d", i));
                chars.append(String.format("%4s", ch == ' ' ? "_" : String.valueOf(ch)));
                vals.append(String.format("%4d", values[i]));
            }
            System.out.println(idx);
            System.out.println(chars);
            System.out.println(vals);
            if (end < s.length()) {
                System.out.println();
            }
        }
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == ' ') {
                System.out.println("  (_ = space)");
                break;
            }
        }
    }

    /** A line with ^ under every matched character. */
    private static String markers(int length, List<Integer> positions, int m) {
        char[] line = new char[length];
        Arrays.fill(line, ' ');
        int end = 0;
        for (int p : positions) {
            for (int k = p; k < p + m && k < length; k++) {
                line[k] = '^';
                end = Math.max(end, k + 1);
            }
        }
        return new String(line, 0, end);
    }

    /** Text before the first space (manual scan). */
    private static String firstWord(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length() && text.charAt(i) != ' '; i++) {
            sb.append(text.charAt(i));
        }
        return sb.toString();
    }

    /** Replace whole-token occurrences of `word` (e.g. "A1" but not inside "A12"). */
    private static String replaceWord(String text, String word, String replacement) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            boolean match = i + word.length() <= text.length();
            for (int k = 0; match && k < word.length(); k++) {
                match = text.charAt(i + k) == word.charAt(k);
            }
            match = match && (i + word.length() == text.length() || !Character.isDigit(text.charAt(i + word.length())));
            if (match) {
                out.append(replacement);
                i += word.length();
            } else {
                out.append(text.charAt(i));
                i++;
            }
        }
        return out.toString();
    }
}
