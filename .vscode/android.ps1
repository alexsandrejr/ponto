# Tarefas do app em Kotlin, chamadas pelo .vscode/tasks.json
#   rodar  -> compila, abre o emulador (se preciso), instala e abre o Ponto
#   testar -> roda os testes das regras
#   apk    -> gera o APK de release
param(
    [ValidateSet("rodar", "testar", "apk")]
    [string]$Acao = "rodar",
    [string]$Avd = "ponto_teste"
)

$ErrorActionPreference = "Stop"
$projeto = Join-Path (Split-Path $PSScriptRoot -Parent) "kotlin"
$sdk = Join-Path $env:USERPROFILE "Android\sdk"
$adb = Join-Path $sdk "platform-tools\adb.exe"
$emulador = Join-Path $sdk "emulator\emulator.exe"

# JDK instalado pelo Flet em ~/java, caso o JAVA_HOME não esteja configurado
if (-not $env:JAVA_HOME) {
    $jdk = Get-ChildItem (Join-Path $env:USERPROFILE "java") -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | Select-Object -First 1
    if ($jdk) { $env:JAVA_HOME = $jdk.FullName }
}

function Invoke-Gradle([string[]]$tarefas) {
    Push-Location $projeto
    try {
        & .\gradlew.bat @tarefas
        if ($LASTEXITCODE -ne 0) { throw "O Gradle falhou (veja as mensagens acima)." }
    }
    finally {
        Pop-Location
    }
}

function Test-Dispositivo {
    return [bool]((& $adb devices) -match "`tdevice$")
}

function Wait-Boot {
    Write-Host "Esperando o Android terminar de ligar..."
    & $adb wait-for-device
    while ($true) {
        $pronto = ""
        try { $pronto = "$(& $adb shell getprop sys.boot_completed)".Trim() } catch {}
        if ($pronto -eq "1") { break }
        Start-Sleep -Seconds 2
    }
}

switch ($Acao) {
    "testar" {
        Invoke-Gradle @("testDebugUnitTest")
        Write-Host "`nTodos os testes passaram." -ForegroundColor Green
    }
    "apk" {
        Invoke-Gradle @("assembleRelease")
        Write-Host "`nAPK gerado em kotlin\app\build\outputs\apk\release\app-release.apk" -ForegroundColor Green
    }
    "rodar" {
        # Abre o emulador enquanto o Gradle compila
        if (-not (Test-Dispositivo)) {
            Write-Host "Abrindo o emulador $Avd..."
            Start-Process $emulador -ArgumentList "-avd", $Avd, "-no-audio", "-no-boot-anim"
        }
        Invoke-Gradle @("assembleDebug")
        Wait-Boot
        Invoke-Gradle @("installDebug")
        & $adb shell am start -n com.ponto.ponto/.MainActivity | Out-Null
        Write-Host "`nPonto aberto no emulador." -ForegroundColor Green
    }
}
