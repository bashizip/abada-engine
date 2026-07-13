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
        throw "mkcert is required to generate trusted local HTTPS certificates. Install from https://github.com/FiloSottile/mkcert"
    }

    $certDir = "docker/traefik/certs"
    $certFile = Join-Path $certDir "localhost.pem"
    $keyFile = Join-Path $certDir "localhost-key.pem"

    New-Item -ItemType Directory -Force -Path $certDir | Out-Null

    $needsGenerate = $true
    if ((Test-Path $certFile) -and (Test-Path $keyFile)) {
        $needsGenerate = $false
    }

    if ($needsGenerate) {
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
            grafana.localhost `
            jaeger.localhost `
            keycloak.localhost `
            traefik.localhost

        Write-Step "Local TLS certs ready at $certDir"
    } else {
        Write-Host "i Local TLS certs already exist at $certDir."
    }
}

function Check-ReleaseAssets {
    $requiredPaths = @(
        "docker/grafana/provisioning",
        "docker/grafana/dashboards",
        "docker/loki-config-prod.yaml",
        "docker/otel-collector-config.yaml",
        "docker/prometheus.yml",
        "docker/promtail-config.yaml",
        "docker/traefik/dynamic.yml",
        "docker/traefik/traefik.yml"
    )

    $missing = @()
    foreach ($path in $requiredPaths) {
        if (-not (Test-Path $path)) {
            $missing += $path
        }
    }

    if ($missing.Count -gt 0) {
        Write-Host "Error: release assets are missing for docker-compose.release.yml."
        Write-Host "Run this quickstart from the repository root, or use a release bundle that includes the docker/ directory."
        Write-Host "Missing paths:"
        foreach ($path in $missing) {
            Write-Host "  - $path"
        }
        throw "Required release assets are missing."
    }

    New-Item -ItemType Directory -Force -Path "logs" | Out-Null
}

function Verify-LocalTls {
    Write-Step "Verifying local TLS certificate and trust..."

    if (-not (Test-Command "mkcert")) {
        throw "mkcert is required to verify trusted local HTTPS certificates."
    }

    $certFile = "docker/traefik/certs/localhost.pem"
    $keyFile = "docker/traefik/certs/localhost-key.pem"
    if (-not (Test-Path $certFile) -or -not (Test-Path $keyFile)) {
        throw "Local TLS cert files are missing: $certFile and/or $keyFile"
    }

    $caroot = (& mkcert -CAROOT).Trim()
    if ([string]::IsNullOrWhiteSpace($caroot)) {
        throw "mkcert CAROOT not found. Run 'mkcert -install' and retry."
    }

    $rootCa = Join-Path $caroot "rootCA.pem"
    if (-not (Test-Path $rootCa)) {
        throw "mkcert root CA not found at $rootCa. Run 'mkcert -install' and retry."
    }

    if (Test-Command "certutil") {
        $dump = & certutil -dump $certFile 2>$null
        $requiredDns = @(
            "localhost",
            "tenda.localhost",
            "orun.localhost",
            "grafana.localhost",
            "keycloak.localhost",
            "jaeger.localhost",
            "traefik.localhost"
        )

        foreach ($dns in $requiredDns) {
            if ($dump -notmatch [regex]::Escape($dns)) {
                throw "TLS certificate is missing SAN '$dns'."
            }
        }
    }

    Write-Host "Local HTTPS certificate is installed and valid for *.localhost"
}

function Start-Platform {
    Write-Step "Starting Abada Platform..."
    & docker compose -f $LocalFile up -d

    Write-Host ""
    Write-Step "Platform available at:"
    Write-Host "  - Engine API : https://localhost/api/"
    Write-Host "  - Swagger UI : https://localhost/api/swagger-ui.html"
    Write-Host "  - Tenda UI   : https://tenda.localhost"
    Write-Host "  - Orun UI    : https://orun.localhost"
    Write-Host "  - Grafana    : https://grafana.localhost"
    Write-Host "  - Keycloak   : https://keycloak.localhost"
    Write-Host "  - Jaeger     : https://jaeger.localhost"
    Write-Host "  - Traefik    : https://traefik.localhost"
    Write-Host ""
    Write-Host "Run 'docker compose -f $LocalFile down' to stop."
}

Write-Header
Check-Prerequisites
Download-Compose
Check-ReleaseAssets
Setup-LocalTls
Verify-LocalTls
Start-Platform
