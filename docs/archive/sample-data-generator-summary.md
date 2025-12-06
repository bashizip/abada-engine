# Sample Data Generator - Summary

## Overview

A comprehensive sample data generation system has been created for the Abada Engine. This system generates realistic process instances in various states for testing, development, and demonstration purposes.

## What Was Created

### 1. Main Generator Class

**File**: `src/main/java/com/abada/engine/util/SampleDataGenerator.java`

A Spring Boot CommandLineRunner component that:

- Automatically runs on application startup when enabled
- Deploys BPMN processes from classpath
- Creates 6 process instances with different scenarios
- Uses proper user roles and group assignments
- Sets realistic process variables

### 2. Shell Script

**File**: `scripts/generate-sample-data.sh`

A convenient script to run the generator:

```bash
./scripts/generate-sample-data.sh
```

### 3. Documentation

**Files**:

- `docs/sample-data-generator.md` - Complete user guide
- `docs/sample-data-generator-config.example` - Configuration examples
- `scripts/README.md` - Updated with generator documentation

## Generated Scenarios

### Recipe Cook Process (4 instances)

1. **Completed Process**
   - User: alice → bob
   - Variables: recipeName="Spaghetti Carbonara", cookingTime=30, rating=5
   - Status: ✅ Completed

2. **Waiting at Choose Recipe**
   - User: jeannot
   - Status: ⏸️ Available (not claimed)

3. **Looped Back (goodOne=false)**
   - User: black
   - Variables: recipeName="Mystery Dish", goodOne=false
   - Status: 🔄 Back at choose-recipe

4. **Waiting at Cook Recipe**
   - User: alice (completed choose-recipe)
   - Variables: recipeName="Chicken Tikka Masala", goodOne=true
   - Status: ⏸️ Waiting for cuistos group

5. **Explicitly Failed Process**
   - User: system
   - Status: ❌ Failed (via API)

6. **Explicitly Failed Task**
   - User: alice
   - Status: ⚠️ Task Failed (via API)

### Parallel Gateway Process (2 instances)

7. **Completed Parallel Process**
   - User: test-user
   - Variables: taskAResult="Branch A completed", taskBResult="Branch B completed"
   - Status: ✅ Completed

8. **One Branch Completed**
   - User: test-user
   - Variables: taskAResult="Branch A completed"
   - Status: ⏸️ Waiting at TaskB (parallel join not satisfied)

## User Roles

### Recipe Cook Process

- **alice**: customers group, can choose recipes
- **bob**: cuistos group, can cook recipes
- **black**: customers group, can choose recipes
- **jeannot**: customers group, can choose recipes

### Parallel Gateway Process

- **test-user**: test-group, can complete all tasks

## How to Use

### Option 1: Shell Script (Recommended)

```bash
./scripts/generate-sample-data.sh
```

### Option 2: Maven Command

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--abada.generate-sample-data=true"
```

### Option 3: Environment Variable

```bash
export ABADA_GENERATE_SAMPLE_DATA=true
./mvnw spring-boot:run
```

### Option 4: Configuration File

Add to `application.properties`:

```properties
abada.generate-sample-data=true
```

## Verification

After running the generator, verify the data:

### API Endpoints

```bash
# List all process instances
curl http://localhost:8080/v1/processes/instances

# List tasks for alice
curl -H "X-User: alice" -H "X-Groups: customers" \
  http://localhost:8080/v1/tasks

# Get user statistics
curl -H "X-User: alice" -H "X-Groups: customers" \
  http://localhost:8080/v1/tasks/user-stats
```

### Expected Results

- 6 process instances created
- Multiple tasks in different states (AVAILABLE, CLAIMED, COMPLETED)
- Process variables set correctly
- User assignments match BPMN definitions

## Technical Details

### Implementation

- Uses `@ConditionalOnProperty` to enable/disable
- Implements `CommandLineRunner` for startup execution
- Loads BPMN files from classpath (both main and test resources)
- Uses the same APIs as the test suite for consistency

### Dependencies

- Spring Boot
- AbadaEngine
- TaskManager
- BPMN files:
  - `src/main/resources/bpmn/recipe-cook.bpmn`
  - `src/test/resources/bpmn/parallel-gateway-test.bpmn`

### Compilation

The code compiles successfully:

```bash
./mvnw clean compile -DskipTests
# BUILD SUCCESS
```

## Benefits

1. **Quick Setup**: One command to populate the engine with realistic data
2. **Consistent**: Uses the same BPMN processes as tests
3. **Comprehensive**: Covers multiple scenarios and states
4. **Configurable**: Easy to enable/disable via configuration
5. **Well-Documented**: Complete documentation and examples
6. **Production-Safe**: Disabled by default, only runs when explicitly enabled

## Next Steps

To customize the sample data:

1. Edit `SampleDataGenerator.java`
2. Add new scenario methods
3. Modify variables or user assignments
4. Add more process instances

For more information, see:

- [Complete Documentation](sample-data-generator.md)
- [Configuration Examples](sample-data-generator-config.example)
- [Scripts README](../scripts/README.md)
