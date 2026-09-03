# PayRecover AI — Phase 4 & Phase 5 Guide

> **This is a simulation / prototype.** No real money moves. Payment data is
> synthetic. Recovery actions are simulated only.

Phase 1–3 gave you a working payment backend. These two phases add the numbers
behind the dashboard, and the AI layer that explains *why* a payment failed.

---

## 1. What you now have

| Phase | What it adds | New endpoints |
|---|---|---|
| 4 | Dashboard analytics — group-by counts, per-method failure rates, a daily trend | `/api/dashboard/summary`, `/failure-breakdown`, `/method-breakdown`, `/trend?days=14` |
| 5 | AI diagnosis with a deterministic fallback, plus the audit trail | `POST /api/payments/{id}/analyze`, `GET /api/payments/{id}/diagnosis`, `/api/audit-logs`, `/api/audit-logs/{id}` |

Nothing from Phase 1–3 was rewritten. Every change was additive: new classes,
new methods, new columns on `PaymentResponseDto`.

Full endpoint list after Phase 5:

```
GET  /api/payments                       all payments (now incl. AI columns)
GET  /api/payments/failed                FAILED + FAILED_AGAIN (the main table)
GET  /api/payments/{paymentId}           one payment
POST /api/payments/{paymentId}/analyze   run AI diagnosis, persist it
GET  /api/payments/{paymentId}/diagnosis latest stored diagnosis (204 if none)

GET  /api/dashboard                      the 6 headline metrics
GET  /api/dashboard/summary              metrics + all 3 charts in one call
GET  /api/dashboard/failure-breakdown    failures grouped by failure code
GET  /api/dashboard/method-breakdown     failure rate per payment method
GET  /api/dashboard/trend?days=14        daily totals for the last N days

GET  /api/audit-logs                     100 most recent events
GET  /api/audit-logs/{paymentId}         full trail for one payment
```

---

## 2. Phase 4 — Dashboard analytics

### Why a new service instead of adding to `PaymentService`

`PaymentService` already worked. Aggregation is a different job from "fetch and
convert payments", so it went into `DashboardAnalyticsService`. That keeps each
class explainable in one sentence, and it meant zero risk of breaking Phase 1–3.

### The files

**`dto/BreakdownItemDto.java`** — one slice of a chart: `label`, `count`,
`amount`, `percentage`. Deliberately generic so any group-by endpoint can reuse
it instead of inventing a new DTO each time.

**`dto/MethodBreakdownItemDto.java`** — `method`, `total`, `failed`,
`recovered`, `failureRate`. Answers "is UPI failing more than cards?".

**`dto/DailyTrendItemDto.java`** — `date`, `total`, `failed`, `recovered`. One
object per day, which is exactly what a line chart wants.

**`dto/DashboardSummaryDto.java`** — wraps the metrics plus all three lists.
The React dashboard makes **one** HTTP call instead of four. Fewer round trips,
and no risk of the four panels disagreeing because they loaded at different
moments.

**`service/DashboardAnalyticsService.java`** — the actual computation.

### How the code works

`getFailureBreakdown()` walks every payment that failed at least once and does:

```java
counts.merge(code, 1L, Long::sum);
```

`merge` means "if this key is missing, store 1; otherwise add 1 to what's
there". That is the whole group-by, in one line, with no null check.

The trend method has one subtlety worth understanding:

```java
for (int i = days - 1; i >= 0; i--) {
    LocalDate day = today.minusDays(i);
    buckets.putIfAbsent(day, new int[3]);   // zero-fill
}
```

If you only counted days that *have* payments, a quiet Tuesday would vanish and
the chart line would jump straight from Monday to Wednesday, which visually
lies. Pre-creating an empty bucket for every day in the window fixes that.

`getDailyTrend(int days)` validates `1 <= days <= 90` and throws
`IllegalArgumentException`, which `GlobalExceptionHandler` turns into a clean
HTTP 400. Without that, `?days=100000` would try to build 100,000 buckets.

**Grouping happens in Java, not SQL.** With ~80 seeded rows that is the simpler
and more readable choice. At 8 million rows you would push it down to
`GROUP BY` in the database. Say exactly that if a judge asks — knowing *when* a
shortcut stops being acceptable is the answer they want.

