# Abada Engine 1.0 — BPMN Dialects, Native Extensions, and Vendor Compatibility

## 1. Status

Target release: **Abada Engine 1.0**

Priority: **Release-blocking**

This specification defines how Abada Engine must represent, parse, validate, execute, import, and migrate BPMN models containing:

- standard BPMN 2.0 elements;
- Abada-native extensions;
- Camunda 7 extensions;
- future vendor-specific extensions such as Flowable and Camunda 8.

The objective is to make Abada an independent BPMN engine while preserving practical interoperability with existing BPMN ecosystems.

---

# 2. Problem statement

Abada currently relies on Camunda-specific BPMN attributes such as:

```xml
camunda:assignee
camunda:candidateUsers
camunda:candidateGroups
```

These attributes are convenient and widely used, but they create several long-term problems:

1. Abada's public process-model format depends on another engine's namespace.
2. Runtime semantics may become accidentally coupled to Camunda behavior.
3. Abada cannot clearly define its own execution guarantees.
4. Future compatibility with Flowable, Camunda 8, or other dialects becomes difficult.
5. Abada risks being perceived as a partial Camunda runtime instead of an independent engine.

Abada 1.0 must solve this by separating:

- BPMN serialization;
- vendor dialect parsing;
- Abada's canonical process model;
- runtime execution semantics.

---

# 3. Architectural decision

Abada Engine must use the following architecture:

```text
BPMN XML
   |
   +-- Standard BPMN 2.0 parser
   |
   +-- Abada extension parser
   |
   +-- Camunda 7 compatibility parser
   |
   +-- Future compatibility parsers
            |
            v
Abada Canonical Process Model
            |
            v
Semantic validation and compilation
            |
            v
Abada Runtime Execution Model
```

The runtime must never directly execute `camunda:*`, `flowable:*`, `zeebe:*`, or XML extension attributes.

All supported BPMN dialects must be translated into the same vendor-neutral internal model before execution.

---

# 4. Core principles

## 4.1 Standards first

When BPMN 2.0 already provides an adequate representation for a concept, Abada should use the BPMN standard.

Examples include:

- `userTask`;
- `serviceTask`;
- `potentialOwner`;
- `humanPerformer`;
- `resourceRole`;
- `formalExpression`;
- `extensionElements`.

Abada-specific extensions must only be introduced when:

- BPMN does not define the required runtime behavior;
- BPMN defines only a structural concept but not the required execution semantics;
- the feature is specific to Abada;
- the feature requires configuration not representable clearly with standard BPMN.

## 4.2 Canonical internal model

All XML dialects must map into neutral Abada domain objects.

The internal model must not contain vendor-prefixed field names such as:

```java
camundaAssignee
camundaCandidateUsers
flowableCandidateGroups
```

Use neutral names:

```java
assignee
candidateUsers
candidateGroups
assignmentStrategy
```

## 4.3 Explicit compatibility

Abada must never claim general “Camunda compatibility.”

Compatibility must be expressed through named and versioned profiles:

```text
standard-bpmn-2.0
abada-native-1
camunda-7
```

Future profiles may include:

```text
camunda-8
flowable-6
```

## 4.4 Transparent behavior

Unsupported or partially supported vendor features must not be silently ignored.

Every deployment or migration must produce:

- errors;
- warnings;
- compatibility mappings;
- unsupported-extension information.

## 4.5 Preserve original models

Importing a vendor BPMN model must not require modifying the source file.

Abada must support:

1. direct deployment through a compatibility profile;
2. explicit conversion to Abada-native BPMN.

---

# 5. Required release scope

Abada Engine 1.0 must include:

1. a canonical vendor-neutral process model;
2. an Abada BPMN namespace;
3. an Abada-native assignment extension;
4. standard BPMN task-assignment parsing where supported;
5. Camunda 7 assignment compatibility;
6. compatibility profiles;
7. deployment-time validation;
8. a compatibility report;
9. BPMN migration tooling;
10. automated tests;
11. user documentation;
12. backward compatibility for existing Abada BPMN models using Camunda extensions.

