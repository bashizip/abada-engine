# Data Surgery Fix - Variable Edition

## Summary

Fixed the **Data Surgery** feature to properly update process variables via the API.

## Issues Found

### 1. Variable Update Not Working ❌

**Location:** `/components/InstanceDetail.tsx` - `handleVariableUpdate` function

**Problem:**

- The function was only refreshing data without actually calling the API
- Variables were not being persisted to the backend
- Comments indicated this was a known TODO

**Fix:**

- Implemented proper change detection by comparing new variables with original ones
- Calls `api.updateProcessVariable()` for each changed variable
- Waits for all updates to complete before refreshing
- Added error handling with user feedback

**Code Changes:**

```typescript
const handleVariableUpdate = async (newVars: Variable[]) => {
    if (!instance) return;
    
    try {
        // Compare new variables with original variables and update only changed ones
        const updatePromises: Promise<void>[] = [];
        
        for (const newVar of newVars) {
            const originalVar = variables.find(v => v.name === newVar.name);
            
            // Check if the variable has changed
            if (originalVar && JSON.stringify(originalVar.value) !== JSON.stringify(newVar.value)) {
                console.log(`Updating variable ${newVar.name}: ${originalVar.value} -> ${newVar.value}`);
                updatePromises.push(
                    api.updateProcessVariable(instance.id, newVar.name, newVar.value, newVar.type)
                );
            }
        }
        
        // Wait for all updates to complete
        if (updatePromises.length > 0) {
            await Promise.all(updatePromises);
            console.log(`Successfully updated ${updatePromises.length} variable(s)`);
        }
        
        // Refresh data to show updated values
        await fetchInstanceData();
    } catch (err) {
        console.error('Failed to update variables:', err);
        alert('Failed to update variables. Please check the console for details.');
    }
};
```

### 2. Job Retry Missing Request Body ❌

**Location:** `/services/api.ts` - `retryJob` function

**Problem:**

- According to API docs (line 514-523), the endpoint expects a JSON body: `{"retries": 3}`
- The implementation was sending a POST request with no body
- This would likely cause the backend to reject the request

**Fix:**

- Added `retries` parameter with default value of 3
- Added proper headers and request body
- Now matches API specification exactly

**Code Changes:**

```typescript
retryJob: async (jobId: string, retries: number = 3): Promise<void> => {
    const response = await fetch(`${API_BASE_URL}/v1/jobs/${jobId}/retries`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ retries }),
    });
    return handleResponse<void>(response);
},
```

## API Specification Compliance

### Variable Update Endpoint

According to `docs/api-docs.md` (lines 588-615):

- **Endpoint:** `PATCH /v1/process-instances/{instanceId}/variables`
- **Request Body:**

  ```json
  {
    "modifications": {
      "variableName": {
        "value": newValue,
        "type": "String|Integer|Long|Double|Float|Boolean"
      }
    }
  }
  ```

- **Response:** `200 OK` with empty body

✅ Our implementation correctly follows this specification.

### Job Retry Endpoint

According to `docs/api-docs.md` (lines 510-528):

- **Endpoint:** `POST /v1/jobs/{jobId}/retries`
- **Request Body:**

  ```json
  {
    "retries": 3
  }
  ```

- **Response:** `200 OK` with empty body

✅ Our implementation now correctly follows this specification.

## Testing Checklist

To verify the fix works:

1. **Start the application:**
   - Backend API should be running
   - Frontend dev server: `npm run dev` (port 5604)

2. **Test Variable Editing:**
   - Navigate to a running process instance
   - Click "Data Surgery" button
   - Modify one or more variables
   - Click "Review Changes"
   - Click "Confirm & Apply"
   - Verify console shows: `Updating variable X: oldValue -> newValue`
   - Verify console shows: `Successfully updated N variable(s)`
   - Check that the variables tab shows the updated values
   - Verify backend received the PATCH request

3. **Test Job Retry:**
   - Navigate to Jobs page
   - Find a failed job
   - Click "Retry" button
   - Verify the request includes `{"retries": 3}` in the body
   - Check that the job is retried successfully

## Files Modified

1. `/components/InstanceDetail.tsx` - Fixed variable update logic
2. `/services/api.ts` - Fixed job retry request body

## Related Documentation

- API Documentation: `/docs/api-docs.md`
  - Variable Management: Lines 552-625
  - Job Management: Lines 484-550