---

## 3. Before Phase 5 — four ideas you need first

You said you have not worked with LLMs before, so here is the whole mental model
in four short pieces. There is no machine learning in this project. You are not
training anything. You are calling a web API.

### 3.1 An LLM API is just a REST call

You already know how to `POST` JSON to a URL and read JSON back. That is
*literally* all an LLM API is:

```
POST https://api.groq.com/openai/v1/chat/completions
Authorization: Bearer gsk_xxxxx
Content-Type: application/json

{ "model": "openai/gpt-oss-20b",
  "messages": [ {"role":"system","content":"..."},
                {"role":"user","content":"..."} ] }
```

and the reply is JSON with your text buried at
`choices[0].message.content`. No SDK, no Python, no LangChain. In this project
that one call lives in exactly one class: `LlmClient`.

### 3.2 A prompt is the input, and it has two parts

- **system prompt** — the job description. Sent once per request, describes the
  role, the rules, and the exact output format. Ours says "you are a payment
  failure analyst, you are advisory only, reply with only this JSON shape".
- **user prompt** — the actual case. Ours contains one payment's amount, method,
  provider, failure code and attempt count.

Splitting them matters because the system prompt is *constant* — it is the
contract — while the user prompt changes every call. In this project all prompt
text lives in one class, `AiPromptBuilder`, so a judge can read your rules in
one place and you can tune wording without touching logic.

### 3.3 Structured output means "reply in JSON, not prose"

By default a model writes paragraphs. You cannot put a paragraph in a database
column. So we do three things:

1. The system prompt demands a JSON object with five exact keys.
2. We send `"response_format": {"type":"json_object"}`, which asks the provider
   itself to guarantee syntactically valid JSON.
3. We still parse defensively, because *valid JSON* and *JSON containing the
   values you expected* are not the same thing.

### 3.4 The model is a witness, never the judge

This is the single most important design rule in the project, and it is worth
memorising as a sentence: **the LLM produces an opinion; deterministic Java code
decides what happens.** A language model can be confidently wrong, and it can be
manipulated by text it reads. Money must never depend on that. So the model may
*recommend* `RETRY`; only the policy engine (Phase 6) may *allow* a retry.

---

## 4. Phase 5 — the AI layer

### The shape of it

```
POST /api/payments/PAY1001/analyze
        |
        v
AiDiagnosisService  (the conductor — owns the order and the failure handling)
        |
        |-- AiPromptBuilder ....... builds the two prompts
        |-- LlmClient ............. the ONLY class that touches the internet
        |-- AiResponseParser ...... untrusted text -> ParsedDiagnosis
        |-- FallbackClassifier .... rules  -> ParsedDiagnosis   (same type!)
        |
        |-- AiDiagnosisRepository . saves an ai_diagnoses row
        |-- AuditLogRepository .... saves an audit_logs row
        v
AiDiagnosisResponseDto  ->  JSON  ->  React
```

Both producers return the **same** `ParsedDiagnosis` type. That is the trick
that keeps the class small: the save-and-audit path is written once and does not
know or care whether the answer came from a model or from a `switch`.

### 4.1 The four enums (`entity/`)

**`FailureCategory`** — the closed set of diagnoses: `BANK_TIMEOUT`,
`NETWORK_ERROR`, `INSUFFICIENT_FUNDS`, `INVALID_PAYMENT_DETAILS`,
`PAYMENT_METHOD_ERROR`, `TEMPORARY_PROVIDER_FAILURE`, `UNKNOWN`. Each carries a
`displayName` for the UI and a `merchantHint` in plain language.

**`RecoveryActionType`** — `RETRY`, `WAIT_AND_RETRY`,
`ALTERNATE_PAYMENT_METHOD`, `ESCALATE`, `STOP`.

Both have a `fromCode(String)` method, and that method is a security control,
not a convenience:

```java
public static RecoveryActionType fromCode(String raw) {
    ...
    return ESCALATE;   // unknown input -> the SAFE option, never RETRY
}
```