The first compatibility scope is intentionally limited to user-task assignment.

The architecture must be extensible to additional directives later.

---

# 6. Namespace definition

Abada-native BPMN extensions must use:

```xml
xmlns:abada="https://abada.io/schema/bpmn"
```

The exact URI may be adjusted to the official project domain before release, but once Abada 1.0 is released it must be treated as stable.

A versioned namespace may be used instead:

```xml
xmlns:abada="https://abada.io/schema/bpmn/1.0"
```

Choose one approach and document it.

The preferred approach is a stable namespace combined with an explicit schema version:

```xml
<abada:metadata schemaVersion="1.0" />
```

---

# 7. Abada-native user-task assignment model

## 7.1 Preferred XML representation

Abada must support user-task assignment through `extensionElements`.

```xml
<bpmn:userTask id="approveRequest" name="Approve request">
  <bpmn:extensionElements>
    <abada:assignment
        assignee="${request.owner}"
        candidateUsers="alice,bob"
        candidateGroups="finance,management"
        strategy="claim" />
  </bpmn:extensionElements>
</bpmn:userTask>
```

The parser should also support nested values to allow future extensibility:

```xml
<bpmn:userTask id="approveRequest" name="Approve request">
  <bpmn:extensionElements>
    <abada:assignment strategy="claim">
      <abada:assignee expression="${request.owner}" />

      <abada:candidateUsers>
        <abada:user value="alice" />
        <abada:user expression="${request.reviewer}" />
      </abada:candidateUsers>

      <abada:candidateGroups>
        <abada:group value="finance" />
        <abada:group value="management" />
      </abada:candidateGroups>
    </abada:assignment>
  </bpmn:extensionElements>
</bpmn:userTask>
```

The implementation may initially serialize only one form, but the parser architecture must allow both forms.

## 7.2 Canonical assignment model

Create a vendor-neutral internal representation similar to:

```java
public record UserTaskAssignment(
    Optional<ProcessExpression> assignee,
    List<ProcessExpression> candidateUsers,
    List<ProcessExpression> candidateGroups,
    AssignmentStrategy strategy
) {
}
```

Possible strategy enum:

```java
public enum AssignmentStrategy {
    DIRECT,
    CLAIM
}
```

The exact package and naming must follow the repository's conventions.

## 7.3 Expression abstraction

Assignment values must not be stored only as raw strings.

Introduce or reuse an expression abstraction:

```java
public sealed interface ProcessExpression
    permits LiteralExpression, DynamicExpression {

    String source();
}
```

The canonical model must distinguish:

```text
alice
```

from:

```text
${request.reviewer}
```

The current Abada expression engine may be reused if one already exists.

---

# 8. Assignment semantics

Abada must define and test its own semantics.

## 8.1 Assignee

An assignee identifies the user currently responsible for completing the task.

When an assignee expression exists:

1. it is evaluated when the user task is created;
2. the resulting value must resolve to zero or one user identifier;
3. a null or empty value means no assignee unless strict validation is enabled;
4. multiple resolved values are invalid;
5. the resolved assignee is stored on the task instance;
6. the original expression is retained in the deployed process definition.

## 8.2 Candidate users

Candidate users identify users allowed to claim the task.

Candidate-user expressions are evaluated when the task is created.

The resulting candidate users must be normalized:

- remove surrounding whitespace;
- remove empty values;
- remove duplicates;
- preserve deterministic ordering.

## 8.3 Candidate groups

Candidate groups identify groups whose members may claim the task.

Group membership must not be expanded into individual users at deployment time.

At runtime, authorization may query an identity-provider abstraction.

The task record should retain candidate group identifiers.

## 8.4 Assignee and candidates together

A task may define both:

- an assignee;
- candidate users or groups.

Abada semantics:

- the assignee is directly responsible for the task;
- candidate users and groups may remain visible for auditing and reassignment;
- whether candidates can claim an already assigned task depends on task-authorization rules;
- default behavior must reject claiming a task that already has an assignee.

## 8.5 Claim behavior

