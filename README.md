# candidate-judge

Multi-candidate generation + AI judge voting for any LLM task.
多候选生成 + AI裁判投票，适用于任何大模型任务。

## English

**The problem**: existing LLM libraries only solve "call one model, get one result". Nobody solves "call multiple times, get multiple candidates, then have AI judges vote on the best one" — but this is the single most effective way to improve output quality.

**Works for**: translation, writing, code generation, prompt engineering, or anything where "one answer isn't good enough".

**Pure Java 8+, zero dependencies**. Runs on Android, server JVM, desktop, anywhere. Kotlin compatible.

### Install

Copy `src/main/java/candidate/judge/` into your project. No build tool needed — it's just Java files.

Or via JitPack (coming soon):

```groovy
// Gradle
implementation 'com.github.ason6565-ai:candidate-judge:v0.1.0'
```

```xml
<!-- Maven -->
<dependency>
  <groupId>com.github.ason6565-ai</groupId>
  <artifactId>candidate-judge</artifactId>
  <version>v0.1.0</version>
</dependency>
```

### Quick start

```java
LlmCaller gen   = (sys, user) -> httpCall(endpoint, model, sys, user);
LlmCaller judge = (sys, user) -> httpCall(endpoint, model, sys, user);

List<Candidate> cands = CandidateJudge.generate(gen, "Rewrite naturally", "Hello", 4);
Report rep = CandidateJudge.vote(judge, cands, 3);
System.out.println(rep);
System.out.println("Best: " + rep.winner.text);
```

### Multi-model

Each candidate can come from a different model. Each judge can be a different model:

```java
List<Candidate> cands = CandidateJudge.generateMulti(
    List.of(gpt4oCall, claudeCall, geminiCall, deepseekCall),
    "Translate to English", "你好");

Report rep = CandidateJudge.voteMulti(
    List.of(gpt4oCall, deepseekCall, qwenCall), cands);
```

## 中文

**解决什么问题**：现有的 LLM 库只解决"调一个模型拿一个结果"。没有人解决"多次调用拿多个候选，再用 AI 投票选最好的那个"——但这是提升任何任务输出质量最有效的手段。

**适用于**：翻译、写作、代码生成、提示词工程，或者任何"一个答案不够好"的场景。

**纯 Java 8+，零依赖**。Android、服务端、桌面都能跑。Kotlin 直接用。

### 安装

直接把 `src/main/java/candidate/judge/` 拷进你的项目。不需要构建工具，就是几个 Java 文件。

### 快速开始

```java
LlmCaller gen   = (sys, user) -> httpCall(endpoint, model, sys, user);
LlmCaller judge = (sys, user) -> httpCall(endpoint, model, sys, user);

List<Candidate> cands = CandidateJudge.generate(gen, "改得自然一点", "你好", 4);
Report rep = CandidateJudge.vote(judge, cands, 3);
System.out.println(rep);
```

## Algorithm / 算法

```
Input: task + prompt
  ├─→ gen-0 → A
  ├─→ gen-1 → B     (parallel, position-shifted / 并行，位置轮换)
  ├─→ gen-2 → C
  └─→ gen-3 → D
  │
  ├─→ judge-0: A vs B vs C vs D → vote 2
  ├─→ judge-1: A vs B vs C vs D → vote 0
  └─→ judge-2: A vs B vs C vs D → vote 2
  │
  └─→ candidate 2 wins / 候选2获胜
```

- **Position rotation / 位置轮换**: each judge sees candidates shifted, prevents positional bias
- **Abstain / 弃权**: invalid judge output doesn't force a wrong vote
- **Fallback / 兜底**: all judges abstain → pick first candidate

## Cost / 成本

| Setup | Extra calls | Quality gain |
|---|---|---|
| 1+1 | 1 | baseline / 基准 |
| 3+3 | 6 | significant / 显著 |
| 5+5 | 10 | diminishing / 边际递减 |

Default 3+3. / 默认 3+3。

## Test

```bash
javac src/main/java/candidate/judge/*.java src/main/java/candidate/judge/*/*.java
java -cp src/main/java candidate.judge.TestAll
```

6 scenarios: single candidate, basic vote, all-abstain, multi-model, position rotation, empty list.

## License

0BSD — do whatever you want, no attribution required.
0BSD — 随便用，不需要署名，可商用。