`Enum.valueOf()` throws on unexpected input. Text arriving from a model *is*
unexpected input — it may be misspelled, translated, or invented. `fromCode`
normalises (trim, uppercase, spaces and dashes to underscores) and, when it
still cannot match, returns `UNKNOWN` / `ESCALATE`. Note the asymmetry on
purpose: an unparseable answer must never become "retry this payment", because
retrying costs money; escalating to a human costs nothing.

**`AiSource`** — `LLM` or `FALLBACK_RULES`, with `isAiAvailable()`. Every stored
diagnosis records which one produced it. This is how the app stays honest: the
UI can show "AI unavailable — fallback rules used" instead of quietly passing
off a `switch` statement as artificial intelligence.

**`PolicyDecision`** — `ALLOWED`, `BLOCKED`, `ESCALATED`. Declared now, used by
the policy engine in Phase 6.

### 4.2 The three entities

**`AiDiagnosis`** (table `ai_diagnoses`) — one row per analysis. Stores category,
probable reason, recommended action, confidence, explanation, `aiSource`,
`modelName`, `latencyMs`, `createdAt`.

Why store it at all instead of just returning it? Three reasons, and the first is
the real one:

1. **Auditability.** A payments system must be able to answer "why did you decide
   that, on that date?" months later. An answer that only ever existed in a
   browser tab cannot answer that.
2. **Cost.** Re-analysing an unchanged payment burns an API call every time
   someone reopens the panel.
3. **Demo safety.** If the venue Wi-Fi dies, already-analysed payments still
   display their diagnosis.

`@ManyToOne` (many diagnoses to one payment) means re-analysis appends a row
rather than overwriting, so history survives.

**`RecoveryActionEntity`** (table `recovery_actions`) — what the model
recommended, what the policy engine decided, the reason, the final action, the
attempt number, the outcome, and how much was recovered. Written in Phase 6/7.
The `Entity` suffix exists purely so the class does not collide with the
`RecoveryActionType` enum.

**`AuditLog`** (table `audit_logs`) — the append-only trail. One design decision
here is worth defending out loud: `paymentId` is a **plain `String`, not a
foreign key**. An audit trail that can be broken by deleting a row from another
table is not an audit trail. Losing the JOIN convenience is a fair price.

### 4.3 The repositories

Spring Data writes the SQL from the method name. Two pieces of syntax:

```java
Optional<AiDiagnosis> findFirstByPayment_PaymentIdOrderByCreatedAtDesc(String id);
```

- `Payment_PaymentId` — the underscore means "step into the related `payment`
  entity, then read its `paymentId` field". Without it Spring looks for a single
  field literally named `paymentPaymentId`, does not find it, and the
  application **fails at startup** rather than at compile time.
- `First` adds `LIMIT 1`. `Optional` because a payment may never have been
  analysed, which is a normal state, not an error.

And one hand-written query:

```java
@Query("select d from AiDiagnosis d join fetch d.payment order by d.createdAt desc")
List<AiDiagnosis> findAllWithPaymentNewestFirst();
```

`AiDiagnosis.payment` is `FetchType.LAZY`, so calling `getPayment()` after the
database session closes throws `LazyInitializationException` — one of the most
common JPA errors. `join fetch` loads the payment in the *same* query, which
kills that error and collapses N+1 queries into one. JPQL, by the way, is written
over entity and field names, not table and column names.

### 4.4 `config/LlmConfig.java` — configuration and the API key

Reads every `payrecover.llm.*` property and builds the HTTP client bean.

**The API key is never in the source and never in a committed file.**
`application.properties` contains:

```properties
payrecover.llm.api-key=${GROQ_API_KEY:}
```

`${GROQ_API_KEY:}` means "read the environment variable `GROQ_API_KEY`; if it is
not set, use an empty string". The empty default is deliberate: a missing key
puts the app into fallback mode instead of crashing at startup.

```java
public boolean isUsable() {
    return enabled && apiKey != null && !apiKey.isBlank();
}
```

One method that answers "can we even try?". `LlmClient` checks it before building
a request, so a missing key costs zero network time.

`@PostConstruct logStartupState()` prints, at boot, whether the app is in real-AI
mode or fallback mode. Thirty seconds of debugging saved every single time.

Timeouts are set on a `SimpleClientHttpRequestFactory` (connect 5s, read 25s).
Without them a hung provider would freeze `/analyze` forever — and there is no
timeout by default.

