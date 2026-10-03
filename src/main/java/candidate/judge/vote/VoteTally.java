package candidate.judge.vote;

import candidate.judge.LlmCaller;
import java.util.*;

/**
 * Tally votes and pick winner.
 */
public class VoteTally {

    public static class TallyResult {
        public int[] votes;
        public int abstains;
        public long totalLatencyMs;
        public int totalTokens;
        public int winnerIndex = -1;
    }

    public static TallyResult tally(int numCandidates, List<Integer> voteResults, int abstainCount,
                                     long latencyMs, int tokens) {
        TallyResult r = new TallyResult();
        r.votes = new int[numCandidates];
        r.abstains = abstainCount;
        r.totalLatencyMs = latencyMs;
        r.totalTokens = tokens;

        for (int v : voteResults) {
            if (v >= 0 && v < numCandidates) r.votes[v]++;
        }

        int totalJudges = voteResults.size() + abstainCount;
        if (abstainCount == totalJudges) {
            r.winnerIndex = 0;
            return r;
        }

        int best = 0;
        for (int i = 1; i < r.votes.length; i++) {
            if (r.votes[i] > r.votes[best]) best = i;
        }
        r.winnerIndex = best;
        return r;
    }
}
