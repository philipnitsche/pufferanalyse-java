$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path -LiteralPath $PSScriptRoot).Path
$outputDir = [IO.Path]::GetFullPath((Join-Path $projectRoot "build/test-classes"))
if (-not $outputDir.StartsWith($projectRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw "Build-Ziel liegt ausserhalb des Projekts."
}
if (Test-Path -LiteralPath $outputDir) {
    $resolved = (Resolve-Path -LiteralPath $outputDir).Path
    if ($resolved -ne $outputDir) { throw "Unerwartetes Build-Ziel." }
    Get-ChildItem -LiteralPath $outputDir -Force | ForEach-Object { Remove-Item -LiteralPath $_.FullName -Recurse -Force }
} else {
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
}
$sourceFiles = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot "src") -Recurse -Filter *.java | Select-Object -ExpandProperty FullName)
$testFiles = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot "tests/java") -Recurse -Filter *.java | Select-Object -ExpandProperty FullName)
Push-Location $projectRoot
try {
    javac -encoding UTF-8 -d $outputDir ($sourceFiles + $testFiles)
    if ($LASTEXITCODE -ne 0) { throw "Kompilierung fehlgeschlagen." }
    java -cp $outputDir tests.TestRunner
    if ($LASTEXITCODE -ne 0) { throw "Mindestens ein Test ist fehlgeschlagen." }
} finally { Pop-Location }
