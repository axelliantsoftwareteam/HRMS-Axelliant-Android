param(
    [string]$Root = "D:\gitAxelliant\HRIS\HRMS-Axelliant-Android",
    [switch]$SummaryOnly
)

$ErrorActionPreference = "Stop"

$javaRoot = Join-Path $Root "app\src\main\java"
$layoutRoot = Join-Path $Root "app\src\main\res\layout"
$resRoot = Join-Path $Root "app\src\main\res"
$designSystemPathPart = "ui\designsystem"

function Get-RelativePath {
    param([string]$Base, [string]$Path)
    return $Path.Substring($Base.Length + 1)
}

function Search-Files {
    param(
        [string]$Path,
        [string]$Include,
        [string]$Pattern
    )

    if (!(Test-Path $Path)) {
        return @()
    }

    @(Get-ChildItem -Path $Path -Recurse -Include $Include -File | ForEach-Object {
        $matches = Select-String -Path $_.FullName -Pattern $Pattern -SimpleMatch
        foreach ($match in $matches) {
            [PSCustomObject]@{
                File = $_.FullName
                Line = $match.LineNumber
                Text = $match.Line.Trim()
            }
        }
    })
}

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "== $Title =="
}

$composeViews = @(Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "androidx.compose.ui.platform.ComposeView")
$setContentCalls = @(
    Search-Files -Path $javaRoot -Include "*.kt" -Pattern "setContent {"
    Search-Files -Path $javaRoot -Include "*.kt" -Pattern "setFluentContent {"
)
$androidViewBindingCalls = @(Search-Files -Path $javaRoot -Include "*.kt" -Pattern "AndroidViewBinding")
$fluentImports = @(Search-Files -Path $javaRoot -Include "*.kt" -Pattern "com.microsoft.fluentui")
$fluentOutsideDesignSystem = @($fluentImports | Where-Object { $_.File -notlike "*$designSystemPathPart*" })
$appTextViewLayouts = @(Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "com.axelliant.hris.ui.designsystem.components.AppTextView")
$materialTextViewLayouts = @(Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "com.google.android.material.textview.MaterialTextView")
$plainTextViewLayouts = @(Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "<TextView")
$fluentScreenStyles = @(Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "Layout.Fluent2.Screen")
$fluentGuidelineStyles = @(Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "Guideline.Fluent2")
$legacyGuidelineStyles = @(
    Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "@style/start_gl"
    Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "@style/end_gl"
    Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "@style/top_gl"
    Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "@style/bottom_gl"
    Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "@style/ia_start_gl"
    Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "@style/ia_end_gl"
    Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "@style/ia_top_gl"
    Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "@style/ia_bottom_gl"
)
$rawProgressBars = @(Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "<ProgressBar")
$rawCheckBoxes = @(Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "<CheckBox")
$materialCheckBoxes = @(Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "com.google.android.material.checkbox.MaterialCheckBox")
$appProgressBars = @(Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "com.axelliant.hris.ui.designsystem.components.AppProgressBarView")
$appCheckBoxes = @(Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "com.axelliant.hris.ui.designsystem.components.AppCheckboxView")
$programmaticProgressBars = @(
    Search-Files -Path $javaRoot -Include "*.kt" -Pattern "ProgressBar(" |
        Where-Object { $_.File -notlike "*$designSystemPathPart*" }
)
$programmaticCheckBoxes = @(
    Search-Files -Path $javaRoot -Include "*.kt" -Pattern "CheckBox(" |
        Where-Object { $_.File -notlike "*$designSystemPathPart*" }
)
$materialTextInputLayouts = @(Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "com.google.android.material.textfield.TextInputLayout")
$appTextFieldLayouts = @(Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "com.axelliant.hris.ui.designsystem.components.AppTextFieldLayout")
$appTextFieldViews = @(Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "com.axelliant.hris.ui.designsystem.components.AppTextFieldView")
$rawEditTextLayouts = @(
    Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "<EditText" |
        Where-Object { $_.Text -notlike "<!--*" }
)
$materialButtons = @(Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "com.google.android.material.button.MaterialButton")
$appButtons = @(Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "com.axelliant.hris.ui.designsystem.components.AppButtonView")
$materialCards = @(Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "com.google.android.material.card.MaterialCardView")
$appCards = @(Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "com.axelliant.hris.ui.designsystem.components.AppCardView")
$rawCardViews = @(
    Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "<androidx.cardview.widget.CardView" |
        Where-Object { $_.Text -notlike "<!--*" }
)
$materialFloatingActionButtons = @(
    Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "com.google.android.material.floatingactionbutton.FloatingActionButton" |
        Where-Object { $_.Text -notlike "<!--*" }
)
$appFloatingActionButtons = @(Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "com.axelliant.hris.ui.designsystem.components.AppFloatingActionButtonView")
$bottomSheetDialogCalls = @(Search-Files -Path $javaRoot -Include "*.kt" -Pattern "BottomSheetDialog(")
$directBottomSheetDialogs = @(
    $bottomSheetDialogCalls |
        Where-Object { $_.File -notlike "*$designSystemPathPart*" -and $_.Text -notlike "*createAppBottomSheetDialog*" }
)
$appBottomSheetDialogs = @(
    Search-Files -Path $javaRoot -Include "*.kt" -Pattern "createAppBottomSheetDialog(" |
        Where-Object { $_.File -notlike "*$designSystemPathPart*" }
)
$legacyHrisTextStyleUsages = @(
    Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "@style/TEXT_" |
        Where-Object { $_.Text -notlike "<!--*" }
)
$legacyInternalAppsTextStyleUsages = @(
    Search-Files -Path $layoutRoot -Include "*.xml" -Pattern "@style/IA_TEXT_" |
        Where-Object { $_.Text -notlike "<!--*" }
)
$legacyColorAliasReferences = @(
    Search-Files -Path $resRoot -Include "*.xml" -Pattern "@color/black"
    Search-Files -Path $resRoot -Include "*.xml" -Pattern "@color/white"
    Search-Files -Path $resRoot -Include "*.xml" -Pattern "@color/grey"
    Search-Files -Path $resRoot -Include "*.xml" -Pattern "@color/btn_text_color"
    Search-Files -Path $resRoot -Include "*.xml" -Pattern "@color/colorApp"
) | Where-Object {
    $_.Text -notlike "<!--*" -and
    $_.File -notlike "*\res\values\colors.xml"
}
$globalComponents = @(Get-ChildItem -Path (Join-Path $javaRoot "com\axelliant\hris\components") -Filter "*.kt" -File -ErrorAction SilentlyContinue)
$designSystemComponents = @(Get-ChildItem -Path (Join-Path $javaRoot "com\axelliant\hris\ui\designsystem\components") -Filter "*.kt" -File -ErrorAction SilentlyContinue)

