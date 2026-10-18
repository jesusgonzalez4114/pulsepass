<#
.SYNOPSIS
  Acciones auxiliares de la prueba sobre la base pulsepass_e2e del contenedor Docker (no abres ningún cliente SQL).
.EXAMPLE
  .\tests\e2e\scripts\04-Sql-Auxiliar.ps1 -Accion DesactivarInactivo   # entre S040 y S041
  .\tests\e2e\scripts\04-Sql-Auxiliar.ps1 -Accion RevocarStaff         # entre S116 y S117
  .\tests\e2e\scripts\04-Sql-Auxiliar.ps1 -Accion RestaurarStaff       # al terminar S117
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidateSet('DesactivarInactivo', 'ActivarInactivo', 'RevocarStaff', 'RestaurarStaff')]
    [string]$Accion,
    [string]$Contenedor = 'pulsepass-e2e-db'
)
. "$PSScriptRoot\_comun.ps1"
$sql = switch ($Accion) {
    'DesactivarInactivo' { "UPDATE users SET active = false WHERE email = 'e2e.inactive@example.test';" }
    'ActivarInactivo'    { "UPDATE users SET active = true WHERE email = 'e2e.inactive@example.test';" }
    'RevocarStaff'       { "DELETE FROM user_roles WHERE user_id = (SELECT id FROM users WHERE email = 'e2e.staff@example.test'); INSERT INTO user_roles (user_id, role) SELECT id, 'USER' FROM users WHERE email = 'e2e.staff@example.test';" }
    'RestaurarStaff'     { "DELETE FROM user_roles WHERE user_id = (SELECT id FROM users WHERE email = 'e2e.staff@example.test'); INSERT INTO user_roles (user_id, role) SELECT id, 'STAFF' FROM users WHERE email = 'e2e.staff@example.test';" }
}
& docker exec $Contenedor psql -U pulsepass -d pulsepass_e2e -v ON_ERROR_STOP=1 -c $sql
if ($LASTEXITCODE -ne 0) { throw "No pude ejecutar la acción. ¿Está corriendo el contenedor $Contenedor? (docker ps)" }
Write-Host "Hecho: $Accion." -ForegroundColor Green
