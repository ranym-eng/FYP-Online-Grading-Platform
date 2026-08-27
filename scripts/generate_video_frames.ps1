param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

Add-Type -AssemblyName System.Drawing

$videoRoot = Join-Path $ProjectRoot "outputs\video-demo"
$screensRoot = Join-Path $videoRoot "screens"
$framesRoot = Join-Path $videoRoot "frames"
$logoPath = Join-Path $ProjectRoot "frontend\src\assets\sultan-qaboos-university-logo-png_seeklogo-271991.png"

New-Item -ItemType Directory -Force -Path $framesRoot | Out-Null
Get-ChildItem -LiteralPath $framesRoot -Filter "*.jpg" -ErrorAction SilentlyContinue | Remove-Item -Force

$canvasWidth = 1920
$canvasHeight = 1080
$screenWidth = 1600
$screenHeight = 900
$screenX = 160
$screenY = 38

$background = [System.Drawing.ColorTranslator]::FromHtml("#071A14")
$surface = [System.Drawing.ColorTranslator]::FromHtml("#10271F")
$gold = [System.Drawing.ColorTranslator]::FromHtml("#C99A2E")
$cream = [System.Drawing.ColorTranslator]::FromHtml("#F6F1E7")
$muted = [System.Drawing.ColorTranslator]::FromHtml("#B9C9C1")
$green = [System.Drawing.ColorTranslator]::FromHtml("#0D5138")

$fontRegular = New-Object System.Drawing.Font("Segoe UI", 23, [System.Drawing.FontStyle]::Regular)
$fontSmall = New-Object System.Drawing.Font("Segoe UI", 16, [System.Drawing.FontStyle]::Regular)
$fontLabel = New-Object System.Drawing.Font("Segoe UI Semibold", 15, [System.Drawing.FontStyle]::Bold)
$fontTitle = New-Object System.Drawing.Font("Georgia", 58, [System.Drawing.FontStyle]::Bold)
$fontSubtitle = New-Object System.Drawing.Font("Segoe UI", 27, [System.Drawing.FontStyle]::Regular)
$fontBrand = New-Object System.Drawing.Font("Segoe UI Semibold", 24, [System.Drawing.FontStyle]::Bold)

function Save-Jpeg {
    param([System.Drawing.Bitmap]$Bitmap, [string]$Path)
    $codec = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() |
        Where-Object { $_.MimeType -eq "image/jpeg" }
    $parameters = New-Object System.Drawing.Imaging.EncoderParameters(1)
    $parameters.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter(
        [System.Drawing.Imaging.Encoder]::Quality,
        [long]94
    )
    $Bitmap.Save($Path, $codec, $parameters)
    $parameters.Dispose()
}

function New-TitleCard {
    param(
        [string]$Path,
        [string]$Eyebrow,
        [string]$Title,
        [string]$Subtitle
    )

    $bitmap = New-Object System.Drawing.Bitmap($canvasWidth, $canvasHeight)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $graphics.Clear($background)

    $graphics.FillRectangle((New-Object System.Drawing.SolidBrush($green)), 0, 0, 34, $canvasHeight)
    $graphics.FillRectangle((New-Object System.Drawing.SolidBrush($gold)), 34, 0, 5, $canvasHeight)

    if (Test-Path -LiteralPath $logoPath) {
        $logo = [System.Drawing.Image]::FromFile($logoPath)
        $graphics.DrawImage($logo, 142, 118, 150, 174)
        $logo.Dispose()
    }

    $brandBrush = New-Object System.Drawing.SolidBrush($cream)
    $mutedBrush = New-Object System.Drawing.SolidBrush($muted)
    $goldBrush = New-Object System.Drawing.SolidBrush($gold)
    $graphics.DrawString("SULTAN QABOOS UNIVERSITY", $fontBrand, $brandBrush, 330, 142)
    $graphics.DrawString("FINAL YEAR GRADING PLATFORM", $fontSmall, $mutedBrush, 333, 190)
    $graphics.FillRectangle($goldBrush, 142, 338, 250, 5)
    $graphics.DrawString($Eyebrow.ToUpperInvariant(), $fontLabel, $goldBrush, 142, 386)

    $titleFormat = New-Object System.Drawing.StringFormat
    $titleFormat.Trimming = [System.Drawing.StringTrimming]::Word
    $titleFormat.FormatFlags = [System.Drawing.StringFormatFlags]::LineLimit
    $graphics.DrawString($Title, $fontTitle, $brandBrush, (New-Object System.Drawing.RectangleF(136, 450, 1510, 225)), $titleFormat)
    $graphics.DrawString($Subtitle, $fontSubtitle, $mutedBrush, (New-Object System.Drawing.RectangleF(142, 718, 1480, 120)))

    $graphics.FillRectangle((New-Object System.Drawing.SolidBrush($surface)), 142, 910, 1636, 1)
    $graphics.DrawString("Secure role-based evaluation from import to final publication", $fontSmall, $mutedBrush, 142, 946)

    Save-Jpeg -Bitmap $bitmap -Path $Path
    $titleFormat.Dispose()
    $brandBrush.Dispose()
    $mutedBrush.Dispose()
    $goldBrush.Dispose()
    $graphics.Dispose()
    $bitmap.Dispose()
}

