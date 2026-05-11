# Ejecutar en PowerShell (opcional): anade supabase.exe al PATH de la sesion actual.
$cliDir = Join-Path $PSScriptRoot "..\tools\supabase-cli" | Resolve-Path
$env:PATH = "$cliDir;$env:PATH"
Write-Host "Supabase CLI en PATH para esta sesion: $(Join-Path $cliDir 'supabase.exe')"
