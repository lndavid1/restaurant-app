# build_apk.ps1 - Build Debug APK va copy ra thu muc goc
# Cach dung: .\build_apk.ps1

param (
    [switch]$BumpVersion
)

$ErrorActionPreference = "Stop"
$ProjectRoot = $PSScriptRoot

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "   Restaurant App - Build APK" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# Doc version tu build.gradle.kts
$buildGradlePath = "$ProjectRoot\app\build.gradle.kts"
$buildGradle = Get-Content $buildGradlePath -Raw

$version = "unknown"
$versionCode = 1

if ($buildGradle -match 'versionCode\s*=\s*(\d+)') {
    $versionCode = [int]$Matches[1]
}
if ($buildGradle -match 'versionName\s*=\s*"([^"]+)"') {
    $version = $Matches[1]
}

if ($BumpVersion) {
    $versionCode++
    # Tang minor version (VD: 1.4 -> 1.5)
    if ($version -match '^(\d+)\.(\d+)$') {
        $major = [int]$Matches[1]
        $minor = [int]$Matches[2] + 1
        $version = "$major.$minor"
    } else {
        $version = "$version-updated"
    }
    
    # Cap nhat file
    $buildGradle = $buildGradle -replace 'versionCode\s*=\s*\d+', "versionCode = $versionCode"
    $buildGradle = $buildGradle -replace 'versionName\s*=\s*"[^"]+"', "versionName = `"$version`""
    Set-Content -Path $buildGradlePath -Value $buildGradle
    Write-Host "Version bumped to: v$version (Code: $versionCode)" -ForegroundColor Green
} else {
    Write-Host "Version: v$version (Code: $versionCode)" -ForegroundColor Yellow
}
Write-Host ""

# Build Debug APK
Write-Host "Building..." -ForegroundColor White
cmd /c "`"$ProjectRoot\gradlew.bat`" assembleDebug"
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "BUILD FAILED! Check errors above." -ForegroundColor Red
    exit 1
}

# Tim APK vua build
$apkName = "restaurant-v$version-debug.apk"
$apkSrc  = "$ProjectRoot\app\build\outputs\apk\debug\$apkName"
$apkDst  = "$ProjectRoot\$apkName"

if (-Not (Test-Path $apkSrc)) {
    $found = Get-ChildItem "$ProjectRoot\app\build\outputs\apk\debug\*.apk" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($found) {
        $apkSrc = $found.FullName
        $apkName = $found.Name
        $apkDst  = "$ProjectRoot\$apkName"
    } else {
        Write-Host "ERROR: APK not found in output directory!" -ForegroundColor Red
        exit 1
    }
}

# Copy ra thu muc goc
Copy-Item -Path $apkSrc -Destination $apkDst -Force

$sizeMB = [math]::Round((Get-Item $apkDst).Length / 1MB, 1)

Write-Host ""
Write-Host "==========================================" -ForegroundColor Green
Write-Host "BUILD SUCCESSFUL" -ForegroundColor Green
Write-Host "  File : $apkName" -ForegroundColor White
Write-Host "  Size : $sizeMB MB" -ForegroundColor White
Write-Host "  Path : $apkDst" -ForegroundColor White
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Install via ADB:" -ForegroundColor Cyan
Write-Host "  adb install -r $apkDst" -ForegroundColor Gray
Write-Host ""
