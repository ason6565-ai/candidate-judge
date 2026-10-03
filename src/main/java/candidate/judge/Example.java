package candidate.judge;

import java.util.*;

/**
 * Minimal runnable example. Mock LlmCaller, no API key needed.
 */
public class Example {

    public static void main(String[] args) {
        LlmCaller gen = (sys, user) ->
            new LlmCaller.Result("rewritten: " + user.hashCode(), 120 + new Random().nextInt(80), 50);

        LlmCaller judge = (sys, user) ->
            new LlmCaller.Result("1", 80, 20);

        List<LlmCaller.Candidate> cands =
            CandidateJudge.generate(gen, "Rewrite naturally", "Hello world", 4);

        System.out.println("Generated " + cands.size() + " candidates:");
        for (int i = 0; i < cands.size(); i++) {
            LlmCaller.Candidate c = cands.get(i);
            System.out.println("  [" + i + "] " + c.text + " (" + c.source + ", " + c.latencyMs + "ms)");
        }

        CandidateJudge.Report rep = CandidateJudge.vote(judge, cands, 3);
        System.out.println();
        System.out.println(rep.toString());
        System.out.println("Winner: " + rep.winner.text);
    }
}
