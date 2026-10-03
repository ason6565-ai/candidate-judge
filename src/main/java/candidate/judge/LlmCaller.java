package candidate.judge;

/**
 * LLM call abstraction. Implement this to plug in any LLM endpoint.
 */
public interface LlmCaller {
    Result call(String systemPrompt, String userPrompt);

    class Result {
        public final String text;
        public final long latencyMs;
        public final int tokens;
        public Result(String text, long latencyMs, int tokens) {
            this.text = text; this.latencyMs = latencyMs; this.tokens = tokens;
        }
    }

    class Candidate {
        public final String text;
        public final String source;
        public final long latencyMs;
        public final int tokens;
        public Candidate(String text, String source, long latencyMs, int tokens) {
            this.text = text; this.source = source; this.latencyMs = latencyMs; this.tokens = tokens;
        }
    }
}
