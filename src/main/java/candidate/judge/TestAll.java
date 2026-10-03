package candidate.judge;

import java.util.*;

/**
 * Run all test scenarios. No API key needed.
 * Compile: javac src/main/java/candidate/judge/*.java src/main/java/candidate/judge/*/*.java
 * Run:     java -cp src/main/java candidate.judge.TestAll
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

        System.out.println();
        System.out.println("=== Results: " + passed + " passed, " + failed + " failed ===");
    }

    static void check(String name, boolean cond) {
        if (cond) { passed++; System.out.println("  PASS: " + name); }
        else { failed++; System.out.println("  FAIL: " + name); }
    }

    // 1. only 1 candidate -> should return it directly
    static void testSingleCandidate() {
        System.out.println("[Test 1] single candidate");
        LlmCaller judge = (s,u) -> new LlmCaller.Result("0", 10, 5);
        List<LlmCaller.Candidate> cands = List.of(
            new LlmCaller.Candidate("hello", "gen-0", 100, 50));
        CandidateJudge.Report r = CandidateJudge.vote(judge, cands, 3);
        check("winner is the only candidate", r.winner.text.equals("hello"));
        check("all votes go to [0]", r.votes[0] == 3);
    }

    // 2. 4 candidates, 3 judges always pick #2
    static void testBasicVote() {
        System.out.println("[Test 2] basic 4-candidate vote");
        LlmCaller judge = (s,u) -> new LlmCaller.Result("2", 10, 5);
        List<LlmCaller.Candidate> cands = makeCands(4);
        CandidateJudge.Report r = CandidateJudge.vote(judge, cands, 3);
        check("winner index is 2", r.winner.source.equals("gen-2"));
        check("candidate 2 has 3 votes", r.votes[2] == 3);
    }

    // 3. all judges return garbage -> abstain -> fallback to first
    static void testAllAbstain() {
        System.out.println("[Test 3] all judges abstain");
        LlmCaller badJudge = (s,u) -> new LlmCaller.Result("I don't know", 10, 5);
        List<LlmCaller.Candidate> cands = makeCands(3);
        CandidateJudge.Report r = CandidateJudge.vote(badJudge, cands, 3);
        check("all abstained", r.abstains == 3);
        check("fallback to first candidate", r.winner.source.equals("gen-0"));
    }

    // 4. 3 different judges, each votes differently
    static void testMultiModelJudges() {
        System.out.println("[Test 4] multi-model judges");
        LlmCaller j0 = (s,u) -> new LlmCaller.Result("0", 10, 5);
        LlmCaller j1 = (s,u) -> new LlmCaller.Result("1", 10, 5);
        LlmCaller j2 = (s,u) -> new LlmCaller.Result("0", 10, 5); // 2 votes for #0
        List<LlmCaller.Candidate> cands = makeCands(3);
        CandidateJudge.Report r = CandidateJudge.voteMulti(List.of(j0, j1, j2), cands);
        check("#0 has 2 votes", r.votes[0] == 2);
        check("#1 has 1 vote", r.votes[1] == 1);
        check("winner is #0", r.winner.source.equals("gen-0"));
    }

    // 5. position rotation: judge 0 sees [0,1,2], judge 1 sees [1,2,0], judge 2 sees [2,0,1]
    //    all judges say "0" (first in their view) -> votes should be spread
    static void testPositionRotation() {
        System.out.println("[Test 5] position rotation");
        LlmCaller alwaysFirst = (s,u) -> new LlmCaller.Result("0", 10, 5);
        List<LlmCaller.Candidate> cands = makeCands(3);
        CandidateJudge.Report r = CandidateJudge.voteMulti(
            List.of(alwaysFirst, alwaysFirst, alwaysFirst), cands);
        // judge 0 picks rotated[0] = cand[0]
        // judge 1 picks rotated[0] = cand[1]
        // judge 2 picks rotated[0] = cand[2]
        check("vote spread across candidates (no positional bias)",
            r.votes[0] == 1 && r.votes[1] == 1 && r.votes[2] == 1);
    }

    // 6. empty candidate list -> no crash
    static void testNoCandidates() {
        System.out.println("[Test 6] empty candidates");
        LlmCaller judge = (s,u) -> new LlmCaller.Result("0", 10, 5);
        CandidateJudge.Report r = CandidateJudge.vote(judge, List.of(), 3);
        check("no crash, winner null", r.winner == null);
    }

    static List<LlmCaller.Candidate> makeCands(int n) {
        List<LlmCaller.Candidate> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(new LlmCaller.Candidate("cand-" + i, "gen-" + i, 100 + i*10, 50));
        }
        return list;
    }
}
