param(
    [string]$OutputDirectory = ".\backups",
    [int]$RetentionDays = 7
)

$ErrorActionPreference = "Stop"
$resolvedOutput = [System.IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Path $resolvedOutput -Force | Out-Null
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupFile = Join-Path $resolvedOutput "sendit-$timestamp.dump"

docker compose exec -T db pg_dump `
    -U $env:POSTGRES_USER `
    -d $env:POSTGRES_DB `
    -Fc `
    -f /tmp/sendit.dump
docker compose cp db:/tmp/sendit.dump $backupFile

$cutoff = (Get-Date).AddDays(-[Math]::Max(1, $RetentionDays))
Get-ChildItem -LiteralPath $resolvedOutput -Filter "sendit-*.dump" -File |
    Where-Object { $_.LastWriteTime -lt $cutoff } |
    Remove-Item -Force

Write-Output "Backup created: $backupFile"