### 4.5 `service/AiPromptBuilder.java` — the only file containing prompt text

`buildSystemPrompt()` uses a Java text block and generates the allowed category
and action lists **from the enums**:

```java
.formatted(allowedCategories(), allowedActions())
```

That matters more than it looks. If you add a `FailureCategory` next month, the
prompt updates itself. Hand-typed lists in prompts drift out of sync with code,
and then the model returns a category your parser rejects.

`buildUserPrompt(Payment)` sends payment id, amount, method, provider, failure
code, attempts, status, timestamp and a customer *reference*. **No PII** — no
name, no email, no card number. Send a model the minimum it needs to do the job.

### 4.6 `service/LlmClient.java` — the only class that touches the internet

Builds the request body (`model`, `temperature`, `max_tokens`, optional
`response_format`, `messages`), POSTs to `/chat/completions` with
`Authorization: Bearer <key>`, and digs the text out of the reply:

```java
JsonNode content = root.path("choices").path(0).path("message").path("content");
```

`path()` rather than `get()`: `path()` returns an empty node for a missing key,
`get()` returns `null` and the next call throws `NullPointerException`. When you
are reading a response shape you do not control, `path()` is the correct tool.

Every failure — no key, DNS failure, 401, 429, timeout, malformed body, empty
content — is converted to one exception type, `LlmUnavailableException`. One type
means the caller needs exactly one `catch` block to be safe. The catch also logs
the error **without** the API key, so a stack trace in a screen-shared terminal
does not leak your credential.

### 4.7 `service/AiResponseParser.java` — treat the model's reply as untrusted

Three defences, each guarding a real failure mode:

**Fence and prose stripping.** Models like to wrap JSON in ``` fences or add "Here
is the analysis:". `extractJsonObject()` strips fences and takes everything from
the first `{` to the last `}`.

**Safe enum parsing.** `FailureCategory.fromCode(...)` instead of `valueOf(...)`,
so `"bank timeout"` or `"Bank-Timeout"` still lands on `BANK_TIMEOUT`, and pure
nonsense lands on `UNKNOWN` rather than throwing.

**Confidence normalisation.** Models return `0.91`, `91`, `"91%"`, sometimes
`1.4`. So:

```java
if (value > 1.0 && value <= 100.0) value = value / 100.0;   // 91  -> 0.91
value = Math.max(0.0, Math.min(1.0, value));                // clamp
```

Without that, a `91` would render as "9100% confident" on your dashboard in front
of judges.

Reason and explanation are length-capped (300 / 800 chars) so a rambling reply
cannot overflow a column or wreck the layout. Blank text falls back to the
category's own `displayName` / `merchantHint`, so the UI never shows an empty box.

### 4.8 `service/FallbackClassifier.java` — the honest non-AI path

A `switch` on the failure code. That is the entire implementation, and the class
javadoc says so in those words.

| failure code | category | action | confidence |
|---|---|---|---|
| `BANK_TIMEOUT` | BANK_TIMEOUT | RETRY | 0.75 |
| `NETWORK_ERROR` | NETWORK_ERROR | RETRY | 0.72 |
| `TEMPORARY_PROVIDER_FAILURE` | TEMPORARY_PROVIDER_FAILURE | WAIT_AND_RETRY | 0.70 |
| `INSUFFICIENT_FUNDS` | INSUFFICIENT_FUNDS | ALTERNATE_PAYMENT_METHOD | 0.80 |
| `INVALID_PAYMENT_DETAILS` | INVALID_PAYMENT_DETAILS | STOP | 0.78 |
| `PAYMENT_METHOD_ERROR` | PAYMENT_METHOD_ERROR | ALTERNATE_PAYMENT_METHOD | 0.68 |
| anything else | UNKNOWN | ESCALATE | 0.30 |

Three deliberate choices:

- Confidences are **modest**. These are rules with no context; claiming 0.99 would
  be dishonest, and 0.75 keeps the number meaningful when compared to the model's.
- It **ignores the attempt count**, even though "already tried 5 times" obviously
  matters. Attempt limits are a *policy* rule, and policy lives in the policy
  engine. Two classes deciding retry limits is how contradictions get shipped.
- `RULES_VERSION = "deterministic-rules-v1"` is stored in `modelName`, so an audit
  row always says exactly what produced it.

`NOTICE = "AI unavailable - fallback rules used."` lives here as a constant and is
surfaced in the API response and appended to the audit details.

### 4.9 `service/AiDiagnosisService.java` — the conductor

**What it does.** Loads the payment, refuses to analyse a successful one, times
the diagnosis, gets an answer (model or rules), saves it, writes one audit row,
returns a DTO.

**Why it is needed.** Every other Phase 5 class is a specialist that does one
thing. Something has to own the *order* and the *failure handling*. This is it.

**How it connects.** It is the only class that talks to all of
`AiPromptBuilder`, `LlmClient`, `AiResponseParser`, `FallbackClassifier`,
`AiDiagnosisRepository` and `AuditLogRepository`.

The most important eight lines in the phase:

```java
} catch (LlmUnavailableException ex) {
    log.warn("... Using fallback rules.");
    return fallbackClassifier.classify(payment);

} catch (RuntimeException ex) {
    log.error("Unexpected error ... Using fallback rules.", ex);
    return fallbackClassifier.classify(payment);
}
```

The first catch handles the failure we designed for. The second handles the one we
did not predict — a Jackson quirk, a provider changing its response shape. Because
both fall back, `POST /analyze` returns `200 OK` with a real, usable answer even
with **no API key and no internet**. It is logged at `error` level so a genuine bug
is still loud in the console.

`@Transactional` on `analyze()` means the diagnosis row and the audit row commit
together or not at all. You can never end up with a decision that has no audit
trail — which, for an audit trail, is the entire point.

Two things this class deliberately does **not** do: it never decides whether the
recommended action is *allowed*, and it never calls `payment.setStatus(...)`.
A diagnosis is an opinion. Turning an opinion into an action is Phase 6's job.
If this class ever wrote a payment status, the project would have broken its own
central rule.

### 4.10 The audit trail

**`AuditLogService`** is read-only and has **no `create()` method**. Audit rows are
written by whichever service made the decision, inside that decision's
transaction. If writing were a public API, someone could later insert a row that
never corresponded to a real event — and a trail you can forge is worthless.

Its `toDto` has an explicit null guard before every `.name()` call, because every
enum column in `audit_logs` is nullable. Forgetting one of those is the single most
common `NullPointerException` in DTO mapping code:

```java
dto.setPolicyDecision(log.getPolicyDecision() == null
        ? null : log.getPolicyDecision().name());
