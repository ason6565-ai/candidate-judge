package candidate.judge.util;

/**
 * Parse judge vote output.
 */
public class VoteParser {
    public static int parse(String verdict, int max) {
        if (verdict == null) return -1;
        verdict = verdict.trim();
        try {
            int n = Integer.parseInt(verdict.replaceAll("[^0-9]", ""));
            if (n < 0 || n >= max) return -1;
            return n;
        } catch (Exception e) { return -1; }
    }
}
