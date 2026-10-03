package candidate.judge.generator;

import candidate.judge.LlmCaller;
import java.util.*;

/**
 * Generate candidates from one or more LLM callers.
 */
public class CandidateGenerator {

    /** Same model, N repeats. */
    public static List<LlmCaller.Candidate> generate(LlmCaller gen, String prompt, String orig, int n) {
        List<LlmCaller> gens = new ArrayList<>();
        for (int i = 0; i < n; i++) gens.add(gen);
        return generateMulti(gens, prompt, orig);
    }

    /** Each candidate from a different model. */
    public static List<LlmCaller.Candidate> generateMulti(List<LlmCaller> gens, String prompt, String orig) {
        List<LlmCaller.Candidate> out = new ArrayList<>();
        for (int i = 0; i < gens.size(); i++) {
            long t0 = System.currentTimeMillis();
            LlmCaller.Result r = gens.get(i).call("You are a text assistant.", prompt + "\nInput:" + orig);
            if (r != null && r.text != null && !r.text.trim().isEmpty()) {
                out.add(new LlmCaller.Candidate(r.text.trim(), "gen-" + i, r.latencyMs, r.tokens));
            }
        }
        return out;
    }
}
