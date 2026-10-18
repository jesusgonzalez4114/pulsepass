<#
.SYNOPSIS
  Levanta PostgreSQL en Docker (base pulsepass_e2e), genera el secreto JWT de E2E y siembra los fixtures.
  No necesitas instalar PostgreSQL ni psql: todo corre dentro del contenedor.
.DESCRIPTION
  Se ejecuta DOS veces (entre ambas la app arranca una vez para que Flyway cree las tablas):
    1ª vez: crea/arranca el contenedor, genera el secreto y te muestra la línea de variables para IntelliJ.
    2ª vez: detecta las tablas, te pide las claves de STAFF/ADMIN/INACTIVE y siembra.
  Las claves no se guardan en ningún archivo. El secreto JWT se guarda en tests\e2e\.jwt-secret.local (ignorado por Git).
.EXAMPLE
  .\tests\e2e\scripts\01-Preparar-Base.ps1
#>
[CmdletBinding()]
param(
    [string]$Contenedor = 'pulsepass-e2e-db',
    [int]$Puerto = 5433
)
. "$PSScriptRoot\_comun.ps1"
$Db = 'pulsepass_e2e'; $PgUser = 'pulsepass'; $PgPass = 'pulsepass_e2e_local'   # contenedor local desechable
$Seed = (Resolve-Path (Join-Path $PSScriptRoot '..\fixtures\seed-e2e.sql')).Path
$SecretFile = Join-Path $PSScriptRoot '..\.jwt-secret.local'

function Invoke-Psql([string[]]$Args2) { & docker exec $Contenedor psql -U $PgUser -d $Db @Args2 }

Write-Host "`n[1/5] Docker" -ForegroundColor Cyan
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'No encuentro docker. Instala Docker Desktop (https://www.docker.com/products/docker-desktop/) y reinicia PowerShell.' }
$null = & docker version --format '{{.Server.Version}}'
if ($LASTEXITCODE -ne 0) { throw 'Docker está instalado pero el motor no responde. Abre Docker Desktop y espera a que diga "Engine running".' }
Write-Host '  Docker responde.' -ForegroundColor Green

Write-Host "`n[2/5] Contenedor $Contenedor (PostgreSQL 16, puerto $Puerto)" -ForegroundColor Cyan
$existe = (& docker ps -a --filter "name=^/$Contenedor$" --format '{{.Names}}')
if (-not $existe) {
    & docker run -d --name $Contenedor `
        -e "POSTGRES_USER=$PgUser" -e "POSTGRES_PASSWORD=$PgPass" -e "POSTGRES_DB=$Db" `
        -p "127.0.0.1:${Puerto}:5432" -v pulsepass_e2e_data:/var/lib/postgresql/data postgres:16 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "No pude crear el contenedor (¿el puerto $Puerto está ocupado? usa -Puerto 5434)." }
    Write-Host '  Creado.' -ForegroundColor Green
} else {
    $corriendo = (& docker ps --filter "name=^/$Contenedor$" --format '{{.Names}}')
    if (-not $corriendo) { & docker start $Contenedor | Out-Null; Write-Host '  Estaba detenido: arrancado.' -ForegroundColor Green }
    else { Write-Host '  Ya estaba corriendo.' }
}
$listo = $false
for ($i = 0; $i -lt 40; $i++) {
    & docker exec $Contenedor pg_isready -U $PgUser -d $Db *> $null
    if ($LASTEXITCODE -eq 0) { $listo = $true; break }
    Start-Sleep -Seconds 2
}
if (-not $listo) { throw "PostgreSQL no quedó listo. Revisa: docker logs $Contenedor" }
Write-Host '  PostgreSQL listo.' -ForegroundColor Green

Write-Host "`n[3/5] Variables de entorno para IntelliJ" -ForegroundColor Cyan
if (Test-Path $SecretFile) { $secreto = (Get-Content -LiteralPath $SecretFile -Raw).Trim() }
else {
    $bytes = New-Object byte[] 32
    [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    $secreto = [Convert]::ToBase64String($bytes)
    Write-Utf8NoBom $SecretFile $secreto
}
Write-Host '  Pega esta línea en IntelliJ: Run > Edit Configurations > PulsepassApplication > Environment variables:'
Write-Host "  DB_URL=jdbc:postgresql://localhost:${Puerto}/$Db;DB_USER=$PgUser;DB_PASSWORD=$PgPass;PULSEPASS_JWT_SECRET_BASE64=$secreto" -ForegroundColor Yellow
Write-Host "  (El secreto queda en tests\e2e\.jwt-secret.local para reutilizarlo; es exclusivo de E2E.)"

Write-Host "`n[4/5] ¿Flyway ya creó las tablas?" -ForegroundColor Cyan
$tabla = "$(Invoke-Psql @('-tAc', "SELECT to_regclass('public.venues')"))".Trim()
if (-not $tabla) {
    Write-Host '  Todavía no. Arranca PulsepassApplication con la línea amarilla, espera "Started PulsepassApplication", DETÉNLA y ejecuta este script otra vez.' -ForegroundColor Yellow
    return
}
Write-Host '  Sí.' -ForegroundColor Green

Write-Host "`n[5/5] Fixtures" -ForegroundColor Cyan
$ya = "$(Invoke-Psql @('-tAc', "SELECT count(*) FROM venues WHERE code LIKE 'E2E-%'"))".Trim()
if ($ya -ne '0') { Write-Host '  Ya estaban sembrados. Para repetir desde cero: .\tests\e2e\scripts\05-Reiniciar-Base.ps1' -ForegroundColor Green; return }
Write-Host '  Elige 3 claves de 12 a 72 caracteres con letras, números, punto, guion o guion bajo (sin espacios ni comillas).'
$pat = '^[A-Za-z0-9._-]{12,72}$'
do { $staff = Read-Secret 'Clave de STAFF' 12 } until ($staff -match $pat)
do { $admin = Read-Secret 'Clave de ADMIN' 12 } until ($admin -match $pat)
do { $inact = Read-Secret 'Clave de INACTIVE' 12 } until ($inact -match $pat)
& docker cp $Seed "${Contenedor}:/tmp/seed-e2e.sql"
if ($LASTEXITCODE -ne 0) { throw 'No pude copiar el seed al contenedor.' }
Invoke-Psql @('-v', 'ON_ERROR_STOP=1', '-v', "staff_pw=$staff", '-v', "admin_pw=$admin", '-v', "inactive_pw=$inact", '-f', '/tmp/seed-e2e.sql')
$ok = ($LASTEXITCODE -eq 0)
& docker exec $Contenedor rm -f /tmp/seed-e2e.sql
$staff = $admin = $inact = $null
if ($ok) {
    Write-Host "`nListo. Debe verse: venues 3, events 5, users 3, user_roles 3." -ForegroundColor Green
    Write-Host 'Arranca la app otra vez (mismas variables) y ejecuta .\tests\e2e\scripts\03-Verificar-Ambiente.ps1'
} else { Write-Host 'El seed falló. Para empezar de cero: .\tests\e2e\scripts\05-Reiniciar-Base.ps1' -ForegroundColor Red; exit 1 }
