$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path -LiteralPath $PSScriptRoot).Path
$outputDir = Join-Path $projectRoot "build/classes"
if (-not (Test-Path -LiteralPath (Join-Path $outputDir "Main.class"))) {
    throw "Kein Build gefunden. Fuehre zuerst build.ps1 aus."
}
Push-Location $projectRoot
try {
    java -cp $outputDir Main
    if ($LASTEXITCODE -ne 0) { throw "Programmlauf fehlgeschlagen (Status $LASTEXITCODE)." }
} finally { Pop-Location }