A user may claim a task when:

- the task has no current assignee;
- the user is directly listed as a candidate user; or
- the user belongs to one of the candidate groups; or
- an explicitly configured administrative override applies.

Claiming the task must:

- set the assignee;
- record the claiming user;
- record the timestamp;
- append an audit event.

## 8.6 Unclaim behavior

Unclaiming must:

- clear the assignee;
- preserve candidate users and groups;
- append an audit event;
- enforce authorization rules.

## 8.7 Auditability

Assignment-related actions must be auditable:

```text
TASK_CREATED
TASK_ASSIGNED
TASK_CLAIMED
TASK_UNCLAIMED
TASK_REASSIGNED
```

Use existing Abada event infrastructure where available.

---

# 9. Standard BPMN assignment support

Abada should recognize standard BPMN structures when possible.

Example:

```xml
<bpmn:userTask id="approveRequest">
  <bpmn:potentialOwner>
    <bpmn:resourceAssignmentExpression>
      <bpmn:formalExpression>group:finance</bpmn:formalExpression>
    </bpmn:resourceAssignmentExpression>
  </bpmn:potentialOwner>
</bpmn:userTask>
```

For Abada 1.0, support the following standard expressions:

```text
user:alice
group:finance
```

Multiple values may be represented by multiple BPMN resource roles.

The exact supported syntax must be documented.

Unsupported standard resource expressions must produce a validation warning or error, not be silently ignored.

---

# 10. Camunda 7 compatibility

## 10.1 Supported directives for Abada 1.0

The `camunda-7` profile must support:

```xml
camunda:assignee
camunda:candidateUsers
camunda:candidateGroups
```

Example:

```xml
<bpmn:userTask
    id="approveRequest"
    camunda:assignee="${request.owner}"
    camunda:candidateUsers="alice,bob"
    camunda:candidateGroups="finance,management" />
```

These directives must map to the canonical `UserTaskAssignment`.

## 10.2 Mapping table

| Camunda 7 directive | Canonical Abada field |
|---|---|
| `camunda:assignee` | `UserTaskAssignment.assignee` |
| `camunda:candidateUsers` | `UserTaskAssignment.candidateUsers` |
| `camunda:candidateGroups` | `UserTaskAssignment.candidateGroups` |

## 10.3 List parsing

For Camunda-compatible comma-separated values:

```xml
camunda:candidateUsers="alice,bob"
```

the parser must:

- split on commas;
- trim whitespace;
- remove empty values;
- retain expressions;
- reject malformed expressions;
- remove exact duplicates.

## 10.4 Expressions

Values such as:

```xml
camunda:assignee="${request.owner}"
```

must be mapped into Abada's expression abstraction.

Abada does not have to reproduce Camunda's expression engine exactly.

When expression syntax or behavior differs, the compatibility report must state that the expression was mapped using Abada semantics.

## 10.5 Unsupported Camunda extensions

Unknown `camunda:*` extensions must be classified as:

- supported;
- ignored safely;
- preserved but not executed;
- partially supported;
- unsupported.

By default, an unknown execution-relevant Camunda extension must produce an error.

Unknown metadata-only extensions may produce warnings.

---

# 11. Compatibility profiles

Introduce a compatibility profile abstraction.

Suggested interface:

```java
public interface BpmnCompatibilityProfile {

    String id();

    Set<String> supportedNamespaces();

    void contributeParsers(BpmnParserRegistry registry);

    CompatibilityReport validate(BpmnDocument document);
}
```

Required profiles:

```text
standard-bpmn-2.0
abada-native-1
camunda-7
```

A deployment may activate one or more profiles.

Example configuration:

```yaml
abada:
  bpmn:
    compatibility:
      profiles:
        - standard-bpmn-2.0
        - abada-native-1
        - camunda-7
```

Default behavior for Abada 1.0:

```text
standard-bpmn-2.0
abada-native-1
camunda-7
```

This default preserves compatibility with existing Abada models.

A strict native mode must also exist:

