<#
.SYNOPSIS
  Borra el contenedor y su volumen para repetir toda la prueba desde cero (solo afecta a pulsepass-e2e-db).
.EXAMPLE
  .\tests\e2e\scripts\05-Reiniciar-Base.ps1
#>
[CmdletBinding()]
param([string]$Contenedor = 'pulsepass-e2e-db')
. "$PSScriptRoot\_comun.ps1"
$r = Read-Host "Se borrarán el contenedor '$Contenedor' y el volumen 'pulsepass_e2e_data'. Escribe SI para continuar"
if ($r -ne 'SI') { Write-Host 'Cancelado.'; return }
& docker rm -f $Contenedor | Out-Null
& docker volume rm pulsepass_e2e_data | Out-Null
Write-Host 'Listo. Ejecuta 01-Preparar-Base.ps1 y repite el primer arranque de la app.' -ForegroundColor Green
