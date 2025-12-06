# Exclusive Gateway — Design & Implementation Notes

> This document explains how **Exclusive Gateways** (XOR-splits) are parsed, represented, and executed in **abada-engine**. It also covers condition evaluation, default flows, engine control‑flow, persistence, and testing patterns.

---

## 1) BPMN semantics (XOR)

An **Exclusive Gateway** routes the token along **exactly one** outgoing `sequenceFlow`:

* The first flow whose **condition evaluates to `true`** is taken.
* If **no conditions** match, and a `default` flow is designated on the gateway, that default is taken.
* If neither applies, the model is **invalid at runtime** and the engine throws an error.

---

## 2) Model representation

### 2.1 `GatewayMeta`

```java
// com.abada.engine.core.model.GatewayMeta
record GatewayMeta(String id, Type type, String defaultFlowId) {
  enum Type { EXCLUSIVE, INCLUSIVE /* future */ }
}
```

### 2.2 `ParsedProcessDefinition`

* Gateways are stored in a `Map<String, GatewayMeta> gateways`.
* Outgoing edges are indexed by source: `Map<String, List<SequenceFlow>> outgoingBySource`.
* Helpers:

    * `isExclusiveGateway(String id)`
    * `getGateways()` → access meta by id
    * `getOutgoing(String sourceId)` → ordered list of flows as modeled

### 2.3 `SequenceFlow`

```java
record SequenceFlow(
  String id,
  String sourceRef,
  String targetRef,
  String name,
  String conditionExpression, // may be null
  boolean isDefault
) {}
```

> **Ordering matters**: iteration preserves the diagram order (as parsed). The selector picks the **first** matching condition.

---

## 3) Parsing from BPMN

The parser populates:

* `GatewayMeta(id, Type.EXCLUSIVE, defaultFlowId)` for `<exclusiveGateway id="..." default="flowX"/>`.
* `SequenceFlow.conditionExpression` from `<bpmn:conditionExpression>` (often wrapped in `<![CDATA[ ... ]]>`).
* `SequenceFlow.isDefault` is inferred from the gateway’s `default` attribute.

---

## 4) Condition evaluation

### 4.1 Expression language

We accept common Camunda‑style expressions and plain JavaScript:

* `${goodOne}` → `goodOne`
* `${x > 5 and status eq 'OK'}` → `x > 5 && status == 'OK'`

### 4.2 Evaluator (`ConditionEvaluator`)

Key steps:

1. **Normalize**: strip `<![CDATA[ ... ]]>` and `${ ... }` wrappers.
2. **Translate** simple EL words → JS: `and`→`&&`, `or`→`||`, `eq`→`==`, `ne`→`!=`.
3. **Bind variables** from `ProcessInstance.variables` into the JS engine context.
4. **Evaluate** using Nashorn (OpenJDK JS): returns `boolean` or coerces numbers/strings.
5. **Safety**: no Java interop exposed; only simple expressions over provided vars. (Future: pluggable expression service.)

```java
boolean ok = ConditionEvaluator.evaluate(condExpr, instance.getVariables());
```

> **Thread‑safety**: we instantiate a fresh `ScriptEngine` per call. Avoid sharing engine instances.

---

## 5) Selection algorithm (`GatewaySelector`)

```java
String chooseOutgoing(GatewayMeta gw, List<SequenceFlow> outgoing, Map<String,Object> vars) {
  // 1) try conditional flows in order
  for (SequenceFlow f : outgoing) {
    var cond = f.getConditionExpression();
    if (cond != null && ConditionEvaluator.evaluate(cond, vars)) {
      return f.getId(); // first true wins
    }
  }
  // 2) default
  if (gw.defaultFlowId() != null) return gw.defaultFlowId();
  // 3) error
  throw new IllegalStateException(
    "No matching condition and no default flow for gateway " + gw.id());
}
```

---

## 6) Engine control‑flow (`ProcessInstance.advance`)

### 6.1 Two modes of advance

