# Funciones compartidas por los scripts E2E. No se ejecuta solo: lo cargan los demás con ". .\_comun.ps1".
Set-StrictMode -Version 2.0
$ErrorActionPreference = 'Stop'

# Orden en que se llenan los tokens (coincide con la colección ORDENADA).
$Script:OrdenTokens = @(
    [pscustomobject]@{ N = 1; Variable = 'userAToken';    S = 'S022'; Actor = 'A';        Origen = 'Login de A (accessToken)' }
    [pscustomobject]@{ N = 2; Variable = 'userBToken';    S = 'S024'; Actor = 'B';        Origen = 'Login de B (accessToken)' }
    [pscustomobject]@{ N = 3; Variable = 'staffToken';    S = 'S025'; Actor = 'STAFF';    Origen = 'Login de STAFF (accessToken)' }
    [pscustomobject]@{ N = 4; Variable = 'adminToken';    S = 'S026'; Actor = 'ADMIN';    Origen = 'Login de ADMIN (accessToken)' }
    [pscustomobject]@{ N = 5; Variable = 'injectedToken'; S = 'S027'; Actor = 'INJ';      Origen = 'Login del usuario inyectado (accessToken)' }
    [pscustomobject]@{ N = 6; Variable = 'tamperedToken'; S = 'S037'; Actor = '-';        Origen = 'userAToken con 1 carácter cambiado en la firma' }
    [pscustomobject]@{ N = 7; Variable = 'forgedToken';   S = 'S038'; Actor = '-';        Origen = 'JWT firmado con clave inventada (no la real)' }
    [pscustomobject]@{ N = 8; Variable = 'inactiveToken'; S = 'S040'; Actor = 'INACTIVE'; Origen = 'Login del inactivable ANTES de desactivarlo' }
    [pscustomobject]@{ N = 9; Variable = 'expiredToken';  S = 'S118'; Actor = '-';        Origen = 'Copia del primer userAToken (vence a los 15 min)' }
)

function Read-Secret {
    param([string]$Etiqueta, [int]$Min = 0)
    while ($true) {
        $s = Read-Host -Prompt $Etiqueta -AsSecureString
        $b = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($s)
        try { $p = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($b) }
        finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($b) }
        if ($p.Length -ge $Min) { return $p }
        Write-Host "  Debe tener al menos $Min caracteres." -ForegroundColor Yellow
    }
}

function ConvertTo-Base64Url([byte[]]$Bytes) {
    [Convert]::ToBase64String($Bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}
function ConvertFrom-Base64Url([string]$Text) {
    $t = $Text.Replace('-', '+').Replace('_', '/')
    switch ($t.Length % 4) { 2 { $t += '==' } 3 { $t += '=' } }
    , [Convert]::FromBase64String($t)
}
function Get-JwtPayload([string]$Token) {
    try {
        $json = [Text.Encoding]::UTF8.GetString((ConvertFrom-Base64Url $Token.Split('.')[1]))
        return ($json | ConvertFrom-Json)
    } catch { return $null }
}
function Get-JwtMinutesLeft([string]$Token) {
    $p = Get-JwtPayload $Token
    if ($null -eq $p -or -not ($p.PSObject.Properties.Name -contains 'exp')) { return $null }
    $exp = [DateTimeOffset]::FromUnixTimeSeconds([int64]$p.exp)
    return [math]::Round(($exp - [DateTimeOffset]::UtcNow).TotalMinutes, 1)
}

# JWT de PRUEBA firmado con una clave inventada. Nunca usar el secreto real de la app.
function New-ForgedJwt {
    $enc = [Text.Encoding]::UTF8
    $h = ConvertTo-Base64Url ($enc.GetBytes('{"alg":"HS256","typ":"JWT"}'))
    $p = ConvertTo-Base64Url ($enc.GetBytes('{"sub":"1","iss":"pulsepass-api","iat":1760000000,"exp":4102444800,"jti":"e2e-forged"}'))
    $hmac = New-Object Security.Cryptography.HMACSHA256 (, $enc.GetBytes('clave-solo-de-prueba-no-es-la-real-0123456789'))
    $sig = ConvertTo-Base64Url ($hmac.ComputeHash($enc.GetBytes("$h.$p")))
    return "$h.$p.$sig"
}

# Cambia UN carácter en el MEDIO de la firma (el último carácter puede no alterar la firma).
function New-TamperedJwt([string]$Token) {
    $parts = $Token.Split('.')
    $sig = $parts[2].ToCharArray()
    $i = [int][math]::Floor($sig.Length / 2)
    $sig[$i] = if ($sig[$i] -eq 'A') { 'B' } else { 'A' }
    return ($parts[0] + '.' + $parts[1] + '.' + (-join $sig))
}

function Write-Utf8NoBom([string]$Path, [string]$Text) {
    [IO.File]::WriteAllText($Path, $Text, (New-Object Text.UTF8Encoding($false)))
}

function Read-EnvFile([string]$Path) {
    Get-Content -LiteralPath $Path -Raw -Encoding UTF8 | ConvertFrom-Json
}
function Get-EnvVar($Env, [string]$Key) {
    $v = @($Env.values | Where-Object { $_.key -eq $Key })
    if ($v.Count -eq 0) { return $null }
    return [string]$v[0].value
}
function Set-EnvVar($Env, [string]$Key, [string]$Value) {
    $v = @($Env.values | Where-Object { $_.key -eq $Key })
    if ($v.Count -eq 0) {
        $Env.values += [pscustomobject]@{ key = $Key; value = $Value; type = 'default'; enabled = $true }
    } else { $v[0].value = $Value }
}
function Resolve-EnvText($Env, [string]$Text) {
    for ($i = 0; $i -lt 5; $i++) {
        $Text = [regex]::Replace($Text, '\{\{(\w+)\}\}', {
            param($m)
            $val = Get-EnvVar $Env $m.Groups[1].Value
            if ($null -eq $val -or $val -eq '') { $m.Value } else { $val }
        })
    }
    return $Text
}