```

`AuditLogController` exposes only `GET`. No `POST`, on purpose.

### 4.11 The failed-payments table columns

`PaymentResponseDto` gained `analyzed`, `failureCategory`,
`failureCategoryLabel`, `aiRecommendation`, `aiRecommendationLabel`,
`confidencePercent`, `aiSource`. `PaymentService` fills them from the newest
diagnosis per payment, in **one** query:

```java
for (AiDiagnosis d : aiDiagnosisRepository.findAllWithPaymentNewestFirst()) {
    latestByPaymentId.putIfAbsent(d.getPayment().getPaymentId(), d);
}
```

The query returns newest-first, and `putIfAbsent` only stores when the key is
absent — so the first row seen per payment is the latest one, and older rows are
ignored. Querying inside the loop instead would have been 1 + 80 queries.

`analyzed` is a single boolean so the UI checks one field instead of null-checking
five. Note the getter is `isAnalyzed()`, not `getAnalyzed()` — Jackson uses that
naming rule to produce the JSON key `"analyzed"`.

---

## 5. How to run and test it

### 5.1 Get a free Groq key

Sign in at <https://console.groq.com/keys> and create a key (starts with `gsk_`).

### 5.2 Set it as an environment variable — never in a file

```powershell
# Windows PowerShell (this shell session only)
$env:GROQ_API_KEY="gsk_your_key_here"
```

```bat
:: Windows CMD
set GROQ_API_KEY=gsk_your_key_here
```

```bash
# macOS / Linux
export GROQ_API_KEY=gsk_your_key_here
```

Set it in the **same terminal** you then run Maven from, otherwise the app will
not see it. Never commit it, never paste it into `application.properties`, never
paste it into a screenshot.

### 5.3 Run

```bash
cd payrecover-ai
mvn clean compile      # compile only
mvn spring-boot:run    # start on http://localhost:8080
```

At startup `LlmConfig` logs which mode you are in. Look for that line first.

### 5.4 Test the AI path

```bash
# find a failed payment id
curl http://localhost:8080/api/payments/failed

