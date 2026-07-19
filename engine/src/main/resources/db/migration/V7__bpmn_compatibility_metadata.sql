ALTER TABLE process_definitions ADD COLUMN definition_format_version VARCHAR(32) DEFAULT 'legacy-1';
ALTER TABLE process_definitions ADD COLUMN compatibility_profiles TEXT DEFAULT 'standard-bpmn-2.0,abada-native-1,camunda-7';
ALTER TABLE process_definitions ADD COLUMN detected_namespaces TEXT DEFAULT '';
ALTER TABLE process_definitions ADD COLUMN compiler_version VARCHAR(32) DEFAULT '1';
ALTER TABLE process_definitions ADD COLUMN compatibility_report TEXT DEFAULT '{"detectedProfiles":[],"mappings":[],"issues":[]}';

ALTER TABLE process_definitions ALTER COLUMN definition_format_version SET NOT NULL;
ALTER TABLE process_definitions ALTER COLUMN compatibility_profiles SET NOT NULL;
ALTER TABLE process_definitions ALTER COLUMN detected_namespaces SET NOT NULL;
ALTER TABLE process_definitions ALTER COLUMN compiler_version SET NOT NULL;
ALTER TABLE process_definitions ALTER COLUMN compatibility_report SET NOT NULL;

ALTER TABLE tasks ADD COLUMN assignment_strategy VARCHAR(16) DEFAULT 'CLAIM';
UPDATE tasks SET assignment_strategy = CASE WHEN assignee IS NULL OR assignee = '' THEN 'CLAIM' ELSE 'DIRECT' END;
ALTER TABLE tasks ALTER COLUMN assignment_strategy SET NOT NULL;
