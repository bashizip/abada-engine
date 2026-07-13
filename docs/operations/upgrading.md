# Upgrading Abada Engine

1. Back up PostgreSQL and verify that the backup can be restored.
2. Stop all engine replicas for upgrades that cross a schema version until
   rolling-upgrade compatibility is explicitly listed in the release notes.
3. Start one new engine replica. Flyway applies pending migrations before
   Hibernate validates the schema.
4. Check `/api/actuator/health`, migration logs, and failed jobs.
5. Start remaining replicas and exercise a representative process.

Existing databases created by Hibernate are baselined at Flyway version 1 and
then upgraded by version 2. New databases execute both migrations. Never edit a
migration that has shipped; add a new versioned migration instead.

Process definitions are immutable after deployment. Redeploying changed BPMN
under the same process key creates a new version. New instances use the latest
version, while existing instances retain their original deployment ID.