```yaml
abada:
  bpmn:
    compatibility:
      profiles:
        - standard-bpmn-2.0
        - abada-native-1
      reject-vendor-extensions: true
```

---

# 12. Parser registry

Vendor extensions must be handled through a parser registry rather than hard-coded conditional logic.

Suggested abstraction:

```java
public interface BpmnExtensionParser<T> {

    boolean supports(BpmnElement element);

    T parse(BpmnElement element, BpmnParsingContext context);
}
```

Alternative naming is acceptable if it matches the project architecture.

The registry must support:

- standard BPMN parsers;
- Abada extension parsers;
- vendor compatibility parsers;
- deterministic parser order;
- duplicate-semantic detection;
- conflict reporting.

---

# 13. Conflict resolution

A user task may contain multiple assignment representations.

Example:

```xml
<bpmn:userTask
    id="approveRequest"
    camunda:assignee="alice">

  <bpmn:extensionElements>
    <abada:assignment assignee="bob" />
  </bpmn:extensionElements>
</bpmn:userTask>
```

Abada must not silently select one.

Default behavior:

```text
deployment error
```

Error code example:

```text
ABADA-BPMN-ASSIGNMENT-001
```

Message:

```text
User task 'approveRequest' defines assignment semantics through more than one
BPMN dialect. Remove one representation or enable an explicit precedence policy.
```

An optional precedence policy may be added later but is not required for 1.0.

---

# 14. Validation

Validation must happen before process deployment is committed.

Required validation rules include:

- invalid namespace declarations;
- duplicate assignment definitions;
- invalid expressions;
- unsupported compatibility directives;
- malformed candidate lists;
- invalid assignment strategy;
- incompatible assignee cardinality;
- unknown execution-relevant extensions;
- unsupported standard BPMN resource expressions.

Each validation issue must include:

```text
code
severity
message
processDefinitionId
elementId
namespace
sourceLocation, when available
suggestedResolution
```

Suggested model:

```java
public record BpmnValidationIssue(
    String code,
    ValidationSeverity severity,
    String message,
    String processDefinitionId,
    String elementId,
    String namespace,
    SourceLocation sourceLocation,
    String suggestedResolution
) {
}
```

Severity levels:

```text
INFO
WARNING
ERROR
```

A deployment containing any `ERROR` must fail atomically.

---

# 15. Compatibility report

Every parsed or deployed BPMN document must be able to produce a compatibility report.

Example:

```text
BPMN Compatibility Report

File: invoice-approval.bpmn
Detected profiles:
- BPMN 2.0
- Camunda 7

Mappings:
✓ camunda:assignee mapped to assignment.assignee
✓ camunda:candidateGroups mapped to assignment.candidateGroups

Warnings:
⚠ Expression '${manager}' will use Abada expression semantics.

Unsupported:
None
```

Suggested API model:

```java
public record CompatibilityReport(
    Set<String> detectedProfiles,
    List<CompatibilityMapping> mappings,
    List<BpmnValidationIssue> issues
) {
}
```

The report must be available:

- programmatically;
- in deployment API responses;
- through CLI commands;
- in logs at an appropriate level.

---

# 16. Migration tooling

Abada 1.0 must provide a migration service that converts supported vendor directives to Abada-native BPMN.

Required command:

```bash
abada bpmn migrate process.bpmn \
  --from camunda-7 \
  --to abada-native-1 \
  --output process.abada.bpmn
```

The migration must:

1. parse the original document;
2. map supported directives into the canonical model;
3. serialize supported semantics using `abada:*`;
4. preserve standard BPMN content;
5. preserve unknown non-conflicting extension elements where possible;
6. produce a migration report;
7. never overwrite the original file unless explicitly requested;
8. fail when semantic equivalence cannot be guaranteed.

Required migration report example:

```text
Migration completed with warnings.

Converted:
- camunda:assignee on task approveRequest
- camunda:candidateGroups on task approveRequest

Preserved:
- camunda:properties

Unsupported:
- camunda:taskListener event="assignment"

Output:
process.abada.bpmn
```

Programmatic API:

