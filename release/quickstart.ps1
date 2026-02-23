$ErrorActionPreference = "Stop"

# Abada Platform Quickstart Script (Windows/PowerShell)
# Downloads and starts the Abada Platform

# Configuration
$ReleaseUrl = "https://raw.githubusercontent.com/bashizip/abada-engine/main/release/docker-compose.release.yml"
$LocalFile = "docker-compose.release.yml"

function Write-Header {
    Write-Host ""
    Write-Host "    _    _               _        "
    Write-Host "   / \  | |__   __ _  __| | __ _  "
    Write-Host "  / _ \ | '_ \ / _` |/ _` |/ _` | "
    Write-Host " / ___ \| |_) | (_| | (_| | (_| | "
    Write-Host "/_/   \_\_.__/ \__,_|\__,_|\__,_| "
    Write-Host "                                  "
    Write-Host "   Quickstart Launcher            "
    Write-Host ""
}

function Write-Step {
    param([string]$Message)
    Write-Host "==> $Message"
}

function Test-Command {
    param([string]$Name)
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

function Check-Prerequisites {
    Write-Step "Checking prerequisites..."

    if (-not (Test-Command "docker")) {
        throw "docker is not installed. Please install Docker Desktop from https://www.docker.com/products/docker-desktop"
    }

    & docker compose version | Out-Null
    Write-Host "Docker and Docker Compose found"
}

function Download-Compose {
    Write-Step "Downloading latest configuration..."
    Invoke-WebRequest -Uri $ReleaseUrl -OutFile $LocalFile -UseBasicParsing
    Write-Host "Configuration downloaded"
}

function Setup-LocalTls {
    Write-Step "Preparing local HTTPS certificates..."

    if (-not (Test-Path $LocalFile)) {
        Write-Host "i Compose file '$LocalFile' not found. Skipping local TLS setup."
        return
    }

    $composeText = Get-Content -Raw -Path $LocalFile
    $hasLocalhostRoutes = $composeText -match 'Host\(`[^`]*localhost`\)|\.localhost'
    $hasHttps = $composeText -match '443:443|published:\s*"?443"?|--entrypoints\.websecure\.address=:443|entrypoints=web,websecure|\.tls=true'

    if (-not $hasLocalhostRoutes) {
        Write-Host "i No localhost host rules detected in $LocalFile; skipping mkcert setup."
        return
    }

    if (-not $hasHttps) {
        Write-Host "i No HTTPS entrypoint detected in $LocalFile; skipping mkcert setup."
        return
    }

    if (-not (Test-Command "mkcert")) {
        Write-Host "Warning: mkcert is not installed. Install mkcert to avoid browser SSL warnings for https://*.localhost."
        Write-Host "https://github.com/FiloSottile/mkcert"
        return
    }

    $certDir = "docker/traefik/certs"
    $certFile = Join-Path $certDir "localhost.pem"
    $keyFile = Join-Path $certDir "localhost-key.pem"

    New-Item -ItemType Directory -Force -Path $certDir | Out-Null

    if ((Test-Path $certFile) -and (Test-Path $keyFile)) {
        Write-Host "i Local TLS certs already exist at $certDir."
        return
    }

    Write-Step "Installing mkcert local CA (if not already installed)..."
    & mkcert -install

    Write-Step "Generating TLS certificate for localhost domains..."
    & mkcert `
        -cert-file $certFile `
        -key-file $keyFile `
        localhost `
        "*.localhost" `
        tenda.localhost `
        orun.localhost `
        keycloak.localhost `
        traefik.localhost

    Write-Step "Local TLS certs ready at $certDir"
}

function Start-Platform {
    Write-Step "Starting Abada Platform..."
    & docker compose -f $LocalFile up -d

    Write-Host ""
    Write-Step "Platform available at:"
    Write-Host "  - Engine : http://localhost:5601/api/"
    Write-Host "  - Tenda  : http://localhost:5602"
    Write-Host "  - Orun   : http://localhost:5603"
    Write-Host "  - Grafana: http://localhost:3000"
    Write-Host ""
    Write-Host "Run 'docker compose -f $LocalFile down' to stop."
}

Write-Header
Check-Prerequisites
Download-Compose
Setup-LocalTls
Start-Platform