function New-ScreenFrame {
    param(
        [string]$Source,
        [string]$Path,
        [string]$Chapter,
        [string]$Caption
    )

    $bitmap = New-Object System.Drawing.Bitmap($canvasWidth, $canvasHeight)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $graphics.Clear($background)

    $shadowBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(95, 0, 0, 0))
    $graphics.FillRectangle($shadowBrush, $screenX + 14, $screenY + 14, $screenWidth, $screenHeight)

    $sourceImage = [System.Drawing.Image]::FromFile($Source)
    $graphics.DrawImage($sourceImage, $screenX, $screenY, $screenWidth, $screenHeight)
    $sourceImage.Dispose()

    $graphics.DrawRectangle((New-Object System.Drawing.Pen($gold, 2)), $screenX, $screenY, $screenWidth, $screenHeight)
    $graphics.FillRectangle((New-Object System.Drawing.SolidBrush($surface)), 0, 960, $canvasWidth, 120)
    $graphics.FillRectangle((New-Object System.Drawing.SolidBrush($gold)), 0, 960, 12, 120)

    $goldBrush = New-Object System.Drawing.SolidBrush($gold)
    $creamBrush = New-Object System.Drawing.SolidBrush($cream)
    $mutedBrush = New-Object System.Drawing.SolidBrush($muted)
    $graphics.DrawString($Chapter.ToUpperInvariant(), $fontLabel, $goldBrush, 64, 981)
    $graphics.DrawString($Caption, $fontRegular, $creamBrush, (New-Object System.Drawing.RectangleF(365, 974, 1440, 70)))
    $graphics.DrawString("SQU · Online FYP Grading Platform", $fontSmall, $mutedBrush, 1500, 1034)

    Save-Jpeg -Bitmap $bitmap -Path $Path
    $shadowBrush.Dispose()
    $goldBrush.Dispose()
    $creamBrush.Dispose()
    $mutedBrush.Dispose()
    $graphics.Dispose()
    $bitmap.Dispose()
}

