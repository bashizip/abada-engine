# Camunda assignment migration

The migration command converts supported Camunda 7 user-task assignments to Abada-native XML without deploying them:

```text
abada bpmn migrate process.bpmn --output process.abada.bpmn --report compatibility.json
```

In the current repository build, invoke `com.abada.engine.cli.AbadaCli` as the Java main class. The command defaults output to `<input>.abada.bpmn` and the report to `<output>.report.json`.

Migration retains the original source in the programmatic result, removes translated Camunda assignment attributes, emits `abada:assignment`, reparses the result with standard/native profiles only, and compares canonical meaning in tests. Conflicts and semantics that cannot be preserved fail with `ABADA-BPMN-MIGRATION-001` or the underlying validation issue; the command never guesses.

