<#
.SYNOPSIS
  Verifica que el ambiente de Postman tenga TODAS las variables que usan las colecciones y que cada una tenga su Value.
.DESCRIPTION
  1. Cruza las {{variables}} de ambas colecciones contra las del ambiente (faltantes / sin uso).
  2. Revisa qué Value está lleno, cuál se llena durante la prueba y cuánto le queda a cada token.
  3. Comprueba que la plantilla del repositorio no lleve secretos ni tokens.
  4. Prueba la conexión: GET /api/venues/active debe mostrar E2E-VEN-01.
  Devuelve código 1 si falta algo imprescindible.
.EXAMPLE
  .\tests\e2e\scripts\03-Verificar-Ambiente.ps1
#>
[CmdletBinding()]
param(
    [string]$Ambiente = (Join-Path $PSScriptRoot '..\postman\PulsePass-local.postman_environment.local.json'),
    [string]$Plantilla = (Join-Path $PSScriptRoot '..\postman\PulsePass-local.postman_environment.json'),
    [switch]$SinRed
)
. "$PSScriptRoot\_comun.ps1"
$fallos = 0
function Fail($t) { Write-Host "  [FALLA] $t" -ForegroundColor Red; $script:fallos++ }
function Ok($t) { Write-Host "  [ OK  ] $t" -ForegroundColor Green }
function Warn($t) { Write-Host "  [AVISO] $t" -ForegroundColor Yellow }

# 1. Plantilla limpia
Write-Host "`n1. Plantilla del repositorio (no debe llevar secretos)" -ForegroundColor Cyan
$p = Read-EnvFile $Plantilla
$sucias = @($p.values | Where-Object { $_.type -eq 'secret' -and $_.value })
if ($sucias.Count -eq 0) { Ok "Las $(@($p.values | Where-Object type -eq 'secret').Count) variables secretas están vacías." } else { Fail ("Con valor en la plantilla: " + (($sucias | ForEach-Object key) -join ', ')) }

# 2. Archivo local
Write-Host "`n2. Ambiente local" -ForegroundColor Cyan
if (-not (Test-Path $Ambiente)) { Fail "No existe $Ambiente. Ejecuta 02-Obtener-Tokens.ps1 (o crea el ambiente a mano con postman\ambiente-variables.md y pasa -Ambiente <archivo exportado>)."; exit 1 }
$e = Read-EnvFile $Ambiente
$claves = @($e.values | ForEach-Object key)

# 3. Variables usadas por las colecciones
Write-Host "`n3. Variables usadas por las colecciones" -ForegroundColor Cyan
$usadas = @{}
Get-ChildItem (Join-Path $PSScriptRoot '..\postman') -Filter '*postman_collection.json' | ForEach-Object {
    $txt = Get-Content -LiteralPath $_.FullName -Raw -Encoding UTF8
    [regex]::Matches($txt, '\{\{(\w+)\}\}') | ForEach-Object { $usadas[$_.Groups[1].Value] = $true }
}
$faltan = @($usadas.Keys | Where-Object { $_ -notin $claves } | Sort-Object)
if ($faltan.Count -eq 0) { Ok "$($usadas.Count) variables usadas; todas existen en el ambiente." } else { Fail ("Faltan en el ambiente: " + ($faltan -join ', ')) }
$sinUso = @($claves | Where-Object { -not $usadas.ContainsKey($_) })
if ($sinUso.Count -gt 0) { Warn ("En el ambiente pero sin uso directo en las colecciones: " + ($sinUso -join ', ')) }

# 4. Estado de cada Value
Write-Host "`n4. Value de cada variable" -ForegroundColor Cyan
$antes = 'runId', 'userAPassword', 'userBPassword', 'staffPassword', 'adminPassword', 'inactivePassword'
$duranteManual = 'artistId', 'ticketCancelableCode', 'ticketUsableCode'
$tokens = $Script:OrdenTokens.Variable
$filas = foreach ($v in $e.values) {
    $val = [string]$v.value
    $grupo = if ($v.key -in $antes) { 'Antes de empezar' } elseif ($v.key -in $tokens) { 'Token (script 02)' } elseif ($v.key -in $duranteManual) { 'Copiar durante la prueba' } else { 'Fija / ya viene lista' }
    $estado = if ($val) { 'lleno' } else { 'vacío' }
    $nota = ''
    if ($v.key -in $tokens -and $val) {
        $m = Get-JwtMinutesLeft $val
        if ($v.key -in 'tamperedToken', 'forgedToken') { $nota = 'inválido a propósito' }
        elseif ($v.key -eq 'expiredToken') { $nota = if ($null -ne $m -and $m -le 0) { 'vencido: listo para S118' } else { "aún vigente $m min: espera a que venza antes de S118" } }
        elseif ($null -ne $m -and $m -le 0) { $nota = 'VENCIDO'; $estado = 'vencido' }
        elseif ($null -ne $m) { $nota = "vence en $m min" }
    }
    [pscustomobject]@{ Grupo = $grupo; Variable = $v.key; Tipo = $v.type; Estado = $estado; Nota = $nota }
}
$filas | Sort-Object Grupo, Variable | Format-Table -AutoSize | Out-String -Width 200 | Write-Host
$critic = @($filas | Where-Object { $_.Grupo -in 'Antes de empezar' -and $_.Estado -eq 'vacío' })
if ($critic.Count -gt 0) { Fail ("Sin Value (obligatorios antes de empezar): " + (($critic | ForEach-Object Variable) -join ', ')) } else { Ok 'Runid y las 5 claves tienen Value.' }
$vencidos = @($filas | Where-Object { $_.Estado -eq 'vencido' })
if ($vencidos.Count -gt 0) { Warn ("Tokens vencidos: " + (($vencidos | ForEach-Object Variable) -join ', ') + '. Repite 02-Obtener-Tokens.ps1.') }

# 5. Conexión
if (-not $SinRed) {
    Write-Host "`n5. Conexión con la app" -ForegroundColor Cyan
    $base = (Get-EnvVar $e 'baseUrl').TrimEnd('/')
    try {
        $r = Invoke-RestMethod -Uri "$base/api/venues/active" -TimeoutSec 8
        $codes = @($r | ForEach-Object { $_.code })
        if ($codes -contains (Get-EnvVar $e 'venueActiveCode')) { Ok "GET /api/venues/active responde y contiene $(Get-EnvVar $e 'venueActiveCode') (seed aplicado, base correcta)." }
        else { Fail "La app responde, pero no aparece $(Get-EnvVar $e 'venueActiveCode'). ¿Arrancó contra pulsepass_e2e y sembraste?" }
    } catch { Fail "No responde $base/api/venues/active: $($_.Exception.Message)" }
}

Write-Host ''
if ($fallos -eq 0) { Write-Host 'AMBIENTE LISTO.' -ForegroundColor Green } else { Write-Host "$fallos problema(s) por corregir." -ForegroundColor Red; exit 1 }
