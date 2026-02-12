# Orun Container Refresh Scripts

This directory contains scripts to rebuild and refresh the Orun Docker container.

## Scripts

### `refresh-container.sh`

Main script to rebuild the Docker image and refresh the running container.

**Usage:**

```bash
./refresh-container.sh
```

**What it does:**

1. Builds a new Docker image tagged as `abada-orun:dev`
2. Stops the running `abada-orun` container
3. Removes the old container
4. Starts a new container with the updated image
5. Maps port 5603 (host) to 5603 (container)
6. Sets restart policy to `unless-stopped`

**Requirements:**

- Docker must be installed and running
- User must have permission to run Docker commands

## Container Details

- **Container Name:** `abada-orun`
- **Image Name:** `abada-orun:dev`
- **Port Mapping:** `5603:5603` (host:container)
- **Restart Policy:** `unless-stopped`
- **Application URL:** <http://localhost:5603>

## Port Configuration

The project uses different ports for different environments to avoid conflicts:

| Environment | Port | URL | Configuration |
|-------------|------|-----|---------------|
| **Docker Container** | 5603 | <http://localhost:5603> | Dockerfile CMD |
| **Local Development** | 5604 | <http://localhost:5604> | vite.config.ts |

This allows you to:

- Run the Docker container for testing the production-like build
- Run local development (`npm run dev`) for active development
- Run both simultaneously without port conflicts

**Note:** The Dockerfile uses `--port 5603` in the CMD, which overrides the vite.config.ts setting when running in Docker.

## Troubleshooting

### Permission Denied

If you get a permission error, make sure the script is executable:

```bash
chmod +x refresh-container.sh
```

### Docker Not Running

If you get "Cannot connect to the Docker daemon", start Docker:

```bash
sudo systemctl start docker
```

### Port Already in Use

If port 5603 is already in use, you can modify the port mapping in the script:

```bash
# Change this line in refresh-container.sh:
-p 5603:5603
# To use a different port, e.g., 5604:
-p 5604:5603
```

### View Container Logs

To view the container logs:

```bash
docker logs abada-orun
```

To follow logs in real-time:

```bash
docker logs -f abada-orun
```

## Manual Commands

If you need to run commands manually:

**Build image:**

```bash
docker build -t abada-orun:dev .
```

**Stop container:**

```bash
docker stop abada-orun
```

**Remove container:**

```bash
docker rm abada-orun
```

**Start container:**

```bash
docker run -d --name abada-orun -p 5603:5603 --restart unless-stopped abada-orun:dev
```

**Check container status:**

```bash
docker ps --filter "name=abada-orun"
```
