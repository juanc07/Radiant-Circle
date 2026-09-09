$ErrorActionPreference = "Stop"

$ApkSource = "app/build/outputs/apk/debug/app-debug.apk"
$ApkTargetDir = "release/submission"
$ApkTarget = Join-Path $ApkTargetDir "RadiantRush-debug.apk"

if (-Not (Test-Path $ApkSource)) {
    Write-Host "Debug APK not found at $ApkSource"
    Write-Host "Run: .\gradlew.bat :app:assembleDebug"
    exit 1
}

New-Item -ItemType Directory -Force -Path $ApkTargetDir | Out-Null
Copy-Item $ApkSource $ApkTarget -Force

Write-Host "Copied APK to $ApkTarget"
Get-Item $ApkTarget | Format-List Name,Length,LastWriteTime
Write-Host "Do not commit APK binaries unless the submission rules require it."
