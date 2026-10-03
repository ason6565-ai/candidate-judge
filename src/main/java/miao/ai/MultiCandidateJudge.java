package miao.ai;

import java.util.*;

/**
 * Multi-candidate generation + AI judge voting.
 * Zero dependency, plain Java.
 */
public class MultiCandidateJudge {

    public interface LlmCaller {
        String call(String systemPrompt, String userPrompt);
    }

    public static List<String> generateCandidates(LlmCaller gen, String prompt, String orig, int n) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String c = gen.call("You are a text rewriting assistant.", prompt + "\nOriginal:" + orig);
            if (c != null && !c.trim().isEmpty()) out.add(c.trim());
        }
        return out;
    }

    public static String judgeVote(LlmCaller judge, List<String> candidates, int k) {
        if (candidates.isEmpty()) return "";
        if (candidates.size() == 1) return candidates.get(0);

        int[] votes = new int[candidates.size()];
        int abstain = 0;

        for (int j = 0; j < k; j++) {
            // position rotation: judge j sees candidates shifted by j
            List<String> rotated = new ArrayList<>();
            for (int i = 0; i < candidates.size(); i++) {
                rotated.add(candidates.get((i + j) % candidates.size()));
            }

            StringBuilder sb = new StringBuilder("Here are " + rotated.size() + " candidates for the same original. Pick the best one, return only the number:\n");
            for (int i = 0; i < rotated.size(); i++) {
                sb.append(i).append(". ").append(rotated.get(i)).append("\n");
            }
            String verdict = judge.call("You are a translation quality judge.", sb.toString());

            int pick = parseVote(verdict, rotated.size());
            if (pick < 0) { abstain++; continue; }
            int origIdx = (pick + j) % candidates.size();
            votes[origIdx]++;
        }

        // all judges abstained -> fallback to first candidate
        if (abstain == k) return candidates.get(0);

        int best = 0;
        for (int i = 1; i < votes.length; i++) {
            if (votes[i] > votes[best]) best = i;
        }
        return candidates.get(best);
    }

    private static int parseVote(String verdict, int max) {
        if (verdict == null) return -1;
        verdict = verdict.trim();
        try {
            int n = Integer.parseInt(verdict.replaceAll("[^0-9]", ""));
            if (n < 0 || n >= max) return -1;
            return n;
        } catch (Exception e) {
            return -1;
        }
    }
}
