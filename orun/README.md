# Abada Orun - Active Operations Cockpit

**Orun** is a modern web-based operations cockpit for the Abada BPMN workflow engine. It provides real-time monitoring, troubleshooting, and management capabilities for business process instances, tasks, and jobs.

![Dark Theme](https://img.shields.io/badge/Theme-Dark-black)
![React](https://img.shields.io/badge/React-19.2.0-blue)
![TypeScript](https://img.shields.io/badge/TypeScript-5.8.2-blue)
![Vite](https://img.shields.io/badge/Vite-6.2.0-purple)

## 🌟 Features

### 📊 Dashboard & Monitoring

- **Real-time metrics** for active processes, tasks, and jobs
- **Visual analytics** with interactive charts (Recharts)
- **Quick stats** showing system health at a glance

### 🔍 Process Instance Management

- **List and filter** all process instances
- **Detailed instance view** with BPMN diagram visualization
- **Live token tracking** showing current execution position
- **Instance controls**: Cancel, Suspend/Resume operations
- **Process variables** inspection and editing

### 🛠️ Data Surgery

- **Edit process variables** on running instances
- **Type-safe modifications** (String, Integer, Boolean, Double, etc.)
- **Add/Delete variables** to fix stuck processes
- **Confirmation workflow** to prevent accidental changes

### 🔧 Job Management ("Fix It")

- **Failed jobs dashboard** with exception details
- **Retry mechanism** for failed external tasks
- **Stack trace viewer** for debugging
- **Bulk operations** support

### 📈 BPMN Visualization

- **Interactive BPMN diagrams** powered by bpmn-js
- **Active token highlighting** showing current execution state
- **Dark theme optimized** for better visibility
- **Zoom and pan** controls

### 🎨 Modern UI/UX

- **Dark theme** with premium aesthetics
- **Responsive design** for all screen sizes
- **Lucide icons** for consistent visual language
- **Smooth animations** and transitions

## 🚀 Quick Start

### Prerequisites

- Node.js 20+ (for local development)
- Docker (for containerized deployment)
- Abada Engine running and accessible

### Local Development

1. **Clone the repository**

   ```bash
   cd /path/to/abada-orun
   ```

2. **Install dependencies**

   ```bash
   npm install
   ```

3. **Configure API endpoint**

   Edit [`config.ts`](./config.ts) to point to your Abada Engine instance:

   ```typescript
   export const API_BASE_URL = 'http://localhost:5601/abada/api';
   ```

4. **Start development server**

   ```bash
   npm run dev
   ```

   The application will be available at `http://localhost:5604`

### Docker Deployment

#### Option 1: Using the refresh script (Recommended)

The project includes a convenient script to build and refresh the Docker container:

```bash
./scripts/refresh-container.sh
```

This script will:

- Build the Docker image (`abada-orun:dev`)
- Stop and remove the old container
- Start a new container on port 5603
- Display container status

#### Option 2: Manual Docker commands

```bash
# Build the image
docker build -t abada-orun:dev .

# Run the container
docker run -d \
  --name abada-orun \
  -p 5603:5603 \
  --restart unless-stopped \
  abada-orun:dev
```

The application will be available at `http://localhost:5603`

### Docker Compose Integration

If you're using Docker Compose with the Abada ecosystem:

```yaml
services:
  orun:
    image: abada-orun:dev
    container_name: abada-orun
    ports:
      - "5603:5603"
    restart: unless-stopped
    networks:
      - abada-network
```

## 📁 Project Structure

```
abada-orun/
├── components/           # React components
│   ├── Dashboard.tsx    # Main dashboard with metrics
│   ├── InstanceList.tsx # Process instances list
│   ├── InstanceDetail.tsx # Instance detail view
│   ├── JobList.tsx      # Failed jobs management
│   ├── Metrics.tsx      # Metrics visualization
│   ├── BPMNViewer.tsx   # BPMN diagram viewer
│   ├── DataSurgeryModal.tsx # Variable editing modal
│   ├── Layout.tsx       # Main layout wrapper
│   └── ui/              # Reusable UI components
├── services/            # API service layer
│   └── api.ts          # API client functions
├── docs/               # Documentation
│   ├── api-docs.md     # Complete API reference
│   ├── user-guide/     # User guides and screenshots
│   ├── data-surgery-fix.md
│   └── container-refresh.md
├── scripts/            # Utility scripts
│   └── refresh-container.sh
├── App.tsx             # Main application component
├── config.ts           # Configuration (API URL)
├── types.ts            # TypeScript type definitions
├── utils.ts            # Utility functions
├── mockData.ts         # Mock data for development
├── Dockerfile          # Docker configuration
├── vite.config.ts      # Vite configuration
└── package.json        # Dependencies and scripts
```

## 🔌 API Integration

Orun communicates with the Abada Engine via REST API. The complete API documentation is available in [`docs/api-docs.md`](./docs/api-docs.md).

### Key Endpoints

| Feature | Endpoint | Description |
|---------|----------|-------------|
| Process Instances | `GET /v1/processes/instances` | List all instances |
| Instance Details | `GET /v1/processes/instances/{id}` | Get instance details |
| BPMN Diagram | `GET /v1/processes/{id}` | Get process definition with XML |
| Active Tokens | `GET /v1/process-instances/{id}/activity-instances` | Get current execution position |
| Variables | `GET /v1/process-instances/{id}/variables` | Get process variables |
| Data Surgery | `PATCH /v1/process-instances/{id}/variables` | Modify variables |
| Failed Jobs | `GET /v1/jobs` | List failed jobs |
| Retry Job | `POST /v1/jobs/{jobId}/retries` | Retry a failed job |
| Cancel Instance | `DELETE /v1/process-instances/{id}` | Cancel a process |
| Suspend/Resume | `PUT /v1/process-instances/{id}/suspension` | Suspend or resume |

### Authentication

Orun now uses the same Keycloak SPA authentication mechanism as Tenda:

- Keycloak login with PKCE
- Route protection with redirect to `/login`
- Automatic token refresh and `Authorization: Bearer <token>` on API calls

Set these environment variables for Vite:

```env
VITE_KEYCLOAK_URL=https://orun.localhost/auth
VITE_KEYCLOAK_REALM=abada-dev
VITE_KEYCLOAK_CLIENT_ID=abada-frontend
VITE_API_URL=/api
```

## 🛠️ Development

### Available Scripts

```bash
# Start development server (port 5604)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

### Port Configuration

- **Local Development**: Port 5604 (configured in `vite.config.ts`)
- **Docker Container**: Port 5603 (configured in `Dockerfile`)

This separation prevents port conflicts when running both environments simultaneously.

### Technology Stack

- **Framework**: React 19.2.0
- **Build Tool**: Vite 6.2.0
- **Language**: TypeScript 5.8.2
- **Routing**: React Router DOM 7.9.6
- **BPMN Rendering**: bpmn-js 18.9.1
- **Charts**: Recharts 3.5.0
- **Icons**: Lucide React 0.555.0

## 📖 User Guides

Detailed user guides are available in the [`docs/user-guide/`](./docs/user-guide/) directory:

- **[Instance Management Guide](./docs/user-guide/instance-management.md)** - Suspend, Resume, Cancel, and Retry operations
- **[Data Surgery Guide](./docs/user-guide/data-surgery.md)** - How to edit process variables
- **[Testing Failures Guide](./docs/testing-failures.md)** - How to create and validate failed jobs
- **[Container Refresh Guide](./docs/container-refresh.md)** - Docker deployment workflow
- **[Data Surgery Fix Documentation](./docs/data-surgery-fix.md)** - Technical implementation details

## 🐛 Troubleshooting

### Common Issues

**Issue**: White page or blank BPMN viewer

- **Solution**: Check browser console for errors. Ensure the API endpoint is correct in `config.ts` and the Abada Engine is running.

**Issue**: Variables not saving

- **Solution**: Verify the PATCH request is being sent to `/v1/process-instances/{id}/variables` with the correct request body format (see API docs).

**Issue**: Port already in use

- **Solution**:
  - For local dev: Change port in `vite.config.ts`
  - For Docker: Change port mapping in `docker run` command or `refresh-container.sh`

**Issue**: Docker build fails

- **Solution**: Ensure you're running the build from the project root directory and `package.json` exists.

## 🤝 Contributing

This project is part of the Abada workflow engine ecosystem. When making changes:

1. Test locally with `npm run dev`
2. Verify Docker build with `docker build -t abada-orun:dev .`
3. Update documentation in `docs/` as needed
4. Test integration with Abada Engine

## 📝 License

This project is part of the Abada workflow engine suite.

## 🔗 Related Projects

- **Abada Engine** - The core BPMN workflow engine
- **Abada Tenda** - Additional Abada ecosystem component

## 📞 Support

For issues, questions, or contributions, please refer to the project documentation in the `docs/` directory.

---

**Built with ❤️ for operational excellence**
