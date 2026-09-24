# Windows entry point for scripts/check-before-push.sh.
#
# Why a wrapper and not a port: two implementations of the same gate drift apart, and the one
# nobody runs in CI is the one that silently stops checking. The gates are bash; Git for Windows
# ships bash. This finds it and runs the real script, so Windows and CI run identical checks.
$ErrorActionPreference = 'Stop'
$candidates = @(
    (Join-Path $env:ProgramFiles 'Git\bin\bash.exe'),
    (Join-Path ${env:ProgramFiles(x86)} 'Git\bin\bash.exe'),
    (Join-Path $env:LOCALAPPDATA 'Programs\Git\bin\bash.exe')
)
$bash = $candidates | Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1
if (-not $bash) { $bash = (Get-Command bash -ErrorAction SilentlyContinue).Source }
if (-not $bash) {
    Write-Error 'Git Bash not found. Install Git for Windows (https://git-scm.com/download/win).'
    exit 1
}
$script = Join-Path $PSScriptRoot 'check-before-push.sh'
& $bash $script @args
exit $LASTEXITCODE
