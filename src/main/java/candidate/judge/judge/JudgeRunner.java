package candidate.judge.judge;

import candidate.judge.LlmCaller;
import candidate.judge.util.VoteParser;
import candidate.judge.vote.VoteTally;
import java.util.*;

/**
 * Run judges against candidates, with position rotation.
 */
public class JudgeRunner {

    public static VoteTally.TallyResult run(List<LlmCaller> judges, List<LlmCaller.Candidate> candidates) {
        List<Integer> votes = new ArrayList<>();
        int abstains = 0;
        long latencyMs = 0;
        int tokens = 0;

        for (LlmCaller.Candidate c : candidates) {
            latencyMs += c.latencyMs;
            tokens += c.tokens;
        }

        for (int j = 0; j < judges.size(); j++) {
            List<LlmCaller.Candidate> rotated = new ArrayList<>();
            for (int i = 0; i < candidates.size(); i++) {
                rotated.add(candidates.get((i + j) % candidates.size()));
            }

            StringBuilder sb = new StringBuilder("Here are " + rotated.size() + " candidates. Pick the best, return only the number:\n");
            for (int i = 0; i < rotated.size(); i++) {
                sb.append(i).append(". ").append(rotated.get(i).text).append("\n");
            }

            long t0 = System.currentTimeMillis();
            LlmCaller.Result r = judges.get(j).call("You are a quality judge.", sb.toString());
            latencyMs += (System.currentTimeMillis() - t0);
            if (r != null) tokens += r.tokens;

            int pick = VoteParser.parse(r != null ? r.text : null, rotated.size());
            if (pick < 0) { abstains++; continue; }
            int origIdx = (pick + j) % candidates.size();
            votes.add(origIdx);
        }

        return VoteTally.tally(candidates.size(), votes, abstains, latencyMs, tokens);
    }
}
