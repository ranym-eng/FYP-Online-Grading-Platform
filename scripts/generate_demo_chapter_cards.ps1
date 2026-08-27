param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

Add-Type -AssemblyName System.Drawing

$outputRoot = Join-Path $ProjectRoot "outputs\video-demo\chapter-cards"
$logoPath = Join-Path $ProjectRoot "frontend\src\assets\sultan-qaboos-university-logo-png_seeklogo-271991.png"
$canvasWidth = 1920
$canvasHeight = 1080

$background = [System.Drawing.ColorTranslator]::FromHtml("#071A14")
$surface = [System.Drawing.ColorTranslator]::FromHtml("#10271F")
$gold = [System.Drawing.ColorTranslator]::FromHtml("#C99A2E")
$cream = [System.Drawing.ColorTranslator]::FromHtml("#F6F1E7")
$muted = [System.Drawing.ColorTranslator]::FromHtml("#B9C9C1")
$green = [System.Drawing.ColorTranslator]::FromHtml("#0D5138")
$deepGreen = [System.Drawing.ColorTranslator]::FromHtml("#092219")

$fontBrand = New-Object System.Drawing.Font("Segoe UI Semibold", 24, [System.Drawing.FontStyle]::Bold)
$fontSmall = New-Object System.Drawing.Font("Segoe UI", 16, [System.Drawing.FontStyle]::Regular)
$fontLabel = New-Object System.Drawing.Font("Segoe UI Semibold", 15, [System.Drawing.FontStyle]::Bold)
$fontTitle = New-Object System.Drawing.Font("Georgia", 56, [System.Drawing.FontStyle]::Bold)
$fontSubtitle = New-Object System.Drawing.Font("Segoe UI", 26, [System.Drawing.FontStyle]::Regular)
$fontStep = New-Object System.Drawing.Font("Georgia", 176, [System.Drawing.FontStyle]::Bold)
$fontFooter = New-Object System.Drawing.Font("Segoe UI", 17, [System.Drawing.FontStyle]::Regular)

function New-ChapterCard {
    param(
        [string]$Path,
        [string]$Step,
        [string]$Eyebrow,
        [string]$Title,
        [string]$Subtitle,
        [string]$Footer
    )

    $bitmap = New-Object System.Drawing.Bitmap($canvasWidth, $canvasHeight)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $graphics.Clear($background)

    $greenBrush = New-Object System.Drawing.SolidBrush($green)
    $goldBrush = New-Object System.Drawing.SolidBrush($gold)
    $creamBrush = New-Object System.Drawing.SolidBrush($cream)
    $mutedBrush = New-Object System.Drawing.SolidBrush($muted)
    $surfaceBrush = New-Object System.Drawing.SolidBrush($surface)
    $stepBrush = New-Object System.Drawing.SolidBrush($deepGreen)

    $graphics.FillRectangle($greenBrush, 0, 0, 34, $canvasHeight)
    $graphics.FillRectangle($goldBrush, 34, 0, 5, $canvasHeight)

    if (Test-Path -LiteralPath $logoPath) {
        $logo = [System.Drawing.Image]::FromFile($logoPath)
        $graphics.DrawImage($logo, 142, 104, 150, 174)
        $logo.Dispose()
    }

    $graphics.DrawString("SULTAN QABOOS UNIVERSITY", $fontBrand, $creamBrush, 330, 130)
    $graphics.DrawString("FINAL YEAR GRADING PLATFORM", $fontSmall, $mutedBrush, 333, 179)

    if ($Step -ne "") {
        $graphics.DrawString($Step, $fontStep, $stepBrush, 1495, 80)
    }

    $graphics.FillRectangle($goldBrush, 142, 330, 250, 5)
    $graphics.DrawString($Eyebrow.ToUpperInvariant(), $fontLabel, $goldBrush, 142, 377)

    $titleFormat = New-Object System.Drawing.StringFormat
    $titleFormat.Trimming = [System.Drawing.StringTrimming]::Word
    $titleFormat.FormatFlags = [System.Drawing.StringFormatFlags]::LineLimit
    $graphics.DrawString($Title, $fontTitle, $creamBrush, (New-Object System.Drawing.RectangleF(136, 438, 1510, 230)), $titleFormat)

    $subtitleFormat = New-Object System.Drawing.StringFormat
    $subtitleFormat.Trimming = [System.Drawing.StringTrimming]::Word
    $graphics.DrawString($Subtitle, $fontSubtitle, $mutedBrush, (New-Object System.Drawing.RectangleF(142, 714, 1500, 130)), $subtitleFormat)

    $graphics.FillRectangle($surfaceBrush, 142, 910, 1636, 1)
    $graphics.DrawString($Footer, $fontFooter, $mutedBrush, 142, 948)
    $graphics.DrawString("SQU · FYP", $fontLabel, $goldBrush, 1645, 948)

    $bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)

    $titleFormat.Dispose()
    $subtitleFormat.Dispose()
    $greenBrush.Dispose()
    $goldBrush.Dispose()
    $creamBrush.Dispose()
    $mutedBrush.Dispose()
    $surfaceBrush.Dispose()
    $stepBrush.Dispose()
    $graphics.Dispose()
    $bitmap.Dispose()
}

