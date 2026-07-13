#!/usr/bin/env pwsh

# Abada Platform - Development Quick Start (PowerShell)
# For Windows users

$ErrorActionPreference = "Stop"

# Colors
$Green = [ConsoleColor]::Green
$Blue = [ConsoleColor]::Blue
$Yellow = [ConsoleColor]::Yellow
$Red = [ConsoleColor]::Red

function Write-Color {
    param($Color, $Text)
    $Host.UI.WriteConsoleLine($Color, $Text)
}

function Write-Success { Write-Color $Green $args[0] }
function Write-Info { Write-Color $Blue $args[0] }
function Write-Warning { Write-Color $Yellow $args[0] }
function Write-Error { Write-Color $Red $args[0] }

function Print-Banner {
    Write-Info @"
 _    _  _____  __   __ _____  _____  _______  _____   _____   _____  _______
| |  | ||_   _| \ \ / /|_   _||  _  ||__   __||  _  | /  ___||  _  ||_   _  |
| |  | |  | |    \ V /   | |  | | | |   | |   | | | | \ `--. | | | |  | |  |
| |/\| |  | |     \ /    | |  | | | |   | |   | | | |  `--. \| | | |  | |  |
\  /\  / _| |_    | |    | |  \ \_/ /   | |   \ \_/ / /\__/ /| \_/ / _| |_  |
 \/  \/ |_____|   \_/    \_/   \___/    \_/    \___/  \____/  \___/ |_____| |

=== Abada Platform - Development Quick Start ===
"@
}

function Check-Prerequisites {
    Write-Warning "Checking prerequisites..."

    # Check Docker
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Write-Error "❌ Docker is not installed."
        Write-Info "Please install Docker Desktop: https://www.docker.com/products/docker-desktop"
        exit 1
    }
    $dockerVersion = docker --version
    Write-Success "✓ Docker is installed ($dockerVersion)"

    # Check Docker Compose
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Write-Error "❌ Docker Compose is not installed."
        exit 1
    }
    $composeVersion = docker compose version --short
    Write-Success "✓ Docker Compose is installed ($composeVersion)"

    Write-Host ""
}

function Setup-TLS {
    Write-Warning "Setting up local TLS certificates..."

    $scriptPath = Join-Path $PSScriptRoot "setup-local-tls.ps1"
    if (Test-Path $scriptPath) {
        & $scriptPath
    } else {
        Write-Warning "⚠ TLS setup script not found. Using self-signed certificates."
        Write-Info "To avoid browser SSL warnings, install mkcert: https://github.com/FiloSottile/mkcert"
    }
    Write-Host ""
}

function Start-Stack {
    Write-Warning "Starting Abada Platform stack..."
    Write-Info "This may take a few minutes on first run..."
    Write-Host ""

    $rootDir = Split-Path $PSScriptRoot -Parent
    $rootDir = Split-Path $rootDir -Parent

    docker compose -f (Join-Path $rootDir "docker-compose.yml") -f (Join-Path $rootDir "docker-compose.dev.yml") up -d

    Write-Host ""
    Write-Success "✓ Containers started!"
    Write-Host ""
}

function Wait-for-Services {
    Write-Warning "Waiting for services to be healthy..."

    $maxAttempts = 30
    $attempt = 1
    $rootDir = Split-Path $PSScriptRoot -Parent
    $rootDir = Split-Path $rootDir -Parent

    while ($attempt -le $maxAttempts) {
        $status = docker compose -f (Join-Path $rootDir "docker-compose.yml") -f (Join-Path $rootDir "docker-compose.dev.yml") ps abada-engine
        if ($status -match "healthy") {
            Write-Success "✓ Engine is healthy"
            break
        }
        Write-Host -NoNewline "Waiting for engine... (attempt $attempt/$maxAttempts)`r"
        Start-Sleep -Seconds 2
        $attempt++
    }

    if ($attempt -gt $maxAttempts) {
        Write-Warning "⚠ Engine took longer than expected to start."
        Write-Info "Check logs with: docker compose logs abada-engine"
    }

    Write-Host ""
}

function Show-Status {
    Write-Info "=== Service Status ==="
    $rootDir = Split-Path $PSScriptRoot -Parent
    $rootDir = Split-Path $rootDir -Parent
    docker compose -f (Join-Path $rootDir "docker-compose.yml") -f (Join-Path $rootDir "docker-compose.dev.yml") ps
    Write-Host ""
}

function Show-AccessInfo {
    Write-Success "============================================"
    Write-Success "  🎉 Abada Platform is running!"
    Write-Success "============================================"
    Write-Host ""

    Write-Info "Service URLs:"
    Write-Host "  🔌 Engine API:     https://localhost/api/"
    Write-Host "  📚 Swagger UI:    https://localhost/api/swagger-ui.html"
    Write-Host "  📋 Tenda UI:      https://tenda.localhost"
    Write-Host "  📊 Orun Dashboard:https://orun.localhost"
    Write-Host "  🔐 Keycloak:      https://keycloak.localhost"
    Write-Host "  📈 Grafana:       https://grafana.localhost (admin/admin123)"
    Write-Host "  🔍 Jaeger:        https://jaeger.localhost"
    Write-Host "  🚦 Traefik:       https://traefik.localhost"
    Write-Host ""

    Write-Info "Test Credentials:"
    Write-Host "  👤 Admin:  admin / admin (Platform Administrator)"
    Write-Host "  👤 Alice:  alice / alice (Customer)"
    Write-Host "  👤 Bob:    bob / bob (Custos)"
    Write-Host ""

    Write-Info "Useful Commands:"
    Write-Host "  📋 View logs:     docker compose logs -f"
    Write-Host "  🛑 Stop stack:    docker compose down"
    Write-Host "  🔄 Restart:       docker compose restart"
    Write-Host "  📊 Status:        docker compose ps"
    Write-Host ""

    Write-Warning "⚠ Note: This is a development/demo environment."
    Write-Warning "⚠ For production deployment, see docs/operations/docker-deployment.md"
    Write-Host ""
}

# Main execution
Print-Banner
Check-Prerequisites
Setup-TLS
Start-Stack
Wait-for-Services
Show-Status
Show-AccessInfo

Write-Success "Done!"
