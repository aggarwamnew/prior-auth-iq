# PriorAuthIQ

A **Spring AI** reference architecture for **prior-authorization triage**: an assistant that checks a prior-auth request against a payer's written coverage policy, produces a structured, criterion-by-criterion determination, and hands it to a human reviewer to sign.

**Decision support, not medical judgment.** The assistant never decides treatment — it checks whether a request *matches documented coverage criteria*, exactly as a human reviewer does today, and always defers the final call to a person. All data in this repo is synthetic.

**12 tests** | **Spring Boot 3 + Spring AI** | **pluggable matcher (deterministic + LLM)** | **structured, auditable determinations**

## Why this exists

Prior authorization is one of the most manual, slowest, and most-disputed workflows in healthcare administration: a provider requests pre-approval for a drug or procedure, and a reviewer checks the clinical documentation against coverage criteria to return **approve / deny / pend-for-more-info**. It is a document-and-policy reasoning task — precisely the shape modern LLM tooling is good at, and precisely the shape enterprises will deploy inside their existing Java estates via Spring AI.

This project is that deployment, built properly: an interface-first design where a transparent rule-based baseline is swapped for a Spring AI reasoning matcher via configuration, with the audit trail, structured output, and human-in-the-loop that a regulated workflow requires.

## Architecture

```
POST /api/prior-auth
      │
      ▼
 PriorAuthService ── resolves ──▶ PolicyStore  (coverage policy for the service code)
      │
      ▼
 PolicyMatcher (interface)          ← selected by `priorauthiq.matcher`
      ├─ DeterministicPolicyMatcher   ← keyword + negation, no model (default)
      └─ SpringAiPolicyMatcher        ← Spring AI ChatClient + structured output
      │
      ▼
 Determination ── stored ──▶ DeterminationStore   (audit trail, awaits reviewer sign-off)
```

The whole system is built around one seam — the `PolicyMatcher` interface. Everything upstream and downstream (the REST API, the service, the stores, the `Determination` contract) is model-agnostic, so switching from the deterministic baseline to the LLM matcher is a **one-line config change** (`priorauthiq.matcher: llm`) that touches no other component.

### The Spring AI matcher (Slice 2)

`SpringAiPolicyMatcher` sends the coverage criteria and the request to a Spring AI `ChatClient` and gets back a **structured** `LlmAssessment` (typed and schema-validated via `.entity()`, never free text). The model supplies only the *judgment* — decision, per-criterion MET/NOT_MET/UNCLEAR, rationale; server metadata (id, timestamp, `reviewed=false`) and the criterion descriptions are added by the code, and the human-in-the-loop guarantee is structural. It is **provider-portable**: the injected `ChatClient` is backed by whichever model provider is on the classpath (OpenAI, Anthropic, Ollama, Azure…) — the matcher neither knows nor cares which.

### The determination contract

Every request produces a `Determination` — the same typed shape whether the deterministic or the LLM matcher produced it:

- **decision** — `APPROVE` · `DENY` · `PEND`
- **criteriaResults** — per-criterion `MET` / `NOT_MET` / `UNCLEAR` with the evidence relied on (the audit trail)
- **missingInfo** — for `PEND`, exactly what documentation is needed
- **rationale** — a one-paragraph human-readable summary
- **reviewed** — `false` until a human signs off

## API

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/prior-auth` | Submit a request, run triage, return a `Determination` (`201`) |
| `GET`  | `/api/determinations` | List determinations, newest first (optional `?decision=APPROVE\|DENY\|PEND`) |
| `GET`  | `/api/determinations/{id}` | Fetch a specific determination |

Request validation via Bean Validation (`@NotBlank`); invalid requests return `400`, unknown service codes `404`.

### Example

```bash
curl -s -X POST http://localhost:8081/api/prior-auth \
  -H 'Content-Type: application/json' \
  -d '{
        "patientId": "patient-1",
        "serviceCode": "J0135",
        "diagnosisCode": "M06.9",
        "clinicalNotes": "Active disease, biologic-naive.",
        "documentedHistory": [
          "Diagnosis: moderate-to-severe rheumatoid arthritis",
          "Tried and failed methotrexate for 4 months",
          "TB screening completed prior to therapy"
        ]
      }'
```

Returns an `APPROVE` determination with all three adalimumab (J0135) criteria marked `MET`. Drop the methotrexate line and it returns `PEND`, listing the missing step-therapy documentation; state that methotrexate was *not* tried and it returns `DENY`.

## Tech stack

- Java 17 · Spring Boot 3.4 · Spring AI 1.0 (`spring-ai-client-chat`)
- Spring Web + Bean Validation
- In-memory stores (`ConcurrentHashMap`) — a vector-store-backed policy corpus arrives in Slice 3
- JUnit 5 + MockMvc + Mockito (12 tests; the LLM matcher is tested end-to-end over a mocked `ChatModel`, so CI needs no API key)

## Running

```bash
mvn test          # 12 tests
mvn spring-boot:run   # http://localhost:8081 (deterministic matcher, no keys)
```

The default deterministic matcher runs with **no model and no API keys**. To run the LLM matcher, add a Spring AI model-provider starter (e.g. `spring-ai-starter-model-openai`) plus its config under `spring.ai.*`, then set `priorauthiq.matcher: llm`. The rest of the application is unchanged — that is the point of the interface seam.

## Roadmap

The full slice-by-slice plan — LLM matcher, RAG over a policy corpus, tool calling, multi-agent pipeline, observability + audit advisor, MCP server, evaluators in CI — is in [ROADMAP.md](ROADMAP.md). Each slice is a self-contained, tested increment.
