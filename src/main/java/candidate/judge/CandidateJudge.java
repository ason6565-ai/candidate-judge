package candidate.judge;

import java.util.*;

/**
 * Multi-candidate generation + AI judge voting for any LLM task.
 * Pure Java 8+, zero dependencies.
 */
public class CandidateJudge {

    public interface LlmCaller {
        /** returns [output, latencyMs, tokensUsed] */
        Result call(String systemPrompt, String userPrompt);
    }

    public static class Result {
        public final String text;
        public final long latencyMs;
        public final int tokens;

        public Result(String text, long latencyMs, int tokens) {
            this.text = text;
            this.latencyMs = latencyMs;
            this.tokens = tokens;
        }
    }

    public static class Candidate {
        public final String text;
        public final String source;   // which generator / model
        public final long latencyMs;
        public final int tokens;

        public Candidate(String text, String source, long latencyMs, int tokens) {
            this.text = text;
            this.source = source;
            this.latencyMs = latencyMs;
            this.tokens = tokens;
        }
    }

    public static class Report {
        public Candidate winner;
        public int[] votes;           // votes[i] = how many judges picked candidate i
        public int abstains;
        public long totalLatencyMs;   // all calls combined
        public int totalTokens;       // all calls combined

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Candidate Judge Report ===\n");
            for (int i = 0; i < votes.length; i++) {
                Candidate c = null;
                sb.append(String.format("  [#%d] votes=%d source=%s %dms tokens=%d%n",
                    i, votes[i],
                    winner != null && i == indexOf(winner) ? winner.source : "?",
                    i < votes.length ? 0 : 0, 0));
            }
            sb.append(String.format("  abstains: %d%n", abstains));
            sb.append(String.format("  total: %dms, %d tokens%n", totalLatencyMs, totalTokens));
            sb.append(String.format("  winner: %s%n", winner != null ? winner.source : "(none)"));
            return sb.toString();
        }
        private int indexOf(Candidate c) { return -1; }
    }

    public static List<Candidate> generateCandidates(LlmCaller gen, String prompt, String orig, int n) {
        List<Candidate> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            long t0 = System.currentTimeMillis();
            Result r = gen.call("You are a text assistant.", prompt + "\nInput:" + orig);
            if (r != null && r.text != null && !r.text.trim().isEmpty()) {
                out.add(new Candidate(r.text.trim(), "gen-" + i, r.latencyMs, r.tokens));
            }
        }
        return out;
    }

    public static Report judgeVote(LlmCaller judge, List<Candidate> candidates, int k) {
        Report rep = new Report();
        rep.votes = new int[candidates.size()];

        if (candidates.isEmpty()) return rep;
        if (candidates.size() == 1) {
            rep.winner = candidates.get(0);
            rep.votes[0] = k;
            return rep;
        }

        for (Candidate c : candidates) {
            rep.totalLatencyMs += c.latencyMs;
            rep.totalTokens += c.tokens;
        }

        for (int j = 0; j < k; j++) {
            List<Candidate> rotated = new ArrayList<>();
            for (int i = 0; i < candidates.size(); i++) {
                rotated.add(candidates.get((i + j) % candidates.size()));
            }

            StringBuilder sb = new StringBuilder("Here are " + rotated.size() + " candidates. Pick the best, return only the number:\n");
            for (int i = 0; i < rotated.size(); i++) {
                sb.append(i).append(". ").append(rotated.get(i).text).append("\n");
            }

            long t0 = System.currentTimeMillis();
            Result r = judge.call("You are a quality judge.", sb.toString());
            rep.totalLatencyMs += (System.currentTimeMillis() - t0);
            if (r != null) rep.totalTokens += r.tokens;

            int pick = parseVote(r != null ? r.text : null, rotated.size());
            if (pick < 0) { rep.abstains++; continue; }
            int origIdx = (pick + j) % candidates.size();
            rep.votes[origIdx]++;
        }

        if (rep.abstains == k) {
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
        } catch (Exception e) {
            return -1;
        }
    }
}
