# BPMN dialect acceptance evidence

This document maps the implemented release evidence to the normative specification. The authoritative detailed checklist remains in `roadmap-to-1.0.md`.

| Area | Evidence |
| --- | --- |
| Canonical model and Camunda translation | `ProcessExpressionsTest`, `Camunda7AssignmentParserTest` |
| Standard and Abada-native parsing, conflicts | `AssignmentParserRegistryTest` |
| Runtime evaluation and normalization | `AssignmentEvaluatorTest`, PostgreSQL restart tests |
| Profiles, reports, directive validation | `BpmnCompatibilityDetectorTest`, `BpmnDirectiveValidatorTest` |
| Secure and bounded XML | `SecureBpmnParsingTest` |
| Persistence and V1–V6 upgrades | `AbadaEnginePersistenceReloadTest`, `PostgresSchemaUpgradeTest` |
| Migration and round trip | `BpmnMigrationServiceTest`, `AbadaCliTest` |
| Atomic deployment/runtime history | `PostgresRestartRecoveryTest` |

No acceptance criterion may be declared complete solely by this table; the final audit must check every item in specification section 28 and document any concrete blocker.

## Specification section 28 audit

### Architecture

- [x] Runtime code has no direct dependency on Camunda XML semantics; vendor access is confined to deployment translators in `parser`.
- [x] Vendor parsers produce the canonical model.
- [x] Canonical types use vendor-neutral names.
- [x] Profiles are explicit and versioned (`standard-bpmn-2.0`, `abada-native-1`, `camunda-7`).

### Abada-native BPMN

- [x] Compact and nested `abada:assignment` parse under the stable namespace.
- [x] Examples remain BPMN 2.0 documents using `extensionElements`.
- [x] Migration serializes native assignment deterministically and reparses it.
- [x] Assignment behavior is documented.

### Camunda compatibility and validation

- [x] Assignee, candidate-user, and candidate-group directives are translated.
- [x] Existing repository Camunda BPMN regression tests pass.
- [x] Unknown execution directives fail explicitly.
- [x] Conflicting dialects and malformed expressions fail before persistence.
- [x] Issues carry stable codes and structured context.
- [x] Deployment remains one `@AtomicRuntimeCommand`; PostgreSQL rollback tests cover history and state.

### Migration

- [x] Camunda assignments convert to native XML.
- [x] The programmatic result retains the original source unchanged.
- [x] CLI and programmatic calls produce a compatibility report.
- [x] Unpreservable directives fail validation; migration does not apply precedence or guesses.

### Testing and documentation

- [x] Standard, native, and Camunda examples compile and persist equivalent group assignments.
- [x] Native migration round trips preserve canonical assignment.
- [x] The complete regression, PostgreSQL migration/restart, and security suites pass.
- [x] Native, Camunda, migration, profiles, semantics, ADR, and runnable examples exist.

## Deliberate 1.0 boundaries

- Standard resource expressions are limited to `user:<value>` and `group:<value>`; unsupported forms fail.
- Dynamic assignment expressions use deterministic `${map.path}` lookup, not Camunda/JUEL evaluation.
- Migration covers Camunda user-task assignment. Other vendor execution directives must first be modeled by a future compatibility profile and currently fail migration.
- Source line and column are optional in the specification and are not populated by the current DOM boundary.
