# PriorAuthIQ — Roadmap

Each slice is a self-contained, tested increment that adds one Spring AI enterprise capability to a working system. The design is interface-first so every slice changes as few components as possible.

## Slice 1 — Working skeleton ✅ (this commit)

- Domain model, REST API, in-memory stores, seeded coverage policies.
- `PolicyMatcher` interface with a **deterministic** keyword-and-negation implementation — no model, no keys.
- The full flow works end to end: submit → policy lookup → triage → structured `Determination` → store → reviewer sign-off pending.
- 9 tests (matcher unit + full-stack MockMvc).
- **Point:** prove the architecture and the determination contract before any model is involved, and give the LLM matcher a deterministic control to be evaluated against.

## Slice 2 — Spring AI matcher (LLM + structured output)

- `SpringAiPolicyMatcher implements PolicyMatcher`, backed by a Spring AI `ChatClient`.
- **Structured output:** the model returns a `Determination` via Spring AI's output converter — typed and validated, never free text.
- Provider-portable (`spring.ai.*`), selected by `priorauthiq.matcher: llm`. Tests use a mock `ChatModel` so CI stays green with no keys.
- **Capabilities shown:** ChatClient, prompt templating, structured output, provider portability.

## Slice 3 — RAG over a policy corpus

- Move coverage policies into a vector store (pgvector); retrieve the applicable criteria for a requested service instead of an in-memory lookup.
- ETL: policy documents → reader → splitter → embeddings.
- **Capabilities shown:** `VectorStore`, document ETL, retrieval-augmented matching, advisors.

## Slice 4 — Tool calling

- Give the matcher `@Tool` access to patient-history, formulary, and prior-claims stores so it can pull evidence rather than being handed it.
- **Capabilities shown:** tool/function calling, grounded agentic data access.

## Slice 5 — Multi-agent pipeline

- Decompose triage into cooperating agents: intake/extract → policy-match → completeness-check → decision-draft.
- **Capabilities shown:** agent orchestration (routing / orchestrator-workers), separation of concerns across LLM steps.

## Slice 6 — Observability, audit, and multi-provider routing

- Micrometer metrics + tracing on every model call (tokens, latency, cost per determination).
- A custom **audit advisor** logging every prompt/response pair — a compliance requirement, not a nicety.
- Data-sensitivity routing: PHI-touching steps on a local model, policy-reasoning steps on a frontier model.
- **Capabilities shown:** observability, advisors, guardrails, multi-cloud/multi-provider.

## Slice 7 — MCP server + evaluators in CI

- Expose the case system as **MCP** tools (and consume an external MCP source) via Spring AI's MCP integration.
- Spring AI **evaluators** (relevancy / factuality) run in CI, regression-testing LLM determinations against a labelled policy-case set.
- **Capabilities shown:** MCP client + server, automated LLM evaluation as a quality gate.

---

**Scope discipline:** the roadmap is a direction, not a promise to build all of it at once. Each slice ships only when it is tested and the README reflects it. The deterministic baseline from Slice 1 remains in the repo permanently as the control the LLM matcher is measured against.
