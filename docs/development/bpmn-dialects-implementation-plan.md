# BPMN Dialects and Compatibility — Implementation Plan

Status: approved specification, implementation pending  
Normative source: [BPMN dialects and compatibility](../specifications/bpmn-dialects-and-compatibility.md)  
Target: Abada Engine 1.0

## Repository assessment

Abada is a single Maven module, so this feature will use focused packages under
`com.abada.engine` rather than create the speculative modules shown in the
specification.

Current execution path:

```text
ProcessController.deploy
  → AbadaEngine.deploy (@AtomicRuntimeCommand)
  → BpmnParser.parse
  → ParsedProcessDefinition / TaskMeta
  → ProcessDefinitionEntity + original bpmn_xml
  → ProcessInstance.advance
  → UserTaskPayload
  → TaskManager.createTaskSnapshot
  → TaskEntity
```

Relevant code and evidence:

| Concern | Current implementation | Planned change |
| --- | --- | --- |
| BPMN parsing | `parser.BpmnParser` using Camunda BPMN Model API | Keep the standard structural parser; add secure source inspection, profile registry and assignment parsers that produce canonical models |
| Deployment validation | `parser.SupportedBpmnValidator` throws `ProcessEngineException` strings | Add structured issues/codes and compatibility validation before persistence |
| Process model | `core.model.ParsedProcessDefinition`, mutable `TaskMeta` strings | Add canonical expression/assignment records; make `TaskMeta` carry one neutral assignment |
| Expressions | `util.ConditionEvaluator` strips `${...}` and runs Nashorn | Add controlled assignment evaluation with explicit literal/dynamic parsing; do not claim Camunda EL equivalence |
| Runtime task creation | `ProcessInstance.advance`, `UserTaskPayload`, `TaskManager` | Resolve canonical assignment once from process variables when creating a task |
| Task authorization | `TaskManager.claimTask/checkCanComplete` | Preserve current eligibility; explicitly reject assigned-task claims and add authorized unclaim |
| Deployment transaction | `AbadaEngine.deploy` with `@AtomicRuntimeCommand` | Return a deployment result/report while retaining the atomic save/history/cache-after-commit behavior |
| Definition persistence | `ProcessDefinitionEntity`, migrations V1–V6 | Add V7 neutral compiler/profile/report metadata; retain original BPMN XML |
| Task persistence | neutral `tasks.assignee` and candidate join tables | No vendor-column rename; add only assignment strategy if runtime audit/behavior needs it |
| API | multipart `POST /v1/processes/deploy` returning a map | Keep multipart contract; add optional profile/strict parameters and compatibility report fields |
| CLI | no product CLI; only Spring Boot main and sample runner | Add a small `com.abada.engine.cli.BpmnCli` entry point plus `bin/abada`; no CLI framework dependency unless argument complexity proves it necessary |
| Metrics | `observability.EngineMetrics` | Add bounded profile/result/code counters without element/process labels |
| Tests | parser, API, runtime and PostgreSQL suites | Add dialect fixtures/tests alongside existing suites and keep all 101 regressions green |

## Current Camunda-specific semantic leaks

1. `BpmnParser` calls `UserTask.getCamundaAssignee()`,
   `getCamundaCandidateUsers()` and `getCamundaCandidateGroups()` directly and
   stores their raw strings in `TaskMeta`.
2. `BpmnParser` and `SupportedBpmnValidator` directly interpret
   `ServiceTask.getCamundaClass()` and `getCamundaTopic()`. Service-task dialect
   redesign is out of scope, but these directives must be classified and must
   not be accidentally rejected by assignment-only compatibility validation.
3. `BpmnParser` directly reads Camunda process starter attributes. They remain
   backward-compatible but outside the new assignment compatibility guarantee.
4. `ConditionEvaluator` describes `${...}` as Camunda/EL and silently converts
   it to JavaScript. Assignment expressions need an explicit Abada abstraction
   and errors; gateway behavior remains unchanged in this focused feature.
5. Most test fixtures declare Camunda namespaces and assignment attributes.
   They are the primary backward-compatibility regression corpus.
