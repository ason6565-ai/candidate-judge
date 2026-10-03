package candidate.judge;

import java.util.*;

/**
 * Minimal runnable example.
 * Uses mock LlmCaller so you can test without any API key.
 */
public class Example {

    public static void main(String[] args) {
        // ---- Mock generators: pretend to be 4 different model calls ----
        LlmCaller gen = (sys, user) -> {
            // simulate latency + tokens
            String mock = "rewritten: " + user.hashCode();
            return new CandidateJudge.Result(mock, 120 + new Random().nextInt(80), 50 + new Random().nextInt(30));
        };

        // ---- Mock judge ----
        LlmCaller judge = (sys, user) -> {
            // always pick candidate 1 (0-indexed)
            return new CandidateJudge.Result("1", 80, 20);
        };

        // ---- Generate 4 candidates ----
        List<CandidateJudge.Candidate> cands =
            CandidateJudge.generateCandidates(gen, "Rewrite naturally", "Hello world", 4);

        System.out.println("Generated " + cands.size() + " candidates:");
        for (int i = 0; i < cands.size(); i++) {
            CandidateJudge.Candidate c = cands.get(i);
            System.out.println("  [" + i + "] " + c.text + " (" + c.source + ", " + c.latencyMs + "ms, " + c.tokens + " tok)");
        }

        // ---- 3 judges vote ----
        CandidateJudge.Report rep = CandidateJudge.judgeVote(judge, cands, 3);

        System.out.println();
        System.out.println(rep.toString());
        System.out.println("Winner text: " + rep.winner.text);
    }
}
