# PriorAuthIQ

A **Spring AI** reference architecture for **prior-authorization triage**: an assistant that checks a prior-auth request against a payer's written coverage policy, produces a structured, criterion-by-criterion determination, and hands it to a human reviewer to sign.

**Decision support, not medical judgment.** The assistant never decides treatment — it checks whether a request *matches documented coverage criteria*, exactly as a human reviewer does today, and always defers the final call to a person. All data in this repo is synthetic.

**9 tests** | **Spring Boot 3** | **pluggable matcher (deterministic → LLM)** | **structured, auditable determinations**

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
 PolicyMatcher (interface)
      ├─ DeterministicPolicyMatcher   ← Slice 1 (this commit): keyword + negation, no model
      └─ SpringAiPolicyMatcher        ← Slice 2: ChatClient + structured output
      │
      ▼
 Determination ── stored ──▶ DeterminationStore   (audit trail, awaits reviewer sign-off)
```

The whole system is built around one seam — the `PolicyMatcher` interface. Everything upstream and downstream (the REST API, the service, the stores, the `Determination` contract) is model-agnostic, so introducing Spring AI in Slice 2 changes exactly one component.

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

- Java 17 · Spring Boot 3.4
- Spring Web + Bean Validation
- In-memory stores (`ConcurrentHashMap`) — a vector-store-backed policy corpus arrives in Slice 3
- JUnit 5 + MockMvc (9 tests)
- Spring AI 1.0 (introduced in Slice 2; BOM staged in `pom.xml`)

## Running

```bash
mvn test          # 9 tests
mvn spring-boot:run   # http://localhost:8081
```

Slice 1 runs with **no model and no API keys** — the deterministic matcher is fully self-contained. The active matcher is chosen in `application.yaml` (`priorauthiq.matcher: deterministic`); Slice 2 adds `llm`, backed by a configurable Spring AI provider.

## Roadmap

The full slice-by-slice plan — LLM matcher, RAG over a policy corpus, tool calling, multi-agent pipeline, observability + audit advisor, MCP server, evaluators in CI — is in [ROADMAP.md](ROADMAP.md). Each slice is a self-contained, tested increment.