$cards = @(
    @{ File = "00-opening"; Step = ""; EnEye = "Product demonstration"; EnTitle = "Final Year Grading Platform"; EnSub = "A complete digital replacement for manual FYP spreadsheets and MATLAB consolidation."; FrEye = "Démonstration du produit"; FrTitle = "Plateforme de notation des projets de fin d'études"; FrSub = "Le remplacement numérique du processus Excel et de la consolidation MATLAB." },
    @{ File = "01-excel-initialization"; Step = "01"; EnEye = "Chapter 1"; EnTitle = "Centralized Excel Initialization"; EnSub = "Official academic data, projects, teams and assignments imported through one validated workflow."; FrEye = "Chapitre 1"; FrTitle = "Initialisation centralisée par Excel"; FrSub = "Les données académiques, projets, équipes et affectations sont importés dans un processus validé." },
    @{ File = "02-administration"; Step = "02"; EnEye = "Chapter 2"; EnTitle = "Administration and Academic Data"; EnSub = "Role-based accounts, students, tracks, projects, teams, phases and evaluation forms."; FrEye = "Chapitre 2"; FrTitle = "Administration et données académiques"; FrSub = "Comptes par rôle, étudiants, filières, projets, équipes, phases et formulaires d'évaluation." },
    @{ File = "03-admin-crud"; Step = "03"; EnEye = "Chapter 3"; EnTitle = "Administrator CRUD Operations"; EnSub = "Create, search, update and safely remove academic records from focused management views."; FrEye = "Chapitre 3"; FrTitle = "Opérations de gestion administrateur"; FrSub = "Créer, rechercher, modifier et supprimer les données académiques depuis des vues dédiées." },
    @{ File = "04-supervisor-fyp1"; Step = "04"; EnEye = "Chapter 4"; EnTitle = "Supervisor Evaluation · FYP I"; EnSub = "Individual student assessment with draft saving, final validation and permanent locking."; FrEye = "Chapitre 4"; FrTitle = "Évaluation du superviseur · FYP I"; FrSub = "Évaluation individuelle avec brouillon, validation finale et verrouillage permanent." },
    @{ File = "05-report-evaluation"; Step = "05"; EnEye = "Chapter 5"; EnTitle = "Paper Report Evaluation"; EnSub = "Assigned Report Evaluators complete the official Report 1 and Report 2 criteria."; FrEye = "Chapitre 5"; FrTitle = "Évaluation des rapports écrits"; FrSub = "Les évaluateurs attribués complètent les critères officiels des rapports 1 et 2." },
    @{ File = "06-oral-evaluation"; Step = "06"; EnEye = "Chapter 6"; EnTitle = "Oral Presentation Evaluation"; EnSub = "Faculty Evaluators complete individual and group presentation criteria for assigned projects."; FrEye = "Chapitre 6"; FrTitle = "Évaluation de la présentation orale"; FrSub = "Les évaluateurs académiques notent les critères individuels et collectifs des projets attribués." },
    @{ File = "07-phase1-consolidation"; Step = "07"; EnEye = "Chapter 7"; EnTitle = "Automated Phase I Consolidation"; EnSub = "Locked forms are averaged, weighted and converted into controlled individual grades."; FrEye = "Chapitre 7"; FrTitle = "Consolidation automatique de la Phase I"; FrSub = "Les fiches validées sont moyennées, pondérées et converties en notes individuelles contrôlées." },
    @{ File = "08-deadlines-extensions"; Step = "08"; EnEye = "Chapter 8"; EnTitle = "Deadlines and Personal Extensions"; EnSub = "Expired drafts remain excluded until an evaluator requests and receives additional time."; FrEye = "Chapitre 8"; FrTitle = "Échéances et prolongations personnelles"; FrSub = "Les brouillons expirés restent exclus jusqu'à l'approbation d'une demande de prolongation." },
    @{ File = "09-industry-demo-day"; Step = "09"; EnEye = "Chapter 9"; EnTitle = "Industry Guest · Demo Day"; EnSub = "Invitation-based access limited to assigned prototypes and authorized Demo Day scoring."; FrEye = "Chapitre 9"; FrTitle = "Invité industriel · Demo Day"; FrSub = "Accès par invitation limité aux prototypes attribués et à l'évaluation du Demo Day." },
    @{ File = "10-phase2-consolidation"; Step = "10"; EnEye = "Chapter 10"; EnTitle = "Automated Phase II Consolidation"; EnSub = "Supervisor, report, oral and industry scores are combined using official weighting rules."; FrEye = "Chapitre 10"; FrTitle = "Consolidation automatique de la Phase II"; FrSub = "Les notes superviseur, rapport, oral et industrie sont combinées selon les pondérations officielles." },
    @{ File = "11-reporting-mail"; Step = "11"; EnEye = "Chapter 11"; EnTitle = "Reports, Excel Exports and Email"; EnSub = "Generate, archive, download and send official results through the configured SMTP service."; FrEye = "Chapitre 11"; FrTitle = "Rapports, exports Excel et e-mail"; FrSub = "Générer, archiver, télécharger et envoyer les résultats officiels via le service SMTP configuré." },
    @{ File = "12-coordinator"; Step = "12"; EnEye = "Chapter 12"; EnTitle = "FYP Coordinator Workspace"; EnSub = "Published grades, official reports and academic progress available in a controlled workspace."; FrEye = "Chapitre 12"; FrTitle = "Espace du coordinateur FYP"; FrSub = "Notes publiées, rapports officiels et suivi académique dans un espace contrôlé." },
    @{ File = "13-password-recovery"; Step = "13"; EnEye = "Chapter 13"; EnTitle = "Secure Password Recovery"; EnSub = "Time-limited, single-use reset links are delivered without exposing account existence."; FrEye = "Chapitre 13"; FrTitle = "Récupération sécurisée du mot de passe"; FrSub = "Des liens temporaires à usage unique sont envoyés sans révéler l'existence du compte." },
    @{ File = "14-notifications"; Step = "14"; EnEye = "Chapter 14"; EnTitle = "Deadline Notifications"; EnSub = "Actionable 24-hour and 12-hour reminders guide evaluators to the correct form."; FrEye = "Chapitre 14"; FrTitle = "Notifications d'échéance"; FrSub = "Des rappels à 24 h et 12 h dirigent les évaluateurs vers le formulaire concerné." },
    @{ File = "15-security"; Step = "15"; EnEye = "Chapter 15"; EnTitle = "Role-Based Security Tests"; EnSub = "Project isolation, form restrictions, locked submissions and invalid-access controls."; FrEye = "Chapitre 15"; FrTitle = "Tests de sécurité selon les rôles"; FrSub = "Isolation des projets, restrictions des fiches, soumissions verrouillées et contrôle des accès." },
    @{ File = "16-closing"; Step = ""; EnEye = "End-to-end academic workflow"; EnTitle = "Ready for Academic Delivery"; EnSub = "Import · Assign · Evaluate · Lock · Consolidate · Publish · Report"; FrEye = "Processus académique complet"; FrTitle = "Prête pour la livraison académique"; FrSub = "Importer · Affecter · Évaluer · Verrouiller · Consolider · Publier · Rapporter" }
)

$englishRoot = Join-Path $outputRoot "EN"
New-Item -ItemType Directory -Force -Path $englishRoot | Out-Null
Get-ChildItem -LiteralPath $englishRoot -Filter "*.png" -ErrorAction SilentlyContinue | Remove-Item -Force

foreach ($card in $cards) {
    $path = Join-Path $englishRoot ($card.File + ".png")
    New-ChapterCard -Path $path -Step $card.Step -Eyebrow $card.EnEye -Title $card.EnTitle -Subtitle $card.EnSub -Footer "Secure role-based evaluation from import to final publication"
}

$fontBrand.Dispose()
$fontSmall.Dispose()
$fontLabel.Dispose()
$fontTitle.Dispose()
$fontSubtitle.Dispose()
$fontStep.Dispose()
$fontFooter.Dispose()

Write-Host "Generated $($cards.Count) English chapter cards in $englishRoot"
