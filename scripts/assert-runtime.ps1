param(
    [Parameter(Mandatory)][string]$Log,
    [Parameter(Mandatory)][ValidateSet('Server', 'Client', 'MultiplayerServer', 'MultiplayerClient', 'Datapack', 'DatapackClient')][string]$Mode
)
$ErrorActionPreference = 'Stop'
$content = Get-Content -LiteralPath $Log -Raw
if ($content -match 'AssertionError|Critical injection failure|MixinTransformerError|Game crashed!|Exception in server tick loop|BUILD FAILED') {
    throw "Runtime failure in $Log"
}
$markers = switch ($Mode) {
    Datapack { @('BUNDLED DEFAULTS PASSED', 'DATAPACK LOADING PASSED', 'DATAPACK GAMEPLAY PASSED', 'DATAPACK RELOAD PASSED', 'DATAPACK CONVERSION PASSED') }
    DatapackClient { @('BUNDLED DEFAULTS PASSED', 'DATAPACK RELOAD PASSED', 'CLIENT DATAPACK RELOAD PASSED') }
    Server { @('LIFECYCLE SMOKE PASSED', 'SERVER MOVEMENT PASSED') }
    Client { @('CLIENT DATAPACK GAMEPLAY PASSED', 'CLIENT LIFECYCLE SMOKE PASSED', 'CLIENT WEIGHT CALCULATION PASSED', 'CLIENT MOVEMENT PASSED', 'ADMIN TOOLS PASSED', 'PLAYER FEEDBACK PASSED', 'GUI COMPATIBILITY PASSED', 'BASELINE COMPATIBILITY PASSED') }
    MultiplayerServer { @('MULTIPLAYER SERVER AUTHORITY PASSED', 'MULTIPLAYER RECONNECT SERVER PASSED', 'BASELINE COMPATIBILITY PASSED') }
    MultiplayerClient { @('MULTIPLAYER CLIENT AUTHORITY PASSED', 'MULTIPLAYER MOVEMENT PASSED', 'MULTIPLAYER RECONNECT CLIENT PASSED') }
}
foreach ($marker in $markers) {
    if (!$content.Contains($marker)) { throw "Missing '$marker' in $Log" }
}
if (!$content.Contains('BUILD SUCCESSFUL')) { throw "Gradle did not complete successfully in $Log" }
Write-Output "$Mode acceptance passed: $Log"
