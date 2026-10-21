<#
.SYNOPSIS
    Automated Quick Deploy & Version Release Script for Nabd Al-Dhikr.
.DESCRIPTION
    Updates app version in build.gradle.kts and version.json, commits changes,
    creates Git release tag (vX.X.X), and pushes to GitHub to trigger CI/CD pipeline.
.EXAMPLE
    .\deploy.ps1 -Version "1.0.1" -Message "تحسينات عامة واستقرار البوصلة"
    .\deploy.ps1 -Message "تحديث دوري"
#>
param(
    [string]$Version = "",
    [string]$Message = "تحديث جديد وإصلاحات عامة",
    [switch]$ForceUpdate
)

$ErrorActionPreference = "Stop"

$repoPath = "F:\omar\New folder"
$gitBin = "F:\mingit\cmd\git.exe"
if (-not (Test-Path $gitBin)) {
    $gitBin = "git"
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " 🚀 نبض الذكر (Nabd Al-Dhikr) - Quick Deploy Script" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Read current version from app/build.gradle.kts
$gradleFile = Join-Path $repoPath "app\build.gradle.kts"
$gradleContent = Get-Content $gradleFile -Raw

$versionCodeMatch = [regex]::Match($gradleContent, 'versionCode\s*=\s*(\d+)')
$versionNameMatch = [regex]::Match($gradleContent, 'versionName\s*=\s*"([^"]+)"')

$currentCode = [int]$versionCodeMatch.Groups[1].Value
$currentName = $versionNameMatch.Groups[1].Value

Write-Host "Current App Version: v$currentName (Code: $currentCode)" -ForegroundColor Yellow

# 2. Determine new version
if ([string]::IsNullOrWhiteSpace($Version)) {
    # Auto increment patch version: 1.0.0 -> 1.0.1
    $parts = $currentName.Split('.')
    $major = [int]$parts[0]
    $minor = if ($parts.Length -gt 1) { [int]$parts[1] } else { 0 }
    $patch = if ($parts.Length -gt 2) { [int]$parts[2] + 1 } else { 1 }
    $newVersion = "$major.$minor.$patch"
} else {
    $newVersion = $Version.Trim().TrimStart('v')
}

$newCode = $currentCode + 1
$tagName = "v$newVersion"

Write-Host "Target App Version : $tagName (Code: $newCode)" -ForegroundColor Green

# 3. Update app/build.gradle.kts
$updatedGradle = $gradleContent -replace 'versionCode\s*=\s*\d+', "versionCode = $newCode"
$updatedGradle = $updatedGradle -replace 'versionName\s*=\s*"[^"]+"', "versionName = `"$newVersion`""
Set-Content -Path $gradleFile -Value $updatedGradle -Encoding utf8

# 4. Update version.json
$versionJsonFile = Join-Path $repoPath "version.json"
$isForce = if ($ForceUpdate) { "true" } else { "false" }
$jsonContent = @"
{
  "versionCode": $newCode,
  "versionName": "$newVersion",
  "apkUrl": "https://github.com/omar15912007-hash/Nabd-Al-Dhikr/releases/download/$tagName/NabdAlDhikr.apk",
  "changelog": "$Message",
  "forceUpdate": $isForce
}
"@
Set-Content -Path $versionJsonFile -Value $jsonContent -Encoding utf8

# 5. Git Commit, Tag & Push
Write-Host "`n📦 Staging and committing changes..." -ForegroundColor Yellow
& $gitBin -C $repoPath add .
& $gitBin -C $repoPath commit -m "chore(release): $tagName - $Message"
& $gitBin -C $repoPath tag -a $tagName -m "Release $tagName - $Message"

Write-Host "`n🚀 Pushing to GitHub (origin main --tags)..." -ForegroundColor Cyan
& $gitBin -C $repoPath push origin main --tags

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host " ✅ DEPLOY SUCCESSFUL! Tag: $tagName" -ForegroundColor Green
Write-Host " GitHub Actions is now building and releasing your APK!" -ForegroundColor Yellow
Write-Host " URL: https://github.com/omar15912007-hash/Nabd-Al-Dhikr/actions" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Green
