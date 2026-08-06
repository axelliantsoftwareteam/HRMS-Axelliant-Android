param(
    [string]$HrisRoot = "D:\gitAxelliant\HRIS\HRMS-Axelliant-Android",
    [string]$InternalAppsRoot = "D:\gitAxelliant\Android-ERP\InternalAppsAndroid"
)

$ErrorActionPreference = "Stop"

function Get-ResourceFiles {
    param([string]$Root, [string]$AppName)

    $resRoot = Join-Path $Root "app\src\main\res"
    Get-ChildItem -Path $resRoot -Recurse -File | ForEach-Object {
        $folder = $_.Directory.Name -replace "-.*$", ""
        [PSCustomObject]@{
            App = $AppName
            Type = $folder
            Name = $_.BaseName
            Path = $_.FullName.Substring($resRoot.Length + 1)
        }
    }
}

function Get-ValueResources {
    param([string]$Root, [string]$AppName)

    $valuesRoot = Join-Path $Root "app\src\main\res\values"
    if (!(Test-Path $valuesRoot)) {
        return
    }

    Get-ChildItem -Path $valuesRoot -Filter "*.xml" -File | ForEach-Object {
        $fileName = $_.Name
        Select-String -Path $_.FullName -Pattern '<(color|string|dimen|integer|style|attr|declare-styleable)\s+name="([^"]+)"' | ForEach-Object {
            [PSCustomObject]@{
                App = $AppName
                Type = $_.Matches[0].Groups[1].Value
                Name = $_.Matches[0].Groups[2].Value
                Path = $fileName
            }
        }
    }
}

function Get-CodeFiles {
    param([string]$Root, [string]$AppName)

    $javaRoot = Join-Path $Root "app\src\main\java"
    Get-ChildItem -Path $javaRoot -Recurse -Include "*.kt", "*.java" -File | ForEach-Object {
        [PSCustomObject]@{
            App = $AppName
            Name = $_.BaseName
            Path = $_.FullName.Substring($javaRoot.Length + 1)
        }
    }
}

function Write-CollisionTable {
    param(
        [string]$Title,
        [object[]]$Left,
        [object[]]$Right,
        [string[]]$Columns
    )

    Write-Host ""
    Write-Host "== $Title =="

    $rows = foreach ($leftItem in $Left) {
        foreach ($rightItem in ($Right | Where-Object { $_.Name -eq $leftItem.Name -and (!$leftItem.Type -or $_.Type -eq $leftItem.Type) })) {
            [PSCustomObject]@{
                Type = $leftItem.Type
                Name = $leftItem.Name
                HRIS = $leftItem.Path
                InternalApps = $rightItem.Path
            }
        }
    }

    $rows | Sort-Object Type, Name, HRIS, InternalApps -Unique | Select-Object $Columns | Format-Table -AutoSize
}

$hrisResources = @(Get-ResourceFiles -Root $HrisRoot -AppName "HRIS")
$iaResources = @(Get-ResourceFiles -Root $InternalAppsRoot -AppName "InternalApps")
$hrisValues = @(Get-ValueResources -Root $HrisRoot -AppName "HRIS")
$iaValues = @(Get-ValueResources -Root $InternalAppsRoot -AppName "InternalApps")
$hrisCode = @(Get-CodeFiles -Root $HrisRoot -AppName "HRIS")
$iaCode = @(Get-CodeFiles -Root $InternalAppsRoot -AppName "InternalApps")

Write-CollisionTable `
    -Title "Resource file collisions" `
    -Left $hrisResources `
    -Right $iaResources `
    -Columns @("Type", "Name", "HRIS", "InternalApps")

Write-CollisionTable `
    -Title "Value resource collisions" `
    -Left $hrisValues `
    -Right $iaValues `
    -Columns @("Type", "Name", "HRIS", "InternalApps")

Write-CollisionTable `
    -Title "Kotlin/Java file-name collisions" `
    -Left $hrisCode `
    -Right $iaCode `
    -Columns @("Name", "HRIS", "InternalApps")
