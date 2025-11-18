#!/bin/sh
# Don't use set -e here, we want to handle errors gracefully

# Fix permissions for mounted volumes
# This is needed because volume mounts can override permissions set in Dockerfile
# Create directories if they don't exist (for bind mounts)
mkdir -p /app/data /app/logs

# Fix ownership - this will work if we're running as root (which we are in the container)
# For bind mounts, we need to ensure the directory is writable
fix_permissions() {
    local dir=$1
    if [ -d "$dir" ]; then
        # Try to chown first
        if chown -R appuser:appgroup "$dir" 2>/dev/null; then
            chmod -R 755 "$dir" 2>/dev/null || true
        else
            # If chown fails (e.g., on some filesystems or with SELinux), try to make it writable
            echo "Warning: Could not change ownership of $dir, attempting to make it writable..."
            # Try chmod 777 first
            if chmod -R 777 "$dir" 2>/dev/null; then
                echo "Successfully made $dir writable with chmod 777"
            else
                # If chmod also fails, try a different approach: create a subdirectory we can control
                echo "Warning: chmod failed on $dir, this may be due to filesystem restrictions"
                echo "Attempting to work around by ensuring subdirectories are writable..."
                # Try to create a test file to see if we can write at all
                if touch "$dir/.write_test" 2>/dev/null; then
                    rm -f "$dir/.write_test"
                    echo "Directory $dir is writable despite permission errors"
                else
                    echo "Error: Cannot write to $dir. This may require host-side permission fixes."
                    echo "On the host, run: chmod -R 777 $(dirname $dir)/$(basename $dir)"
                    # Don't exit - let's try to continue and see if the app can work
                fi
            fi
        fi
    fi
}

fix_permissions /app/data
fix_permissions /app/logs

# Switch to appuser and execute the main command
exec su-exec appuser "$@"

