# ADR-001: BPMN Dialects and Vendor Compatibility

- Status: Accepted for implementation
- Date: 2026-07-14
- Target: Abada Engine 1.0

## Context

Abada currently reads Camunda assignment attributes directly into runtime task
metadata. This makes another engine's serialization vocabulary look like
Abada's domain model, obscures semantic differences, and prevents explicit
compatibility guarantees. Existing Camunda-authored workflows are nevertheless
valuable and must remain deployable.

## Decision

Abada will retain the Camunda BPMN Model library as a structural BPMN 2.0 XML
reader, but vendor directives will be interpreted only by named compatibility
profile parsers. Standard BPMN, Abada-native, and Camunda 7 assignment syntax
will compile into one vendor-neutral canonical assignment model before runtime
execution.

Compatibility is profile-based (`standard-bpmn-2.0`, `abada-native-1`,
`camunda-7`). Direct deployment preserves source XML and records profile/report
metadata. Explicit migration converts semantics that can be preserved into the
stable Abada namespace `https://abada.io/schema/bpmn`.

Unknown execution-relevant directives fail with stable validation codes.
Known metadata-only extensions may be preserved with warnings. Runtime code
never branches on vendor XML.

## Alternatives considered

- **Keep Camunda attributes as Abada's native format:** rejected because it
  couples public models and semantics to another engine.
- **Reject all vendor XML:** rejected because it breaks existing Abada models
  and practical ecosystem interoperability.
- **Convert files before every deployment:** rejected because direct import is
  required and forced source rewrites harm adoption.
- **Replace the structural parser:** rejected for 1.0 because the current
  library already parses BPMN structure; replacing it would create broad risk
  unrelated to assignment dialect semantics.
- **Create multiple Maven modules now:** rejected because package boundaries
  are sufficient in the current single-module repository.

## Consequences

- The parser/compiler gains explicit profile, report and validation concepts.
- Definition metadata requires an additive Flyway migration.
- Existing Camunda assignment models continue working under the default
  profile, while strict native mode can reject vendor extensions.
- Abada owns expression, normalization, claim and audit semantics and documents
  differences from Camunda.
- Migration output may normalize XML formatting, but must preserve standard
  content and supported semantics deterministically.
- Future Flowable/Camunda 8 profiles can add parsers without changing runtime
  assignment objects; no abstraction is added until a concrete directive needs
  it.