Write-Section "UI architecture summary"
[PSCustomObject]@{
    ComposeViewLayouts = $composeViews.Count
    SetContentCalls = $setContentCalls.Count
    AndroidViewBindingCalls = $androidViewBindingCalls.Count
    FluentImports = $fluentImports.Count
    FluentImportsOutsideDesignSystem = $fluentOutsideDesignSystem.Count
    AppTextViewLayouts = $appTextViewLayouts.Count
    MaterialTextViewLayouts = $materialTextViewLayouts.Count
    PlainTextViewLayouts = $plainTextViewLayouts.Count
    FluentScreenStyleUsages = $fluentScreenStyles.Count
    FluentGuidelineStyleUsages = $fluentGuidelineStyles.Count
    LegacyGuidelineStyleUsages = $legacyGuidelineStyles.Count
    AppProgressBarLayouts = $appProgressBars.Count
    RawProgressBarLayouts = $rawProgressBars.Count
    DirectProgressBarConstructorCalls = $programmaticProgressBars.Count
    AppCheckboxLayouts = $appCheckBoxes.Count
    RawCheckboxLayouts = $rawCheckBoxes.Count
    MaterialCheckboxLayouts = $materialCheckBoxes.Count
    DirectCheckBoxConstructorCalls = $programmaticCheckBoxes.Count
    AppTextFieldLayoutUsages = $appTextFieldLayouts.Count
    AppTextFieldViewUsages = $appTextFieldViews.Count
    MaterialTextInputLayoutUsages = $materialTextInputLayouts.Count
    RawEditTextLayouts = $rawEditTextLayouts.Count
    AppButtonLayouts = $appButtons.Count
    MaterialButtonLayouts = $materialButtons.Count
    AppCardLayouts = $appCards.Count
    MaterialCardLayouts = $materialCards.Count
    RawCardViewLayouts = $rawCardViews.Count
    AppFloatingActionButtonLayouts = $appFloatingActionButtons.Count
    MaterialFloatingActionButtonLayouts = $materialFloatingActionButtons.Count
    AppBottomSheetDialogCalls = $appBottomSheetDialogs.Count
    DirectBottomSheetDialogCalls = $directBottomSheetDialogs.Count
    LegacyHrisTextStyleUsages = $legacyHrisTextStyleUsages.Count
    LegacyInternalAppsTextStyleUsages = $legacyInternalAppsTextStyleUsages.Count
    LegacyColorAliasReferences = $legacyColorAliasReferences.Count
    GlobalComponentFiles = $globalComponents.Count
    DesignSystemComponentFiles = $designSystemComponents.Count
} | Format-List

if ($SummaryOnly) {
    return
}

Write-Section "ComposeView layout usages"
$composeViews |
    Sort-Object File, Line |
    Select-Object @{Name="Path";Expression={Get-RelativePath -Base $Root -Path $_.File}}, Line, Text |
    Format-Table -AutoSize

Write-Section "setContent usages"
$setContentCalls |
    Sort-Object File, Line |
    Select-Object @{Name="Path";Expression={Get-RelativePath -Base $Root -Path $_.File}}, Line, Text |
    Format-Table -AutoSize

