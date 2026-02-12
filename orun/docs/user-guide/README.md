# Orun User Guide

Welcome to the Orun Active Operations Cockpit user documentation.

## Table of Contents

### Getting Started

- [Overview](#overview)
- [Accessing Orun](#accessing-orun)

### Features

#### Process Management

- **[Instance Management Guide](instance-management.md)** - Suspend, Resume, and Cancel operations
  - Safe suspension and resumption of processes
  - Secure cancellation with verification codes
  - Status monitoring

#### Data Surgery

- **[Data Surgery Guide](data-surgery.md)** - Complete guide to managing process variables
  - View and edit existing variables
  - Add new variables to running processes
  - Delete variables
  - Validation and best practices

### Administration

- [Container Refresh](../container-refresh.md) - Docker container management
- [API Documentation](../api-docs.md) - REST API reference

## Overview

Orun is an active operations cockpit for the Abada workflow engine. It provides:

- **Process Instance Management** - Monitor and control running processes
- **Data Surgery** - Modify process variables in real-time
- **Job Management** - Retry failed jobs and view error details
- **BPMN Visualization** - View process diagrams with active tokens
- **Metrics & Monitoring** - Track process performance

## Accessing Orun

### Docker Container

```
http://localhost:5603
```

### Local Development

```
http://localhost:5604
```

## Quick Links

- [Instance Management Guide](instance-management.md)
- [Data Surgery User Guide](data-surgery.md)
- [API Documentation](../api-docs.md)
- [Container Refresh Guide](../container-refresh.md)
- [Technical Documentation](../data-surgery-fix.md)

## Support

For technical issues or questions, please refer to the troubleshooting sections in each guide or contact your system administrator.

---

**Version**: 1.0  
**Last Updated**: December 6, 2025