# analyse it  (POST, not GET -- it costs money and writes rows)
curl -X POST http://localhost:8080/api/payments/PAY1001/analyze

# read it back without calling the model again
curl http://localhost:8080/api/payments/PAY1001/diagnosis

# the audit trail for it
curl http://localhost:8080/api/audit-logs/PAY1001
```

Expected response shape:

```json
{ "paymentId": "PAY1001",
  "failureCategory": "BANK_TIMEOUT",
  "failureCategoryLabel": "Bank Timeout",
  "recommendedAction": "RETRY",
  "confidencePercent": 88,
  "aiSource": "LLM",
  "aiAvailable": true,
  "notice": null,
  "modelName": "openai/gpt-oss-20b",
  "latencyMs": 640,
  "simulation": true }
```

### 5.5 Test the fallback path — do this before you demo

Open a terminal **without** `GROQ_API_KEY` set (or set
`payrecover.llm.enabled=false`) and analyse a payment again. You should still get
`200 OK`, with `"aiSource": "FALLBACK_RULES"`, `"aiAvailable": false` and
`"notice": "AI unavailable - fallback rules used."`

If that works, no network problem on stage can break your demo.

### 5.6 Test Phase 4

```bash
curl http://localhost:8080/api/dashboard/summary
curl http://localhost:8080/api/dashboard/failure-breakdown
curl http://localhost:8080/api/dashboard/method-breakdown
curl "http://localhost:8080/api/dashboard/trend?days=7"
curl "http://localhost:8080/api/dashboard/trend?days=0"   # expect HTTP 400
```

---

## 6. Verification checklist

Startup:

- [ ] `mvn clean compile` succeeds
- [ ] App starts with no `BeanCreationException`
- [ ] Startup log states real-AI mode or fallback mode
- [ ] Tables `ai_diagnoses`, `recovery_actions`, `audit_logs` were created
      (`SHOW TABLES;` in `payrecover_db`)

Phase 4:

- [ ] `/api/dashboard/summary` returns metrics plus three non-empty lists
- [ ] failure-breakdown percentages add up to ~100
- [ ] `trend?days=7` returns exactly 7 items, including any zero days
- [ ] `trend?days=0` returns HTTP 400, not 500

Phase 5 with a key:

- [ ] `POST /analyze` returns 200 with `"aiSource": "LLM"`
- [ ] `confidencePercent` is between 0 and 100
- [ ] `GET /diagnosis` returns the same result without a new API call
      (`latencyMs` unchanged)
- [ ] a row appears in `ai_diagnoses` and one in `audit_logs`
- [ ] `/api/payments/failed` now shows `analyzed: true` for that payment

Phase 5 without a key:

- [ ] `POST /analyze` still returns **200**, never 500
- [ ] `aiSource` is `FALLBACK_RULES`, `aiAvailable` is `false`
- [ ] `notice` says "AI unavailable - fallback rules used."

Edge cases:

- [ ] analysing a `SUCCESS` payment returns HTTP 400 with a clear message
- [ ] analysing an unknown id returns HTTP 404
- [ ] `GET /diagnosis` on a never-analysed payment returns HTTP 204
- [ ] no API key appears anywhere in the repository (`git grep gsk_` finds nothing)

---

## 7. Common errors and what they actually mean

**`404 model_not_found` from Groq.** The model ID is retired. This bit a lot of
tutorials: `llama-3.3-70b-versatile` and `llama-3.1-8b-instant` were deprecated in
June 2026 and shut down for free/developer tiers in August 2026. This project is
configured with `openai/gpt-oss-20b`; `openai/gpt-oss-120b` reasons better and is
a little slower. If you copied a model name from a blog post, that is your bug.

**`aiSource` is always `FALLBACK_RULES` even though you set the key.** The
environment variable was set in a different terminal from the one running Maven,
or you set it after the app started. Check the startup log line.

**Empty content from the model.** `max-tokens` is too low. These models spend some
of their budget on internal reasoning before writing the JSON, and an empty reply
is what you get when the budget runs out. Keep it at 1200 or higher.

**`LazyInitializationException`.** You called `diagnosis.getPayment()` outside a
transaction using a query without `join fetch`. Use
`findAllWithPaymentNewestFirst()`, or add `@Transactional(readOnly = true)`.

**Startup failure mentioning a property that cannot be found on an entity.** A
Spring Data method name does not match the entity's fields — usually a missing
underscore, e.g. `findByPaymentPaymentId` instead of `findByPayment_PaymentId`.

**`NullPointerException` in a `toDto`.** A nullable enum column had `.name()`
called on it without a guard.

**Confidence showing as 9100%.** The normalisation step was skipped. The model
returned `91` where `0.91` was expected.

**Unknown enum value crashes the parse.** Someone used `valueOf()` instead of
`fromCode()`.

---

## 8. Interview / judging questions

**Where exactly is the AI in this project, and where is it not?**
The AI is one HTTP call in `LlmClient`, driven by prompts from
`AiPromptBuilder`, whose reply is parsed by `AiResponseParser`. Everything else —
categories, the fallback classifier, the metrics, the policy engine — is
deterministic Java. No training, no ML libraries, no vector database.

**Why can the LLM not execute a retry?**
Because a language model can be confidently wrong and can be influenced by text it
reads, and retries move money. It returns a recommendation; the deterministic
policy engine decides, and can override the recommendation. That boundary is
enforced structurally: `AiDiagnosisService` has no access to anything that changes
a payment's status.

**What happens if the AI API is down during your demo?**
Nothing breaks. `LlmClient` converts every failure into
`LlmUnavailableException`, `AiDiagnosisService` catches it (plus any unexpected
`RuntimeException`) and calls `FallbackClassifier`. The response is still `200 OK`,
`aiSource` becomes `FALLBACK_RULES`, and the UI shows "AI unavailable — fallback
rules used." I test this path by unsetting the key.

**How do you keep the model's output safe to store?**
Fences and prose are stripped, enums are parsed through `fromCode()` which returns
`UNKNOWN`/`ESCALATE` rather than throwing, confidence is normalised and clamped to
0–1, and free text is length-capped. Unparseable input defaults to the *safe*
action, never to `RETRY`.

**Why POST for `/analyze` and GET for `/diagnosis`?**
`GET` should be safe and repeatable. `/analyze` costs an API call and inserts two
rows, so it is `POST`. `/diagnosis` only reads, so it is `GET` — and returns `204`
when the payment has never been analysed, because "no diagnosis yet" is a normal
state, not an error.

**Why does the audit log store `paymentId` as a String instead of a foreign key?**
So the trail survives changes to the payments table. An audit record that can be
invalidated by another table's row disappearing is not an audit record.

**How is the confidence number produced?**
The model reports it; we normalise and clamp it. The fallback path uses fixed
per-rule values (0.30–0.80) that are deliberately modest, and `aiSource` records
which of the two produced the number, so we never present a `switch` statement's
guess as a model's judgement.

**Are any dashboard numbers hardcoded?**
No. Every metric is computed from the database on each request. You can verify it
by inserting a row and refreshing.

**How would this scale?**
The grouping currently happens in Java, which is right for ~80 rows and wrong for
millions — that becomes `GROUP BY` in SQL with indexes on `status` and
`created_at`. Diagnoses would move to a queue rather than running inside the HTTP
request, and identical failure codes would be cached rather than re-analysed.

---

## 9. What Phase 6 adds

`PolicyEngine` — the deterministic decision maker. It takes the recommendation and
applies real rules: maximum attempt count, no retry for `INVALID_PAYMENT_DETAILS`,
no retry for `INSUFFICIENT_FUNDS`, escalate above an amount threshold, escalate
when confidence is low. Its verdict (`ALLOWED` / `BLOCKED` / `ESCALATED`) plus a
human-readable reason is written to `recovery_actions` and to a second
`audit_logs` row with `eventType = "POLICY_DECISION"`.

That is the phase where "AI recommends, rules decide" stops being a design
statement and becomes running code.