```java
MigrationResult migrate(
    InputStream source,
    String sourceProfile,
    String targetProfile
);
```

---

# 17. Deployment API behavior

The deployment API should accept an optional compatibility configuration.

Example request:

```json
{
  "resourceName": "invoice-approval.bpmn",
  "compatibilityProfiles": [
    "standard-bpmn-2.0",
    "abada-native-1",
    "camunda-7"
  ],
  "strict": false
}
```

Example response:

```json
{
  "deploymentId": "dep-123",
  "status": "DEPLOYED",
  "detectedProfiles": [
    "standard-bpmn-2.0",
    "camunda-7"
  ],
  "compatibilityReport": {
    "mappings": [
      {
        "source": "camunda:candidateGroups",
        "target": "assignment.candidateGroups",
        "elementId": "approveRequest"
      }
    ],
    "issues": []
  }
}
```

Existing deployment APIs must remain backward compatible.

---

# 18. Serialization

Abada must define a canonical serialization format for native BPMN models.

When serializing Abada-native task assignment, the engine must use the Abada namespace, not Camunda directives.

Example output:

```xml
<bpmn:userTask id="approveRequest">
  <bpmn:extensionElements>
    <abada:assignment
        assignee="${request.owner}"
        candidateGroups="finance,management"
        strategy="claim" />
  </bpmn:extensionElements>
</bpmn:userTask>
```

Serialization must be deterministic enough for meaningful Git diffs.

Where practical:

- preserve element order;
- preserve unrelated extension elements;
- avoid unnecessary namespace changes;
- use stable formatting;
- avoid rewriting the entire document for a small migration.

---

# 19. Backward compatibility

Existing Abada process definitions using Camunda assignment directives must continue to deploy and execute in Abada 1.0.

No existing persisted process instance may become unreadable solely because the parser architecture changes.

Where process definitions are persisted:

- retain the original BPMN resource;
- persist the canonical compiled model or versioned representation;
- store the profile used during deployment;
- ensure old deployments remain executable.

Suggested persisted metadata:

```text
definitionFormatVersion
compatibilityProfiles
detectedNamespaces
compilerVersion
```

---

# 20. Persistence migration

Codex must inspect the current persistence model before implementation.

If assignment fields are stored with Camunda-specific database names, migrate them to neutral names.

Example undesirable schema:

```text
camunda_assignee
camunda_candidate_users
```

Preferred schema:

```text
assignee
candidate_users
candidate_groups
assignment_strategy
```

Any database migration must:

- preserve existing values;
- be backward compatible during rolling upgrade where required;
- include rollback documentation;
- include automated migration tests.

Do not introduce a database migration unless the current schema actually requires it.

---

# 21. Error codes

Create stable machine-readable error codes.

Minimum required codes:

```text
ABADA-BPMN-PROFILE-001
Unknown compatibility profile

ABADA-BPMN-EXTENSION-001
Unsupported execution-relevant extension

ABADA-BPMN-ASSIGNMENT-001
Conflicting assignment representations

ABADA-BPMN-ASSIGNMENT-002
Invalid assignee expression

ABADA-BPMN-ASSIGNMENT-003
Invalid candidate-user expression

ABADA-BPMN-ASSIGNMENT-004
Invalid candidate-group expression

ABADA-BPMN-MIGRATION-001
Migration cannot preserve semantic equivalence
```

Error codes must be documented and covered by tests.

---

# 22. Observability

Parsing, validation, deployment, and migration must expose useful diagnostics.

Required metrics may include:

```text
abada_bpmn_deployments_total
abada_bpmn_validation_failures_total
abada_bpmn_vendor_extensions_total
abada_bpmn_migrations_total
abada_bpmn_migration_failures_total
```

Recommended labels:

```text
profile
namespace
result
issue_code
```

Avoid high-cardinality labels such as process instance ID or element ID.

Structured logs should include:

```text
deploymentId
processDefinitionId
profile
namespace
issueCode
elementId
```

---

# 23. Security requirements

The implementation must not instantiate arbitrary Java classes based only on vendor XML attributes.