$sequence = @(
    @{ Kind = "title"; Duration = 4; Eyebrow = "Product demonstration"; Title = "Final Year Grading Platform"; Subtitle = "A complete digital replacement for manual FYP spreadsheets and MATLAB consolidation." },
    @{ Kind = "screen"; Duration = 5; File = "01-login.jpg"; Chapter = "Secure access"; Caption = "Every authorized user signs in and is redirected to a role-specific workspace." },
    @{ Kind = "title"; Duration = 3; Eyebrow = "Chapter 1"; Title = "Centralized Excel Initialization"; Subtitle = "Official academic data is validated and imported once by the administrator." },
    @{ Kind = "screen"; Duration = 5; File = "02-admin-dashboard.jpg"; Chapter = "Administration"; Caption = "The administrator monitors users, projects, phases, evaluations and reports." },
    @{ Kind = "screen"; Duration = 6; File = "03-excel-import.jpg"; Chapter = "Excel import"; Caption = "The initialization workbook creates actors, students, teams, projects and assignments." },
    @{ Kind = "screen"; Duration = 5; File = "04-project-eic01.jpg"; Chapter = "Academic data"; Caption = "Imported projects remain searchable and manageable through compact data tables." },
    @{ Kind = "title"; Duration = 3; Eyebrow = "Chapter 2"; Title = "Role-Based Evaluation"; Subtitle = "Each evaluator sees only assigned projects, authorized forms and applicable phases." },
    @{ Kind = "screen"; Duration = 5; File = "05-supervisor-dashboard.jpg"; Chapter = "Supervisor"; Caption = "The supervisor dashboard displays only assigned projects and required actions." },
    @{ Kind = "screen"; Duration = 6; File = "06-supervisor-form.jpg"; Chapter = "Score sheet"; Caption = "The online grid keeps the familiar spreadsheet-style evaluation experience." },
    @{ Kind = "screen"; Duration = 5; File = "07-supervisor-draft.jpg"; Chapter = "Draft workflow"; Caption = "Scores are saved as a draft in PostgreSQL and remain excluded from final calculations." },
    @{ Kind = "screen"; Duration = 4; File = "08-submit-confirmation.jpg"; Chapter = "Validation"; Caption = "A confirmation dialog prevents accidental irreversible submission." },
    @{ Kind = "screen"; Duration = 5; File = "09-form-locked.jpg"; Chapter = "Locked form"; Caption = "Validated forms are permanently locked and become eligible for consolidation." },
    @{ Kind = "screen"; Duration = 4; File = "10-report-dashboard.jpg"; Chapter = "Report evaluator"; Caption = "Paper report evaluators receive dedicated report forms and assigned projects." },
    @{ Kind = "screen"; Duration = 5; File = "11-report-evaluation-locked.jpg"; Chapter = "Report score"; Caption = "Report criteria are completed at project level and submitted independently." },
    @{ Kind = "screen"; Duration = 4; File = "12-faculty-dashboard.jpg"; Chapter = "Faculty evaluator"; Caption = "Academic evaluators have their own presentation evaluation workspace." },
    @{ Kind = "screen"; Duration = 5; File = "13-oral-evaluation-locked.jpg"; Chapter = "Oral presentation"; Caption = "Individual and group presentation criteria are combined by the grading engine." },
    @{ Kind = "title"; Duration = 3; Eyebrow = "Chapter 3"; Title = "Automated Consolidation"; Subtitle = "The platform reproduces the former MATLAB aggregation with controlled weighting rules." },
    @{ Kind = "screen"; Duration = 7; File = "14-consolidated-grades-calculated.jpg"; Chapter = "Calculation"; Caption = "Locked forms are averaged by evaluator type and weighted into individual final scores." },
    @{ Kind = "screen"; Duration = 4; File = "15-publish-confirmation.jpg"; Chapter = "Publication"; Caption = "The administrator reviews the calculated grades before publication." },
    @{ Kind = "screen"; Duration = 6; File = "16-grades-published.jpg"; Chapter = "Published results"; Caption = "Published grades become visible in authorized workspaces with a complete audit trail." },
    @{ Kind = "title"; Duration = 3; Eyebrow = "Chapter 4"; Title = "Reporting and Notifications"; Subtitle = "Completeness tracking, Excel exports, archived reports and actionable alerts." },
    @{ Kind = "screen"; Duration = 6; File = "17-report-completeness.jpg"; Chapter = "Completeness"; Caption = "The administrator can verify every required evaluator submission before reporting." },
    @{ Kind = "screen"; Duration = 4; File = "18-report-archive-confirmation.jpg"; Chapter = "Report generation"; Caption = "Official phase reports are generated only after explicit confirmation." },
    @{ Kind = "screen"; Duration = 5; File = "19-report-archive.jpg"; Chapter = "Report archive"; Caption = "Generated Excel reports can be downloaded, regenerated or sent to the coordinator." },
    @{ Kind = "screen"; Duration = 6; File = "20-notification-popover.jpg"; Chapter = "Notifications"; Caption = "Actionable notifications open in a compact menu and route users to the correct module." },
    @{ Kind = "screen"; Duration = 5; File = "21-extension-request.jpg"; Chapter = "Deadline request"; Caption = "Evaluators request an extension without choosing a new deadline." },
    @{ Kind = "screen"; Duration = 5; File = "22-extension-decision.jpg"; Chapter = "Administrator decision"; Caption = "Only the administrator defines the extended deadline and approves or rejects the request." },
    @{ Kind = "title"; Duration = 3; Eyebrow = "Chapter 5"; Title = "External and Coordination Workspaces"; Subtitle = "Time-limited Industry Guest access and transparent academic coordination." },
    @{ Kind = "screen"; Duration = 5; File = "24-industry-dashboard.jpg"; Chapter = "Industry Guest"; Caption = "Invited guests access only their assigned Demo Day projects before account expiry." },
    @{ Kind = "screen"; Duration = 6; File = "25-industry-demo-day-only.jpg"; Chapter = "Demo Day only"; Caption = "Industry Guests cannot select supervisor, report or oral evaluation phases." },
    @{ Kind = "screen"; Duration = 5; File = "26-coordinator-dashboard.jpg"; Chapter = "FYP coordinator"; Caption = "The coordinator reviews published grades, reports and evaluation progress." },
    @{ Kind = "title"; Duration = 5; Eyebrow = "End-to-end workflow"; Title = "Ready for Academic Delivery"; Subtitle = "Import · Assign · Evaluate · Lock · Consolidate · Publish · Report" }
)

$concatLines = New-Object System.Collections.Generic.List[string]
$index = 1
foreach ($item in $sequence) {
    $frameName = "{0:D3}.jpg" -f $index
    $framePath = Join-Path $framesRoot $frameName
    if ($item.Kind -eq "title") {
        New-TitleCard -Path $framePath -Eyebrow $item.Eyebrow -Title $item.Title -Subtitle $item.Subtitle
    } else {
        $sourcePath = Join-Path $screensRoot $item.File
        if (-not (Test-Path -LiteralPath $sourcePath)) {
            throw "Missing source screenshot: $sourcePath"
        }
        New-ScreenFrame -Source $sourcePath -Path $framePath -Chapter $item.Chapter -Caption $item.Caption
    }
    $concatLines.Add("file '/work/frames/$frameName'")
    $concatLines.Add("duration $($item.Duration)")
    $index++
}

$lastFrame = "{0:D3}.jpg" -f ($index - 1)
$concatLines.Add("file '/work/frames/$lastFrame'")
[System.IO.File]::WriteAllLines((Join-Path $videoRoot "concat.txt"), $concatLines, [System.Text.UTF8Encoding]::new($false))

$fontRegular.Dispose()
$fontSmall.Dispose()
$fontLabel.Dispose()
$fontTitle.Dispose()
$fontSubtitle.Dispose()
$fontBrand.Dispose()

Write-Host "Generated $($sequence.Count) English video frames in $framesRoot"
