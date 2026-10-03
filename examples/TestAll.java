import candidate.judge.*;
import java.util.*;

/**
 * Run all test scenarios. No API key needed.
 * Compile: javac -cp src/main/java examples/TestAll.java
 * Run:     java -cp src/main/java;examples TestAll
 */
public class TestAll {
    static int passed = 0, failed = 0;

    public static void main(String[] args) {
        testSingleCandidate();
        testBasicVote();
        testAllAbstain();
        testMultiModelJudges();
        testPositionRotation();
        testNoCandidates();
        System.out.println("\n=== " + passed + " passed, " + failed + " failed ===");
    }

    static void check(String name, boolean cond) {
        if (cond) { passed++; System.out.println("  PASS: " + name); }
        else { failed++; System.out.println("  FAIL: " + name); }
    }

    static void testSingleCandidate() {
        System.out.println("[Test 1] single candidate");
        LlmCaller judge = (s,u) -> new LlmCaller.Result("0", 10, 5);
        List<LlmCaller.Candidate> cands = List.of(
            new LlmCaller.Candidate("hello", "gen-0", 100, 50));
        CandidateJudge.Report r = CandidateJudge.vote(judge, cands, 3);
        check("winner is the only candidate", r.winner.text.equals("hello"));
        check("all votes go to [0]", r.votes[0] == 3);
    }

    static void testBasicVote() {
        System.out.println("[Test 2] basic 4-candidate vote");
        LlmCaller judge = (s,u) -> new LlmCaller.Result("2", 10, 5);
        List<LlmCaller.Candidate> cands = makeCands(4);
        CandidateJudge.Report r = CandidateJudge.vote(judge, cands, 3);
        check("winner is gen-2", r.winner.source.equals("gen-2"));
        check("candidate 2 has 3 votes", r.votes[2] == 3);
    }

    static void testAllAbstain() {
        System.out.println("[Test 3] all judges abstain");
        LlmCaller bad = (s,u) -> new LlmCaller.Result("I don't know", 10, 5);
        List<LlmCaller.Candidate> cands = makeCands(3);
        CandidateJudge.Report r = CandidateJudge.vote(bad, cands, 3);
        check("all abstained", r.abstains == 3);
        check("fallback to first", r.winner.source.equals("gen-0"));
    }

    static void testMultiModelJudges() {
        System.out.println("[Test 4] multi-model judges");
        LlmCaller j0 = (s,u) -> new LlmCaller.Result("0", 10, 5);
        LlmCaller j1 = (s,u) -> new LlmCaller.Result("1", 10, 5);
        LlmCaller j2 = (s,u) -> new LlmCaller.Result("0", 10, 5);
        List<LlmCaller.Candidate> cands = makeCands(3);
        CandidateJudge.Report r = CandidateJudge.voteMulti(List.of(j0, j1, j2), cands);
        check("#0 has 2 votes", r.votes[0] == 2);
        check("#1 has 1 vote", r.votes[1] == 1);
        check("winner is #0", r.winner.source.equals("gen-0"));
    }

    static void testPositionRotation() {
        System.out.println("[Test 5] position rotation");
        LlmCaller alwaysFirst = (s,u) -> new LlmCaller.Result("0", 10, 5);
        List<LlmCaller.Candidate> cands = makeCands(3);
        CandidateJudge.Report r = CandidateJudge.voteMulti(
            List.of(alwaysFirst, alwaysFirst, alwaysFirst), cands);
        check("vote spread (no positional bias)",
            r.votes[0] == 1 && r.votes[1] == 1 && r.votes[2] == 1);
    }

    static void testNoCandidates() {
        System.out.println("[Test 6] empty candidates");
        LlmCaller judge = (s,u) -> new LlmCaller.Result("0", 10, 5);
        CandidateJudge.Report r = CandidateJudge.vote(judge, List.of(), 3);
        check("no crash, winner null", r.winner == null);
    }

    static List<LlmCaller.Candidate> makeCands(int n) {
        List<LlmCaller.Candidate> list = new ArrayList<>();
        for (int i = 0; i < n; i++)
            list.add(new LlmCaller.Candidate("cand-" + i, "gen-" + i, 100+i*10, 50));
        return list;
    }
}