Expression evaluation must use Abada's controlled expression environment.

BPMN parsing must protect against:

- XML external entity attacks;
- entity expansion attacks;
- unbounded input sizes;
- unsafe schema resolution;
- remote schema loading.

Migration must not execute process expressions.

Compatibility parsing must only interpret declared supported fields.

---

# 24. Testing requirements

## 24.1 Unit tests

Cover:

- Abada assignment parsing;
- Camunda assignment parsing;
- standard BPMN potential-owner parsing;
- literal expressions;
- dynamic expressions;
- candidate-list normalization;
- duplicate removal;
- malformed values;
- conflicting dialects;
- unknown directives;
- compatibility-report generation;
- serialization;
- migration.

## 24.2 Integration tests

Deploy and execute equivalent process models written in:

1. standard BPMN;
2. Abada-native BPMN;
3. Camunda 7 BPMN.

All three must create equivalent canonical task assignments.

Example assertion:

```java
assertThat(standardTask.assignment())
    .isEqualTo(abadaTask.assignment())
    .isEqualTo(camundaTask.assignment());
```

## 24.3 Round-trip tests

Required paths:

```text
Abada BPMN
→ parse
→ canonical model
→ serialize
→ parse
→ equivalent canonical model
```

```text
Camunda 7 BPMN
→ migrate
→ Abada BPMN
→ parse
→ equivalent canonical model
```

## 24.4 Regression tests

All current BPMN parsing and execution tests must continue passing.

Add fixtures representing BPMN resources currently used by the project.

## 24.5 Negative tests

Cover:

- conflicting directives;
- unsupported Camunda extensions;
- invalid expressions;
- invalid strategy values;
- malformed XML;
- unsafe XML constructs;
- migration with unsupported execution semantics.

---

# 25. Documentation requirements

Create the following documentation:

```text
docs/bpmn/abada-extensions.md
docs/bpmn/camunda-7-compatibility.md
docs/bpmn/compatibility-profiles.md
docs/bpmn/migration-guide.md
docs/bpmn/assignment-semantics.md
```

Documentation must include:

- complete BPMN examples;
- supported directives;
- unsupported directives;
- semantic differences;
- CLI examples;
- deployment API examples;
- migration examples;
- troubleshooting guidance.

---

# 26. Recommended module organization

Codex must adapt this proposal to the current repository rather than creating unnecessary modules.

Suggested logical boundaries:

```text
abada-bpmn-model
abada-bpmn-parser
abada-bpmn-extensions
abada-bpmn-compatibility
abada-bpmn-migration
abada-runtime
```

If Abada currently uses a single-module architecture, packages may be sufficient.

Suggested package structure:

```text
io.abada.bpmn.model
io.abada.bpmn.parser
io.abada.bpmn.validation
io.abada.bpmn.extension
io.abada.bpmn.compatibility
io.abada.bpmn.compatibility.camunda7
io.abada.bpmn.migration
io.abada.runtime.task
```

Do not reorganize the whole repository unless necessary.

---

# 27. Implementation phases

## Phase 1 — Repository assessment

Before changing code, inspect:

- BPMN parser implementation;
- process-definition domain model;
- user-task model;
- expression handling;
- deployment pipeline;
- validation pipeline;
- persistence entities;
- database schema;
- test fixtures;
- public APIs;
- CLI structure;
- documentation structure.

Produce an implementation plan referencing actual classes and packages.

## Phase 2 — Canonical assignment model

Introduce the neutral assignment model.

Refactor current Camunda parsing to populate the canonical model.

Do not change external BPMN behavior yet.

All existing tests must remain green.

## Phase 3 — Compatibility profiles

Introduce:

- compatibility profile abstraction;
- profile registry;
- Camunda 7 profile;
- profile detection;
- compatibility report.

## Phase 4 — Abada-native extension

Implement:

- namespace support;
- assignment parser;
- validation;
- canonical serialization;
- tests.

## Phase 5 — Conflict validation

Detect and reject multiple assignment representations.