6. `JavaDelegate` documentation names `camunda:class`. This is an existing
   service-task compatibility boundary, not part of the 1.0 assignment
   canonicalization scope.

## Persistence decision

A Flyway migration is required. Existing task columns are already neutral, so
they must not be renamed. V7 will add definition metadata (`definition_format_version`,
`compatibility_profiles`, `detected_namespaces`, `compiler_version`, and a
compatibility-report JSON/text column) and, if retained by the final runtime
design, a neutral `assignment_strategy` column on tasks. Existing definitions
will be backfilled as legacy Camunda-compatible format and continue to be
recompiled from their original `bpmn_xml`. Upgrade tests will cover V1–V6 → V7
and fresh V7 creation on PostgreSQL.

## Backward-compatibility risks

- Existing definitions rely on permissive comma splitting; new normalization
  must preserve valid values while newly rejecting malformed expressions.
- Existing fixtures include execution-relevant Camunda service-task directives
  and metadata such as `formKey`, modeler attributes, starter candidates and
  `versionTag`. Classification must not break currently supported runtime
  behavior, but unknown execution directives must fail.
- Changing `TaskMeta` constructors/getters can break tests and internal callers;
  compatibility accessors will remain temporarily while runtime migrates.
- Re-parsing old persisted XML after restart must use the deployment's persisted
  profiles, with a legacy default for pre-V7 rows.
- Adding report fields must not remove or rename existing deployment response
  fields.
- Strict-native mode intentionally rejects vendor extensions and therefore must
  be opt-in.
- XML serialization can alter prefixes/whitespace. Migration tests will assert
  semantic preservation and deterministic output, not byte identity.
- The repository does not yet certify rolling upgrades. V7 will be additive and
  nullable/backfilled, but no new rolling-upgrade guarantee is implied.

## Unclear or conflicting requirements and chosen interpretations

1. **Namespace versioning:** choose the preferred stable namespace
   `https://abada.io/schema/bpmn` with optional `abada:metadata
   schemaVersion="1.0"`.
2. **Deployment JSON example vs current multipart API:** preserve multipart and
   represent profiles/strict as optional form/query parameters; do not add a
   breaking JSON-only endpoint.
3. **Standard assignment roles:** `potentialOwner` maps `user:` to candidate
   users and `group:` to candidate groups. `humanPerformer` accepts exactly one
   `user:` and maps it to assignee. Other resource syntax is an error because
   it affects execution.
4. **Null dynamic assignee under strict validation:** deployment cannot know
   runtime cardinality. Syntax is validated at deployment; null/empty resolves
   to unassigned by default and fails task creation only when strict runtime
   assignment is selected.
5. **Identity-provider abstraction:** membership expansion is explicitly out of
   scope. Existing request-supplied user groups remain the authorization input.
6. **Unknown Camunda metadata:** known metadata-only/modeler directives are
   preserved with warnings; unknown attributes on executable BPMN elements are
   treated as execution-relevant and rejected. No directive is silently lost.
7. **Original expression retention:** expressions remain in the persisted
   original XML and compiled definition cache; task rows store resolved values.
8. **CLI packaging:** the repository has no distribution launcher. A checked-in
   `bin/abada` wrapper invoking the executable JAR satisfies the required
   command without a new module.
9. **Migration overwrite:** default output is mandatory; an explicit
   `--overwrite` may target the source, otherwise source and output must differ.
10. **Assignment audit events:** existing history supports `TASK_CREATED` and
    `TASK_CLAIMED`; add `TASK_UNCLAIMED` and keep reassignment outside the API
    unless a safe authorized endpoint is introduced.

## Phased implementation

### Phase 1 — Assessment, roadmap and ADR

Files: roadmap, this plan, `docs/adr/ADR-001-bpmn-dialects-and-vendor-compatibility.md`.

Tests: documentation link/diff checks only. No runtime behavior changes.

Exit: real change surfaces, migration need, risks, conflicts and phase gates
are documented.

### Phase 2 — Canonical assignment model with Camunda behavior preserved

