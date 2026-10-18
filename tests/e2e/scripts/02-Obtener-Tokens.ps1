<#
.SYNOPSIS
  Hace los logins, genera los tokens de prueba y deja el ambiente de Postman listo para IMPORTAR (con todos los Value).
.DESCRIPTION
  - Pide por consola las claves (no se muestran, no van al repositorio) y el runId.
  - Inicia sesión de los actores indicados y guarda cada accessToken en su variable.
  - Genera tamperedToken (userAToken alterado) y forgedToken (clave inventada); guarda expiredToken
    la primera vez (el token de A de ese momento; vencerá a los 15 min).
  - Escribe postman\PulsePass-local.postman_environment.local.json (ignorado por Git). Impórtalo en Postman
    (Environments > Import) y reemplaza el ambiente anterior. NO lo subas ni lo sincronices.
  - -SoloAmbiente: NO hace logins; solo arma el ambiente con runId, claves y forgedToken (úsalo ANTES de S001 para importarlo).
  - -Valores @{ artistId = '3' }: fija cualquier otra variable al escribir el archivo.
  - -Pegar: copia al portapapeles, en orden, cada token nuevo para pegarlo en Postman (Current value).
  - Se puede repetir cada vez que un token venza (15 min): conserva claves, runId y expiredToken.
  - Registro de A, B y del usuario inyectado: se hace en Postman (S016, S017, S021). Antes de eso no hay login posible.
.EXAMPLE
  .\tests\e2e\scripts\02-Obtener-Tokens.ps1 -SoloAmbiente -RunId ra     # antes de S001: crea el ambiente
  .\tests\e2e\scripts\02-Obtener-Tokens.ps1 -Pegar                      # tras S021: A, B, STAFF, ADMIN, INJ
  .\tests\e2e\scripts\02-Obtener-Tokens.ps1 -Actores INACTIVE               # antes de desactivarlo (S040)
  .\tests\e2e\scripts\02-Obtener-Tokens.ps1 -Actores A,STAFF -Pegar         # renovar y pegar uno a uno
#>
[CmdletBinding()]
param(
    [string]$BaseUrl,
    [string]$RunId,
    [ValidateSet('A', 'B', 'STAFF', 'ADMIN', 'INACTIVE', 'INJ')]
    [string[]]$Actores = @('A', 'B', 'STAFF', 'ADMIN', 'INJ'),
    [string]$Ambiente = (Join-Path $PSScriptRoot '..\postman\PulsePass-local.postman_environment.local.json'),
    [string]$Plantilla = (Join-Path $PSScriptRoot '..\postman\PulsePass-local.postman_environment.json'),
    [switch]$ReiniciarVencido,
    [switch]$SoloAmbiente,
    [hashtable]$Valores,
    [switch]$Pegar
)
. "$PSScriptRoot\_comun.ps1"

$e = if (Test-Path $Ambiente) { Read-EnvFile $Ambiente } else { Read-EnvFile $Plantilla }
if ($BaseUrl) { Set-EnvVar $e 'baseUrl' $BaseUrl }
if ($RunId) { Set-EnvVar $e 'runId' $RunId }
if (-not (Get-EnvVar $e 'runId')) { Set-EnvVar $e 'runId' 'r01' }
$run = Get-EnvVar $e 'runId'
$base = (Get-EnvVar $e 'baseUrl').TrimEnd('/')
Write-Host "Ambiente: runId=$run  baseUrl=$base" -ForegroundColor Cyan

# actor -> identificador, variable de clave, variable de token
$mapa = @{
    A        = @{ id = { Resolve-EnvText $e '{{userAUsername}}' }; pw = 'userAPassword';        tok = 'userAToken';    nombre = 'USER_A' }
    B        = @{ id = { Resolve-EnvText $e '{{userBUsername}}' }; pw = 'userBPassword';        tok = 'userBToken';    nombre = 'USER_B' }
    STAFF    = @{ id = { Get-EnvVar $e 'staffIdentifier' };        pw = 'staffPassword';        tok = 'staffToken';    nombre = 'STAFF' }
    ADMIN    = @{ id = { Get-EnvVar $e 'adminIdentifier' };        pw = 'adminPassword';        tok = 'adminToken';    nombre = 'ADMIN' }
    INACTIVE = @{ id = { Get-EnvVar $e 'inactiveIdentifier' };     pw = 'inactivePassword';     tok = 'inactiveToken'; nombre = 'INACTIVE' }
    INJ      = @{ id = { "inj_$run" };                             pw = 'userAPassword';        tok = 'injectedToken'; nombre = 'INYECTADO' }
}

