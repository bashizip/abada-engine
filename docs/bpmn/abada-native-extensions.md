# Abada-native BPMN extensions

Abada 1.0 uses the stable namespace `https://abada.io/schema/bpmn`. Schema evolution is expressed through `abada:metadata schemaVersion="1.0"`; the namespace URI does not change.

User-task assignment is placed below BPMN `extensionElements`:

```xml
<bpmn:extensionElements>
  <abada:assignment assignee="${request.owner}"
      candidateUsers="alice,bob" candidateGroups="finance" strategy="direct" />
</bpmn:extensionElements>
```

The parser also accepts nested `abada:assignee`, `abada:candidateUsers/abada:user`, and `abada:candidateGroups/abada:group` elements with `value` or `expression` attributes. Strategies are `direct` and `claim`. Unknown Abada elements are deployment errors.