Write-Section "AndroidViewBinding usages"
$androidViewBindingCalls |
    Sort-Object File, Line |
    Select-Object @{Name="Path";Expression={Get-RelativePath -Base $Root -Path $_.File}}, Line, Text |
    Format-Table -AutoSize

Write-Section "Text view layout usages"
@(
    [PSCustomObject]@{ Type = "AppTextView"; Count = $appTextViewLayouts.Count }
    [PSCustomObject]@{ Type = "MaterialTextView"; Count = $materialTextViewLayouts.Count }
    [PSCustomObject]@{ Type = "Plain TextView"; Count = $plainTextViewLayouts.Count }
) | Format-Table -AutoSize

Write-Section "Screen shell usage"
@(
    [PSCustomObject]@{ Type = "Layout.Fluent2.Screen"; Count = $fluentScreenStyles.Count }
    [PSCustomObject]@{ Type = "Guideline.Fluent2"; Count = $fluentGuidelineStyles.Count }
    [PSCustomObject]@{ Type = "Legacy guideline aliases"; Count = $legacyGuidelineStyles.Count }
) | Format-Table -AutoSize

Write-Section "Primitive bridge usage"
@(
    [PSCustomObject]@{ Type = "AppProgressBarView"; Count = $appProgressBars.Count }
    [PSCustomObject]@{ Type = "Raw ProgressBar"; Count = $rawProgressBars.Count }
    [PSCustomObject]@{ Type = "Direct ProgressBar constructor"; Count = $programmaticProgressBars.Count }
    [PSCustomObject]@{ Type = "AppCheckboxView"; Count = $appCheckBoxes.Count }
    [PSCustomObject]@{ Type = "Raw CheckBox"; Count = $rawCheckBoxes.Count }
    [PSCustomObject]@{ Type = "MaterialCheckBox"; Count = $materialCheckBoxes.Count }
    [PSCustomObject]@{ Type = "Direct CheckBox constructor"; Count = $programmaticCheckBoxes.Count }
    [PSCustomObject]@{ Type = "AppTextFieldLayout"; Count = $appTextFieldLayouts.Count }
    [PSCustomObject]@{ Type = "AppTextFieldView"; Count = $appTextFieldViews.Count }
    [PSCustomObject]@{ Type = "Material TextInputLayout"; Count = $materialTextInputLayouts.Count }
    [PSCustomObject]@{ Type = "Raw EditText"; Count = $rawEditTextLayouts.Count }
    [PSCustomObject]@{ Type = "AppButtonView"; Count = $appButtons.Count }
    [PSCustomObject]@{ Type = "MaterialButton"; Count = $materialButtons.Count }
    [PSCustomObject]@{ Type = "AppCardView"; Count = $appCards.Count }
    [PSCustomObject]@{ Type = "MaterialCardView"; Count = $materialCards.Count }
    [PSCustomObject]@{ Type = "Raw AndroidX CardView"; Count = $rawCardViews.Count }
    [PSCustomObject]@{ Type = "AppFloatingActionButtonView"; Count = $appFloatingActionButtons.Count }
    [PSCustomObject]@{ Type = "Material FloatingActionButton"; Count = $materialFloatingActionButtons.Count }
    [PSCustomObject]@{ Type = "createAppBottomSheetDialog"; Count = $appBottomSheetDialogs.Count }
    [PSCustomObject]@{ Type = "Direct BottomSheetDialog"; Count = $directBottomSheetDialogs.Count }
) | Format-Table -AutoSize

Write-Section "Legacy style and token alias usage"
@(
    [PSCustomObject]@{ Type = "HRIS TEXT_* style aliases"; Count = $legacyHrisTextStyleUsages.Count }
    [PSCustomObject]@{ Type = "Internal Apps IA_TEXT_* style aliases"; Count = $legacyInternalAppsTextStyleUsages.Count }
    [PSCustomObject]@{ Type = "Legacy color aliases outside colors.xml"; Count = $legacyColorAliasReferences.Count }
) | Format-Table -AutoSize

Write-Section "Direct Fluent imports outside ui/designsystem"
$fluentOutsideDesignSystem |
    Sort-Object File, Line |
    Select-Object @{Name="Path";Expression={Get-RelativePath -Base $Root -Path $_.File}}, Line, Text |
    Format-Table -AutoSize

Write-Section "Global component files to classify"
$globalComponents |
    Sort-Object Name |
    Select-Object Name, @{Name="Path";Expression={Get-RelativePath -Base $Root -Path $_.FullName}} |
    Format-Table -AutoSize

Write-Section "Design-system component files"
$designSystemComponents |
    Sort-Object Name |
    Select-Object Name, @{Name="Path";Expression={Get-RelativePath -Base $Root -Path $_.FullName}} |
    Format-Table -AutoSize
