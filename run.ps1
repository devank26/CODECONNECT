# ── Configuration ─────────────────────────────────────────────────────────────
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot"
$env:PATH = "${env:JAVA_HOME}\bin;${env:PATH}"
$MvnVersion = "3.9.6"
$MvnDir = Join-Path $PSScriptRoot ".mvn-local"
$MvnZip = Join-Path $MvnDir "apache-maven-$MvnVersion-bin.zip"
$MvnHome = Join-Path $MvnDir "apache-maven-$MvnVersion"
$MvnCmd = Join-Path $MvnHome "bin/mvn.cmd"

Write-Host "==============================================="
Write-Host " Java Platform — Build and Run"
Write-Host "==============================================="
Write-Host " JAVA_HOME: $env:JAVA_HOME"
Write-Host " Project: $PSScriptRoot"
Write-Host ""

# ── Download Maven if not already present ────────────────────────────────────
if (!(Test-Path $MvnCmd)) {
    Write-Host "[1/3] Downloading Apache Maven $MvnVersion..."
    if (!(Test-Path $MvnDir)) { New-Item -ItemType Directory -Path $MvnDir }

    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri "https://archive.apache.org/dist/maven/maven-3/$MvnVersion/binaries/apache-maven-$MvnVersion-bin.zip" -OutFile $MvnZip

    Write-Host "[2/3] Extracting Maven..."
    Expand-Archive -Path $MvnZip -DestinationPath $MvnDir -Force
    Remove-Item $MvnZip
    Write-Host "Maven ready at: $MvnHome"
} else {
    Write-Host "[1/3] Maven already downloaded, skipping."
}

# ── Build and run ─────────────────────────────────────────────────────────────
Write-Host "[3/3] Building and launching Java Platform..."
Write-Host ""
cd $PSScriptRoot

# Optional: Kill stray java processes if they are blocking the 'target' folder
# Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force

& $MvnCmd javafx:run

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "⚠️  Build failed or process interrupted."
    Write-Host "If you see 'Failed to delete target', try closing your IDE or other Java apps."
    Write-Host "Retrying without clean..."
    & $MvnCmd compile javafx:run
}
