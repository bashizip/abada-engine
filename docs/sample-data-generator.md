# Sample Data Generator

This utility generates sample data to preload the Abada Engine with process instances in various states for testing and demonstration purposes.

## Overview

The Sample Data Generator creates **6 process instances** using two BPMN processes:

- **recipe-cook.bpmn** (4 instances)
- **parallel-gateway-test.bpmn** (2 instances)

Each instance demonstrates different scenarios including completed processes, failed processes, in-progress processes at various stages, and processes with different variable values.

## Usage

### Option 1: Using the Shell Script (Recommended)

```bash
./scripts/generate-sample-data.sh
```

This script will:

1. Start the Abada Engine
2. Deploy the BPMN processes
3. Generate sample data
4. Leave the engine running with the sample data loaded

### Option 2: Using Maven Directly

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--abada.generate-sample-data=true"
```

### Option 3: Using Environment Variable

```bash
export ABADA_GENERATE_SAMPLE_DATA=true
./mvnw spring-boot:run
```

### Option 4: Using application.properties

Add the following to your `application.properties`:

```properties
abada.generate-sample-data=true
```

Then run the application normally:

```bash
./mvnw spring-boot:run
```

## Generated Scenarios

### Recipe Cook Process (recipe-cook.bpmn)

#### Scenario 1: Completed Recipe Process

- **User**: alice
- **State**: Completed
- **Flow**:
  1. Alice chooses a recipe (goodOne=true, recipeName="Spaghetti Carbonara")
  2. Bob cooks the recipe (cookingTime=30, rating=5)
  3. Process completes successfully

#### Scenario 2: Waiting at Choose Recipe

- **User**: jeannot
- **State**: In-progress
- **Flow**: Process started, waiting at `choose-recipe` task (not yet claimed)

#### Scenario 3: Back to Choose Recipe (Loop)

- **User**: black
- **State**: In-progress (looped back)
- **Flow**:
  1. Black chooses a recipe (goodOne=false, recipeName="Mystery Dish")
  2. Process loops back to `choose-recipe` task due to exclusive gateway condition

#### Scenario 4: Waiting at Cook Recipe

- **User**: alice (starter), waiting for cuistos group
- **State**: In-progress
- **Flow**:
  1. Alice chooses a recipe (goodOne=true, recipeName="Chicken Tikka Masala")
  2. Process advances to `cook-recipe` task
  3. Waiting for a cook (cuistos group) to claim and complete

#### Scenario 7: Explicitly Failed Process

- **User**: system
- **State**: Failed
- **Flow**:
  1. Process started
  2. Process explicitly failed via `abadaEngine.failProcess()`
  3. Status set to FAILED, end date set

#### Scenario 8: Explicitly Failed Task

- **User**: alice
- **State**: In-progress (Task Failed)
- **Flow**:
  1. Process started
  2. Alice claims `choose-recipe` task
  3. Task explicitly failed via `abadaEngine.failTask()`
  4. Task status set to FAILED

### Parallel Gateway Process (parallel-gateway-test.bpmn)

#### Scenario 5: Completed Parallel Process

- **User**: test-user
- **State**: Completed
- **Flow**:
  1. Complete InitialTask
  2. Process forks into TaskA and TaskB
  3. Both parallel tasks completed
  4. Process joins and completes

#### Scenario 6: One Parallel Branch Completed

- **User**: test-user
- **State**: In-progress (waiting at parallel join)
- **Flow**:
  1. Complete InitialTask
  2. Process forks into TaskA and TaskB
  3. TaskA completed
  4. TaskB still pending (parallel join not yet satisfied)

## User Roles

The generator uses the following users and groups as defined in the BPMN processes:

### Recipe Cook Process

- **Users**: alice, bob, black, jeannot
- **Groups**:
  - `customers` - Can start process and complete choose-recipe task
  - `cuistos` - Can complete cook-recipe task

### Parallel Gateway Process

- **Users**: test-user
- **Groups**: test-group

## Implementation Details

The sample data generator is implemented as a Spring Boot `CommandLineRunner` component that:

1. Only runs when `abada.generate-sample-data=true` is set
2. Deploys the required BPMN processes
3. Creates process instances with realistic data
4. Advances processes through various states
5. Uses proper user authentication and group membership
6. Sets meaningful process variables

## Code Location

- **Generator Class**: `src/main/java/com/abada/engine/util/SampleDataGenerator.java`
- **Shell Script**: `scripts/generate-sample-data.sh`
- **BPMN Files**:
  - `src/main/resources/bpmn/recipe-cook.bpmn`
  - `src/test/resources/bpmn/parallel-gateway-test.bpmn`

## Verification

After running the generator, you can verify the data using:

### API Endpoints

```bash
# List all process instances
curl http://localhost:8080/v1/processes/instances

# List all tasks
curl -H "X-User: alice" -H "X-Groups: customers" http://localhost:8080/v1/tasks

# Get user statistics
curl -H "X-User: alice" -H "X-Groups: customers" http://localhost:8080/v1/tasks/user-stats
```

### Database Queries

If using PostgreSQL:

```sql
-- View all process instances
SELECT id, process_definition_id, status, started_by, start_date, end_date 
FROM process_instances;

-- View all tasks
SELECT id, task_definition_key, name, assignee, status, start_date, end_date 
FROM tasks;
```

## Customization

To customize the sample data:

1. Edit `SampleDataGenerator.java`
2. Add new scenario methods
3. Modify existing scenarios with different variables or user assignments
4. Add more process instances by calling additional scenario methods

## Notes

- The generator runs automatically on application startup when enabled
- It's designed for development and testing environments only
- Sample data is created in-memory and persisted to the configured database
- The generator uses the same APIs as the test suite for consistency
- All scenarios follow the BPMN process definitions exactly

## Troubleshooting

### Generator doesn't run

- Ensure `abada.generate-sample-data=true` is set
- Check application logs for any errors during startup

### BPMN files not found

- Verify BPMN files exist in the expected locations
- Check that the classpath includes both `src/main/resources` and `src/test/resources`

### User/Group permission errors

- Verify that user and group assignments match the BPMN definitions
- Check the candidate users and groups defined in the BPMN files