* `advance()` → **stop at next user task** (used on `start`).
* `advance(boolean skipCurrentIfUserTask)` → if `true` and pointer sits on a user task (just completed), **step past it** before continuing (used on `complete`).

### 6.2 Pseudocode

```java
while (true) {
  if (isUserTask(pointer)) {
    if (skipCurrent) { pointer = firstOutgoing(pointer); skipCurrent=false; continue; }
    currentActivityId = pointer; return userTaskPayload(pointer);
  }
  if (isExclusiveGateway(pointer)) {
    var gw = gateways.get(pointer);
    var chosen = selector.chooseOutgoing(gw, outgoing(pointer), variables);
    pointer = resolveTarget(chosen); continue;
  }
  if (isEndEvent(pointer)) { currentActivityId = null; return empty(); }
  // pass‑through
  pointer = firstOutgoing(pointer);
}
```

### 6.3 Variable timing

On task completion, variables from the API are **merged** into the instance **before** calling `advance(true)`, ensuring gateway conditions can access them.

---

## 7) API & persistence integration

### 7.1 Task completion path (simplified)

1. **Authorize** via `TaskManager.canComplete(...)`.
2. Load instance; **merge variables** → `instance.setVariable(k, v)`.
3. **Complete** current task in `TaskManager`; persist task and instance.
4. Call `instance.advance(true)`.
5. If a new user task is returned, **materialize** it in `TaskManager` and persist.

### 7.2 Start path

* Create `ProcessInstance`, persist.
* `instance.advance(false)` → stop at first user task.
* Create initial `TaskInstance` and persist.

---

## 8) Edge cases & errors

* **No conditions match and no default**: throw `IllegalStateException` with gateway id.
* **Expressions with `${...}`** not normalized: always false → ensure evaluator normalization.
* **Missing variables**: use default flow or error, per model.
* **Empty outgoing from a node**: treat node as terminal and end the process.

---

## 9) Logging & troubleshooting

Enable `DEBUG` for `ProcessInstance` and `GatewaySelector`:

```
GW {gwId} vars={...}
  flow {flowId} cond='{expr}' -> {true|false}
pi={id} gateway {gwId} -> flow {flowId} -> {target}
```

This immediately shows variable visibility and which flow was chosen.

---

## 10) Testing strategy

* **Unit**: `ProcessInstanceAdvanceTest` with hand‑crafted models registering outgoing flows; asserts both key and name.
* **Integration**: `EngineIntegrationTest` and `TaskApiTest`:

    * Deploy BPMN, start process → initial user task for `alice`.
    * Complete with `{ "goodOne": true }` → branch to Bob’s task.
    * Validate visibility via assignee/candidate users/groups.

---

## 11) Future: Inclusive Gateway

* `GatewayMeta.Type.INCLUSIVE` has been provisioned.
* Execution semantics require selecting **all** true condition flows, managing **fork/join** bookkeeping, and completing the join when all required paths arrive.
* The expression evaluation and outgoing indexing are already compatible; the main work is in token accounting and join convergence.

---

## 12) Security & performance notes

* **Sandbox**: current JS eval is limited to bound variables; no Java interop exposed.
* **Thread‑safety**: create a new `ScriptEngine` per evaluation.
* **Pluggability**: wrap evaluator behind an interface to swap for JEXL/MVEL/custom parser later.
* **Cost**: conditions are tiny and infrequent; per‑eval engine creation is acceptable. If hot, consider a lightweight interpreter or caching compiled scripts.

---

## 13) Quick checklist for implementers

* [ ] Parser sets `GatewayMeta(Type.EXCLUSIVE, defaultFlowId)` and populates `SequenceFlow.conditionExpression`.
* [ ] `ParsedProcessDefinition.getOutgoing(id)` returns flows in model order.
* [ ] `ConditionEvaluator` normalizes `${...}` and binds variables.
* [ ] `GatewaySelector` picks **first true**, else default, else error.
* [ ] `advance(false)` for start; `advance(true)` after completion.
* [ ] Variables merged **before** advancing.
* [ ] Tests cover true/false/default paths and end‑of‑graph behavior.