Files: new canonical types under `core.model.assignment`; `TaskMeta`,
`ParsedProcessDefinition`, `BpmnParser`, `ProcessInstance`, `UserTaskPayload`;
new parser/model unit tests.

Tests:

- literal vs dynamic expression parsing;
- candidate trimming, empty removal, stable deduplication;
- current Camunda fixtures produce the same resolved task fields;
- full backend regression suite.

Exit: runtime consumes neutral assignment objects; Camunda XML is translated at
the parser boundary and existing behavior remains operational.

### Phase 3 — Profiles, registry, reports and persisted compiler metadata

Files: new packages `parser.compatibility`, `parser.validation`; parser
orchestrator; configuration properties; `ProcessDefinitionEntity`; V7 Flyway
migration; `DatabaseTestHelper`, `PostgresSchemaUpgradeTest`, definition cache
tests.

Tests:

- default/explicit/unknown profiles;
- deterministic parser order and profile detection;
- mapping/report generation;
- unknown safe metadata warning vs execution directive error;
- fresh PostgreSQL V7 and V1–V6 upgrades;
- restart recompilation using persisted profiles.

Exit: profiles and reports are programmatic/persisted, and migration evidence
is green.

### Phase 4 — Abada-native and standard assignment plus runtime expressions

Files: Abada and standard assignment parsers, secure XML reader, controlled
assignment evaluator, `TaskManager`/task payload flow, fixtures.

Tests:

- compact and nested `abada:assignment`;
- standard `potentialOwner` and `humanPerformer` subset;
- equivalent standard/native/Camunda assignments at runtime;
- dynamic values evaluated once at task creation;
- deterministic candidate normalization;
- XXE/entity/remote schema/input-size rejection.

Exit: all three required dialects compile to and execute the same canonical
assignment semantics.

### Phase 5 — Conflict and directive validation, task lifecycle audit

Files: issue/error-code catalog, conflict validator, exception/API error
mapping, task unclaim command/API if compatible, history tests.

Tests:

- all required `ABADA-BPMN-*` codes;
- cross-dialect conflicts;
- invalid expressions/strategy/cardinality/lists;
- unknown execution directives;
- failed deployment persists no definition/history/cache entry;
- claim assigned task rejected; unclaim preserves candidates and audits actor.

Exit: errors are stable and machine-readable, and deployment remains atomic.

### Phase 6 — Deterministic migration service and CLI

Files: `bpmn.migration` package, native serializer, `cli.BpmnCli`, `bin/abada`,
migration fixtures/tests.

Tests:

- Camunda → native conversion and canonical equivalence;
- native parse/serialize/parse round trip;
- original input unchanged and output refusal without explicit overwrite;
- safe unknown extensions preserved where possible;
- listener/unsupported semantics fail with `ABADA-BPMN-MIGRATION-001`;
- CLI success, warning, invalid arguments and failure exit codes.

Exit: programmatic and command-line migration are usable and deterministic.

### Phase 7 — Atomic deployment API and observability integration

Files: deployment request/result DTOs, `ProcessController`, `AbadaEngine`,
`GlobalExceptionHandler`, `EngineMetrics`, API tests/OpenAPI assertions.

Tests:

- old multipart request/response fields remain valid;
- optional profiles and strict mode work;
- response includes detected profiles, mappings and issues;
- validation errors expose codes/details;
- metrics use bounded labels;
- PostgreSQL failed/successful deployment transaction tests.

Exit: compatibility is visible through public deployment contracts without a
breaking API change.

### Phase 8 — Documentation, examples and acceptance audit

Files: required `docs/bpmn/*`, ADR finalization, BPMN support/runtime semantics,
release notes and runnable samples.

Tests: full  backend suite, frontend contract builds if API consumption changes,
Docker/package checks where CLI packaging changes, link/example validation.

Exit: every specification acceptance criterion links to implementation and
test evidence; deviations and remaining post-1.0 profiles are explicit.

## Refactoring boundary

This work will not split Maven modules, replace the Camunda structural BPMN
library, redesign service-task delegates, implement an identity provider, or
rewrite unrelated gateway/event parsing. Camunda's model library remains an XML
structure reader; vendor assignment semantics move behind canonical adapters.