Add stable error codes.

## Phase 6 — Migration

Implement Camunda 7 to Abada-native migration.

Add CLI and programmatic API.

## Phase 7 — Deployment integration

Expose:

- selected profiles;
- detected profiles;
- validation issues;
- compatibility mappings.

Preserve existing API compatibility.

## Phase 8 — Documentation and examples

Add complete documentation and sample BPMN files.

---

# 28. Acceptance criteria

The feature is complete only when all the following conditions are satisfied.

## Architecture

- [ ] Runtime code has no direct dependency on Camunda XML semantics.
- [ ] Vendor-specific parsers map into a canonical Abada model.
- [ ] The canonical model uses vendor-neutral names.
- [ ] Compatibility profiles are versioned and explicit.

## Abada-native BPMN

- [ ] `abada:assignment` is supported.
- [ ] Abada-native BPMN is valid BPMN 2.0 with extensions.
- [ ] Abada-native assignment can be serialized deterministically.
- [ ] Assignment semantics are documented.

## Camunda compatibility

- [ ] `camunda:assignee` is supported.
- [ ] `camunda:candidateUsers` is supported.
- [ ] `camunda:candidateGroups` is supported.
- [ ] Existing Abada BPMN files continue working.
- [ ] Unsupported execution-relevant extensions fail explicitly.

## Validation

- [ ] Conflicting dialects fail deployment.
- [ ] Invalid expressions fail deployment.
- [ ] Validation issues include machine-readable codes.
- [ ] Deployment remains atomic.

## Migration

- [ ] Camunda 7 assignment directives can be converted to Abada-native BPMN.
- [ ] The original file is preserved.
- [ ] Migration produces a report.
- [ ] Semantic uncertainty causes a warning or failure.

## Testing

- [ ] Equivalent standard, Abada, and Camunda models produce equivalent assignments.
- [ ] Round-trip tests pass.
- [ ] Regression tests pass.
- [ ] Security-oriented XML tests pass.

## Documentation

- [ ] Native extension documentation exists.
- [ ] Camunda compatibility documentation exists.
- [ ] Migration documentation exists.
- [ ] User-facing examples are runnable.

---

# 29. Out of scope for Abada 1.0

Unless already substantially implemented, the following are out of scope:

- complete Camunda 7 compatibility;
- complete Camunda 8 compatibility;
- Flowable compatibility;
- task listeners;
- execution listeners;
- connector compatibility;
- Java delegate compatibility redesign;
- forms migration;
- decision-table migration;
- identity-provider implementation;
- graphical BPMN modeler plugins;
- bidirectional Camunda export;
- arbitrary vendor-extension preservation guarantees.

The architecture must support these later without redesigning the canonical model.

---

# 30. Definition of done

This feature is considered done when:

1. all acceptance criteria are met;
2. all tests pass;
3. no existing public API is broken without an approved migration;
4. existing Camunda-based Abada definitions remain operational;
5. new examples use Abada-native directives;
6. compatibility behavior is documented;
7. migration tooling is usable through the CLI;
8. code review confirms the runtime is vendor-neutral;
9. the release notes describe the compatibility boundary accurately.

---

# 31. Required implementation deliverables

Codex must produce:

1. repository assessment;
2. architecture decision record;
3. implementation plan;
4. canonical assignment model;
5. parser registry or equivalent extension architecture;
6. Abada-native parser;
7. Camunda 7 compatibility parser;
8. compatibility-report model;
9. validation rules and error codes;
10. migration service;
11. CLI commands;
12. unit and integration tests;
13. sample BPMN fixtures;
14. user documentation;
15. release-note entry.

---

# 32. Required ADR

Create:

```text
docs/adr/ADR-XXX-bpmn-dialects-and-vendor-compatibility.md
```

The ADR must record:

- why Abada cannot retain Camunda directives as its native model;
- why vendor compatibility remains valuable;
- why a canonical internal model is required;
- why compatibility is profile-based;
- why direct deployment and explicit migration are both supported;
- alternatives considered;
- consequences and trade-offs.