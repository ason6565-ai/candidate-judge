package candidate.judge;

import java.net.http.*;
import java.net.URI;
import java.time.Duration;

/**
 * Real usage: wire LlmCaller to any OpenAI-compatible endpoint.
 * No HTTP client dependency required — uses Java 11+ built-in HttpClient.
 */
public class RealExample {

    // ---- Plug in your endpoint + key + model here ----
    static final String ENDPOINT = "https://api.your-provider.com/v1/chat/completions";
    static final String API_KEY  = "sk-...";
    static final String MODEL    = "your-model-name";

    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        LlmCaller call = (sys, user) -> {
            String body = "{"
                + "\"model\":\"" + MODEL + "\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + esc(sys) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + esc(user) + "\"}"
                + "],\"temperature\":0.7"
                + "}";

            long t0 = System.currentTimeMillis();
            try {
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT))
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                long ms = System.currentTimeMillis() - t0;

                // crude parse: extract content field
                String json = resp.body();
                String text = extractContent(json);
                int tokens = extractTokens(json);
                return new CandidateJudge.Result(text, ms, tokens);
            } catch (Exception e) {
                return new CandidateJudge.Result("", System.currentTimeMillis() - t0, 0);
            }
        };

        // Run: 4 candidates, 3 judges
        List<CandidateJudge.Candidate> cands =
            CandidateJudge.generateCandidates(call, "Translate to natural English", "你好世界", 4);

        CandidateJudge.Report rep = CandidateJudge.judgeVote(call, cands, 3);
        System.out.println(rep.toString());
        System.out.println("Best translation: " + rep.winner.text);
    }

    static String esc(String s) { return s.replace("\"", "\\\"").replace("\n", "\\n"); }

    static String extractContent(String json) {
        int i = json.indexOf("\"content\":\"");
        if (i < 0) return "";
        i += 11;
        int j = json.indexOf("\"", i);
        return json.substring(i, j).replace("\\n", "\n").replace("\\\"", "\"");
    }

    static int extractTokens(String json) {
        int i = json.indexOf("\"total_tokens\":");
        if (i < 0) i = json.indexOf("\"completion_tokens\":");
        if (i < 0) return 0;
        i += 15;
        int j = i;
        while (j < json.length() && Character.isDigit(json.charAt(j))) j++;
        try { return Integer.parseInt(json.substring(i, j)); } catch (Exception e) { return 0; }
    }
}
