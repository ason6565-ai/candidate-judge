package candidate.judge;

import java.util.*;

/**
 * Multi-candidate generation + AI judge voting for any LLM task.
 * Pure Java 8+, zero dependencies.
 *
 * Each candidate can come from a DIFFERENT model.
 * Each judge can be a DIFFERENT model.
 */
public class CandidateJudge {

    public interface LlmCaller {
        Result call(String systemPrompt, String userPrompt);
    }

    public static class Result {
        public final String text;
        public final long latencyMs;
        public final int tokens;
        public Result(String text, long latencyMs, int tokens) {
            this.text = text; this.latencyMs = latencyMs; this.tokens = tokens;
        }
    }

    public static class Candidate {
        public final String text;
        public final String source;   // which model / generator
        public final long latencyMs;
        public final int tokens;
        public Candidate(String text, String source, long latencyMs, int tokens) {
            this.text = text; this.source = source; this.latencyMs = latencyMs; this.tokens = tokens;
        }
    }

    public static class Report {
        public Candidate winner;
        public int[] votes;
        public int abstains;
        public long totalLatencyMs;
        public int totalTokens;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Judge Report ===\n");
            for (int i = 0; i < votes.length; i++) {
                sb.append(String.format("  [#%d] votes=%d%n", i, votes[i]));
            }
            sb.append(String.format("  abstains: %d%n", abstains));
            sb.append(String.format("  total: %dms, %d tokens%n", totalLatencyMs, totalTokens));
            sb.append(String.format("  winner: %s%n", winner != null ? winner.source : "(none)"));
            return sb.toString();
        }
    }

    // ===== Generate: single generator, N repeats =====
    public static List<Candidate> generateCandidates(LlmCaller gen, String prompt, String orig, int n) {
        List<LlmCaller> gens = new ArrayList<>();
        for (int i = 0; i < n; i++) gens.add(gen);
        return generateCandidatesMulti(gens, prompt, orig);
    }

    // ===== Generate: each candidate from a DIFFERENT model =====
    public static List<Candidate> generateCandidatesMulti(List<LlmCaller> gens, String prompt, String orig) {
        List<Candidate> out = new ArrayList<>();
        for (int i = 0; i < gens.size(); i++) {
            long t0 = System.currentTimeMillis();
            Result r = gens.get(i).call("You are a text assistant.", prompt + "\nInput:" + orig);
            if (r != null && r.text != null && !r.text.trim().isEmpty()) {
                out.add(new Candidate(r.text.trim(), "gen-" + i, r.latencyMs, r.tokens));
            }
        }
        return out;
    }

    // ===== Vote: single judge, K repeats =====
    public static Report judgeVote(LlmCaller judge, List<Candidate> candidates, int k) {
        List<LlmCaller> judges = new ArrayList<>();
        for (int i = 0; i < k; i++) judges.add(judge);
        return judgeVoteMulti(judges, candidates);
    }

    // ===== Vote: each judge is a DIFFERENT model =====
    public static Report judgeVoteMulti(List<LlmCaller> judges, List<Candidate> candidates) {
        Report rep = new Report();
        rep.votes = new int[candidates.size()];

        if (candidates.isEmpty()) return rep;
        if (candidates.size() == 1) {
            rep.winner = candidates.get(0);
            rep.votes[0] = judges.size();
            return rep;
        }

        for (Candidate c : candidates) {
            rep.totalLatencyMs += c.latencyMs;
            rep.totalTokens += c.tokens;
        }

        for (int j = 0; j < judges.size(); j++) {
            // position rotation: judge j sees candidates shifted by j
            List<Candidate> rotated = new ArrayList<>();
            for (int i = 0; i < candidates.size(); i++) {
                rotated.add(candidates.get((i + j) % candidates.size()));
            }

            StringBuilder sb = new StringBuilder("Here are " + rotated.size() + " candidates. Pick the best, return only the number:\n");
            for (int i = 0; i < rotated.size(); i++) {
                sb.append(i).append(". ").append(rotated.get(i).text).append("\n");
            }

            long t0 = System.currentTimeMillis();
            Result r = judges.get(j).call("You are a quality judge.", sb.toString());
            rep.totalLatencyMs += (System.currentTimeMillis() - t0);
            if (r != null) rep.totalTokens += r.tokens;

            int pick = parseVote(r != null ? r.text : null, rotated.size());
            if (pick < 0) { rep.abstains++; continue; }
            int origIdx = (pick + j) % candidates.size();
            rep.votes[origIdx]++;
        }

        if (rep.abstains == judges.size()) {
            rep.winner = candidates.get(0);
            return rep;
        }

        int best = 0;
        for (int i = 1; i < rep.votes.length; i++) {
            if (rep.votes[i] > rep.votes[best]) best = i;
        }
        rep.winner = candidates.get(best);
        return rep;
    }

    private static int parseVote(String verdict, int max) {
        if (verdict == null) return -1;
        verdict = verdict.trim();
        try {
            int n = Integer.parseInt(verdict.replaceAll("[^0-9]", ""));
            if (n < 0 || n >= max) return -1;
            return n;
        } catch (Exception e) { return -1; }
    }
}
