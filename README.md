# miao-ai

Multi-candidate generation + AI judge voting for LLM text tasks.

**The problem**: existing LLM libraries (LiteLLM, OpenRouter, LangChain) only solve "call one model, get one result". Nobody solves "call multiple models, get multiple candidates, then have AI judges vote on the best one" — but this is the single most effective way to improve translation/rewrite quality.

## Algorithm

```
Input: prompt + original text
  │
  ├─→ Generator[0] → candidate A
  ├─→ Generator[1] → candidate B     (parallel, position-shifted)
  ├─→ Generator[2] → candidate C
  └─→ Generator[K-1] → candidate D
  │
  ├─→ Judge[0]: A vs B vs C vs D → votes 2
  ├─→ Judge[1]: A vs B vs C vs D → votes 0
  └─→ Judge[K-1]: A vs B vs C vs D → votes 2
  │
  └─→ Tally: candidate 2 gets 2 votes, candidate 0 gets 1 → pick candidate 2
```

### Three key design choices

1. **Position rotation**: each judge sees candidates in a different order (shifted by judge index), preventing positional bias.
2. **Abstain**: when a judge returns invalid/out-of-range/unparseable output, it abstains rather than forcing a wrong vote.
3. **All-abstain fallback**: if every judge abstains, fall back to the first candidate rather than picking randomly.

## Usage

```java
LlmCaller generator = (sys, user) -> httpCall("openai-compatible-endpoint", "gpt-4o", sys, user);
LlmCaller judge     = (sys, user) -> httpCall("openai-compatible-endpoint", "gpt-4o", sys, user);

List<String> candidates = MultiCandidateJudge.generateCandidates(
    generator, "Rewrite the sentence naturally", "Hello world", 4);
String best = MultiCandidateJudge.judgeVote(judge, candidates, 3);
System.out.println("Best: " + best);
```

## Cost

| Setup | Extra calls | Quality gain |
|---|---|---|
| 1 candidate, 1 judge | 1 | baseline |
| 3 candidates, 3 judges | 6 | significant |
| 5 candidates, 5 judges | 10 | diminishing returns |

**Default: 3+3**. Good cost/quality tradeoff. Go 5+5 only for high-stakes output.

## Requirements

- Java 8+
- No dependencies
- Bring your own LLM endpoint (any OpenAI-compatible API works)

## License

MIT
