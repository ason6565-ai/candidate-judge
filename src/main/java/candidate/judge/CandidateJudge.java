package candidate.judge;

import candidate.judge.generator.CandidateGenerator;
import candidate.judge.judge.JudgeRunner;
import candidate.judge.vote.VoteTally;
import java.util.*;

/**
 * Multi-candidate generation + AI judge voting.
 * Facade: calls generator / judge / vote packages.
 */
public class CandidateJudge {

    public static class Report {
        public LlmCaller.Candidate winner;
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

    // ===== Generate =====
    public static List<LlmCaller.Candidate> generate(LlmCaller gen, String prompt, String orig, int n) {
        return CandidateGenerator.generate(gen, prompt, orig, n);
    }

    public static List<LlmCaller.Candidate> generateMulti(List<LlmCaller> gens, String prompt, String orig) {
        return CandidateGenerator.generateMulti(gens, prompt, orig);
    }

    // ===== Vote =====
    public static Report vote(LlmCaller judge, List<LlmCaller.Candidate> candidates, int k) {
        List<LlmCaller> judges = new ArrayList<>();
        for (int i = 0; i < k; i++) judges.add(judge);
        return voteMulti(judges, candidates);
    }

    public static Report voteMulti(List<LlmCaller> judges, List<LlmCaller.Candidate> candidates) {
        Report rep = new Report();
        if (candidates.isEmpty()) return rep;

        if (candidates.size() == 1) {
            rep.winner = candidates.get(0);
            rep.votes = new int[]{judges.size()};
            return rep;
        }

        VoteTally.TallyResult t = JudgeRunner.run(judges, candidates);
        rep.votes = t.votes;
        rep.abstains = t.abstains;
        rep.totalLatencyMs = t.totalLatencyMs;
        rep.totalTokens = t.totalTokens;
        rep.winner = candidates.get(t.winnerIndex);
        return rep;
    }
}
