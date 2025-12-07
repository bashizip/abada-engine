import requests
import time
import random
import json

# Configuration
BASE_URL = "http://localhost:5601/abada/api/v1"
WORKER_ID = "demo-worker-python"
TOPIC_NAME = "demo-topic"
LOCK_DURATION = 10000  # 10 seconds

def fetch_and_lock():
    url = f"{BASE_URL}/external-tasks/fetch-and-lock"
    payload = {
        "workerId": WORKER_ID,
        "topics": [TOPIC_NAME],
        "lockDuration": LOCK_DURATION
    }
    try:
        response = requests.post(url, json=payload)
        if response.status_code == 200:
            tasks = response.json()
            return tasks
        else:
            print(f"Error fetching tasks: {response.status_code} - {response.text}")
            return []
    except Exception as e:
        print(f"Connection error: {e}")
        return []

def complete_task(task_id):
    url = f"{BASE_URL}/external-tasks/{task_id}/complete"
    payload = {
        "workerResult": "Success from Python"
    }
    response = requests.post(url, json=payload)
    if response.status_code == 200:
        print(f"Task {task_id} completed successfully.")
    else:
        print(f"Error completing task {task_id}: {response.status_code} - {response.text}")

def report_failure(task_id, error_message):
    url = f"{BASE_URL}/external-tasks/{task_id}/failure"
    payload = {
        "workerId": WORKER_ID,
        "errorMessage": error_message,
        "errorDetails": "Traceback: ... simulated failure ...",
        "retries": 0,
        "retryTimeout": 1000
    }
    response = requests.post(url, json=payload)
    if response.status_code == 200:
        print(f"Task {task_id} reported as failed.")
    else:
        print(f"Error reporting failure for task {task_id}: {response.status_code} - {response.text}")

def main():
    print(f"Starting worker '{WORKER_ID}' for topic '{TOPIC_NAME}'...")
    print("Press Ctrl+C to stop.")
    
    while True:
        try:
            tasks = fetch_and_lock()
            
            if not tasks:
                # No tasks, wait a bit
                time.sleep(2)
                continue
                
            for task in tasks:
                task_id = task['id']
                print(f"Processing task {task_id}...")
                
                # Simulate processing time
                time.sleep(1)
                
                # Randomly succeed or fail
                if random.choice([True, False]):
                    complete_task(task_id)
                else:
                    report_failure(task_id, "Simulated processing error")
                    
        except KeyboardInterrupt:
            print("\nStopping worker...")
            break
        except Exception as e:
            print(f"Unexpected error: {e}")
            time.sleep(5)

if __name__ == "__main__":
    main()