$nuevos = New-Object System.Collections.Generic.List[string]
if ($SoloAmbiente) { $Actores = @() }
if ($SoloAmbiente) {
    foreach ($k in 'userAPassword', 'userBPassword', 'staffPassword', 'adminPassword', 'inactivePassword') {
        if (-not (Get-EnvVar $e $k)) { Set-EnvVar $e $k (Read-Secret "Clave para $k (12 a 72 caracteres)" 12) }
    }
}
foreach ($a in $Actores) {
    $m = $mapa[$a]
    $clave = Get-EnvVar $e $m.pw
    if (-not $clave) {
        $clave = Read-Secret "Clave de $($m.nombre) ($($m.pw))" 12
        Set-EnvVar $e $m.pw $clave
    }
    $ident = & $m.id
    try {
        $body = @{ identifier = $ident; password = $clave } | ConvertTo-Json -Compress
        $r = Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" -ContentType 'application/json' -Body $body
        Set-EnvVar $e $m.tok $r.accessToken
        $nuevos.Add($m.tok)
        Write-Host ("  OK   {0,-9} -> {1,-14} (identifier={2}, expiresIn={3}s)" -f $a, $m.tok, $ident, $r.expiresIn) -ForegroundColor Green
    } catch {
        $code = ''
        if ($_.Exception.Response) { $code = [int]$_.Exception.Response.StatusCode }
        Write-Host ("  FALLA {0,-9} login devolvió {1} {2}" -f $a, $code, $_.Exception.Message) -ForegroundColor Red
        if ($a -in 'A', 'B', 'INJ') { Write-Host "        ¿Ya hiciste el registro en Postman (S016/S017/S021) con este runId?" -ForegroundColor Yellow }
        if ($a -in 'STAFF', 'ADMIN', 'INACTIVE') { Write-Host "        ¿Sembraste con 01-Preparar-Base.ps1 y usaste las mismas claves?" -ForegroundColor Yellow }
    }
}

# Tokens derivados
$tokA = Get-EnvVar $e 'userAToken'
if ($tokA) {
    Set-EnvVar $e 'tamperedToken' (New-TamperedJwt $tokA); if ($nuevos -contains 'userAToken') { $nuevos.Add('tamperedToken') }
    if ($ReiniciarVencido -or -not (Get-EnvVar $e 'expiredToken')) { Set-EnvVar $e 'expiredToken' $tokA; if ($nuevos -contains 'userAToken') { $nuevos.Add('expiredToken') }; Write-Host '  expiredToken guardado: úsalo en S118 (cuando hayan pasado 15 min).' }
}
if (-not (Get-EnvVar $e 'forgedToken')) { Set-EnvVar $e 'forgedToken' (New-ForgedJwt); $nuevos.Add('forgedToken') }
if ($Valores) { foreach ($k in $Valores.Keys) { Set-EnvVar $e $k ([string]$Valores[$k]) } }

Write-Utf8NoBom $Ambiente ($e | ConvertTo-Json -Depth 10)
Write-Host "`nGuardado: $Ambiente" -ForegroundColor Cyan

Write-Host "`nTOKENS (en este orden se usan; los valores completos están en el archivo):" -ForegroundColor Cyan
$tabla = foreach ($t in $Script:OrdenTokens) {
    $v = Get-EnvVar $e $t.Variable
    $min = if ($v) { Get-JwtMinutesLeft $v } else { $null }
    $estado = if (-not $v) { 'vacío' }
              elseif ($t.Variable -in 'tamperedToken', 'forgedToken') { 'inválido a propósito' }
              elseif ($t.Variable -eq 'expiredToken') { if ($null -ne $min -and $min -le 0) { 'vencido (listo)' } else { "vence en $min min" } }
              elseif ($null -ne $min -and $min -le 0) { 'VENCIDO: repite el login' }
              else { "vence en $min min" }
    [pscustomobject]@{ Orden = $t.N; Paso = $t.S; Variable = $t.Variable; Valor = $(if ($v) { $v.Substring(0, [math]::Min(14, $v.Length)) + '…' } else { '' }); Estado = $estado }
}
$tabla | Format-Table -AutoSize | Out-String -Width 200 | Write-Host

if ($Pegar) {
    Write-Host 'Modo pegar: cada token se copia al portapapeles. En Postman: Environments > ambiente > fila de la variable > Current value > pegar > Save.' -ForegroundColor Cyan
    $lista = @($Script:OrdenTokens | Where-Object { $nuevos -contains $_.Variable })
    $i = 0
    foreach ($t in $lista) {
        $i++
        $v = Get-EnvVar $e $t.Variable
        try { Set-Clipboard -Value $v } catch { Write-Host "  (sin portapapeles; valor completo abajo)`n  $v" }
        Read-Host "  [$i/$($lista.Count)] Paso $($t.S): $($t.Variable) copiado. Pégalo en su Current value y pulsa Enter"
    }
    try { Set-Clipboard -Value ' ' } catch { }
}
Write-Host 'Siguiente: Postman > Environments > Import > ese archivo; selecciona "PulsePass — local E2E". Luego .\03-Verificar-Ambiente.ps1' -ForegroundColor Green
