package ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Locale;

/**
 * Terminal input and output helpers for the SmartCityX command-line program.
 *
 * Everything the menu and the demonstrations print or ask goes through this
 * class, so the look of the program (rules, headings, prompts, colors) is
 * defined in one place.
 *
 *   Output : title, section, kv, note, error
 *   Input  : ask, askShort, askInt, pause
 *            Every prompt accepts Enter for its sample value and copes with
 *            the end of the input (Ctrl+D, or input piped from a file).
 *   Colors : used only when the program writes to a real terminal. They are
 *            switched off when the output is piped or redirected, when the
 *            NO_COLOR environment variable is set, or with --no-color.
 */
public final class Terminal {

    /** Width of the rules drawn around titles and menus. */
    public static final int WIDTH = 76;

    private static final String LINE = repeat('=', WIDTH);
    private static final String THIN = repeat('-', WIDTH);
    private static final BufferedReader INPUT = new BufferedReader(new InputStreamReader(System.in));

    /** false while demonstrations run unattended, so every prompt returns its sample value. */
    private static boolean interactive = true;
    private static boolean endOfInput = false;
    private static boolean color = detectColor();

    private Terminal() {
    }

    // ================================================================= modes
    public static void setInteractive(boolean value) {
        interactive = value;
    }

    public static void setColor(boolean value) {
        color = value;
    }

    /** true while the user can still be asked something. */
    public static boolean canAsk() {
        return interactive && !endOfInput;
    }

    private static boolean detectColor() {
        if (System.getenv("NO_COLOR") != null || System.console() == null) {
            return false; // switched off by the user, or the output is piped / redirected
        }
        String term = System.getenv("TERM");
        if ("dumb".equals(term)) {
            return false;
        }
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows")) {
            // Windows Terminal and the VS Code terminal understand ANSI codes; the old console may not.
            return System.getenv("WT_SESSION") != null || "vscode".equals(System.getenv("TERM_PROGRAM"));
        }
        return term != null;
    }

    // ================================================================ colors
    private static String style(String code, String text) {
        return color ? "\u001B[" + code + "m" + text + "\u001B[0m" : text;
    }

    public static String bold(String text) {
        return style("1", text);
    }

    public static String heading(String text) {
        return style("1;36", text); // bold cyan
    }

    public static String green(String text) {
        return style("32", text);
    }

    public static String red(String text) {
        return style("31", text);
    }

    public static String yellow(String text) {
        return style("33", text);
    }

    // ================================================================ output
    public static void rule() {
        System.out.println(heading(LINE));
    }

    public static void thinRule() {
        System.out.println(THIN);
    }

    /** Heading of one demonstration. */
    public static void title(int number, String name, String module, String purpose) {
        System.out.println();
        rule();
        System.out.println(heading(" [" + number + "] " + name));
        System.out.println("     " + module + "  |  " + purpose);
        rule();
    }

    /** Heading of one part of a demonstration, e.g. "--- Input -----". */
    public static void section(String name) {
        System.out.println();
        System.out.println(bold("--- " + name + " " + repeat('-', Math.max(3, 70 - name.length()))));
    }

    /** One "key : value" line. */
    public static void kv(String key, Object value) {
        System.out.printf("  %-22s: %s%n", key, value);
    }

    public static void note(String message) {
        System.out.println("  " + yellow(message));
    }

    public static void error(String message) {
        System.out.println("  " + red(message));
    }

    // ================================================================= input
    /** Print the prompt and read one line. Returns null at the end of the input. */
    public static String readLine(String prompt) {
        if (endOfInput) {
            return null;
        }
        System.out.print(prompt);
        System.out.flush();
        try {
            String line = INPUT.readLine();
            if (line == null) {
                endOfInput = true;
                System.out.println();
            }
            return line;
        } catch (IOException e) {
            endOfInput = true;
            return null;
        }
    }

    /** Ask a question. Enter (or unattended mode) keeps the sample value. */
    public static String ask(String question, String sample) {
        if (!canAsk()) {
            return sample;
        }
        String hint = sample.isEmpty() ? "" : " [Enter = " + shorten(sample, 40) + "]";
        String answer = readLine("  " + heading(">") + " " + question + hint + ": ");
        if (answer == null) {
            return sample;
        }
        answer = answer.trim();
        return answer.isEmpty() ? sample : answer;
    }

    /** Like ask, but repeats the question until the answer has at most maxLength characters. */
    public static String askShort(String question, String sample, int maxLength) {
        while (canAsk()) {
            String answer = ask(question, sample);
            if (answer.length() <= maxLength) {
                return answer;
            }
            error("That is " + answer.length() + " characters. Please use at most " + maxLength
                    + " so the table stays readable.");
        }
        return sample;
    }

    /** Ask for a whole number from min to max, repeating the question until it is valid. */
    public static int askInt(String question, int sample, int min, int max) {
        while (canAsk()) {
            String answer = ask(question, String.valueOf(sample));
            try {
                int value = Integer.parseInt(answer);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // not a number: explain below and ask again
            }
            error("Please enter a whole number from " + min + " to " + max + ", or press Enter for " + sample + ".");
        }
        return sample;
    }

    public static void pause() {
        if (canAsk()) {
            readLine("\nPress Enter to return to the menu...");
        }
    }

    // ========================================================== text helpers
    public static String quote(String text) {
        return "\"" + text + "\"";
    }

    /** Cut text to at most max characters, ending with "..." when it was cut. */
    public static String shorten(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }

    public static String repeat(char ch, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, ch);
        return new String(chars);
    }
}
