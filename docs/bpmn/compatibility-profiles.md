# BPMN compatibility profiles

Abada compiles BPMN XML into a vendor-neutral canonical process model at deployment. Runtime execution never interprets vendor XML.

The default deployment enables `standard-bpmn-2.0`, `abada-native-1`, and `camunda-7`. The multipart deployment endpoint accepts `profiles` as a comma-separated list, plus `strict` and `rejectVendorExtensions` booleans. Standard BPMN is always active.

`strict=true` rejects every unsupported vendor directive. Compatibility mode reports unknown metadata directives as warnings, but always rejects unknown execution-relevant directives. A task cannot define assignment through multiple dialects.

Validation failures contain stable `ABADA-BPMN-*` issue codes, severity, element and namespace context, and a suggested resolution. A successful deployment returns and persists its detected profiles, mappings, issues, compiler version, and definition format version.

