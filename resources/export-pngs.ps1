Param(
    [string]$Icon = "all"
)

$Inkscape = "C:\Program Files\Inkscape\inkscape.com"
$Resources = "$PSScriptRoot"
$Destination = Join-Path $PSScriptRoot "..\app\src\main\res"


if ($Icon -eq "all" -or $Icon -eq "bg_drawer_top")
{
    Write-Host -----------------------------------------------
    Write-Host bg_drawer_top
    Write-Host -----------------------------------------------

    $Sizes = @( "xxhdpi", "xhdpi", "hdpi", "mdpi" )
    $DPIs =  @( 288,      192,     144,    96     )
    for ($i = 0; $i -lt $Sizes.Length; $i++)
    {
        & $Inkscape `
            --without-gui `
            --export-png="$Destination\drawable-$($Sizes[$i])\bg_drawer_top.png" `
            --export-area-page `
            --export-dpi=$($DPIs[$i]) `
            "$Resources\bg_drawer_top.svg"
    }
}

if ($Icon -eq "all" -or $Icon -eq "ic_launcher")
{
    Write-Host -----------------------------------------------
    Write-Host ic_launcher
    Write-Host -----------------------------------------------

    $Sizes = @( "xxxhdpi", "xxhdpi", "xhdpi", "hdpi", "mdpi" )
    $DPIs =  @( 384,       288,      192,     144,    96     )
    for ($i = 0; $i -lt $Sizes.Length; $i++)
    {
        & $Inkscape `
            --without-gui `
            --export-png="$Destination\mipmap-$($Sizes[$i])\ic_launcher.png" `
            --export-area-page `
            --export-dpi=$($DPIs[$i]) `
            "$Resources\ic_launcher.svg"
    }
}

if ($Icon -eq "all" -or $Icon -eq "ic_c_notification_24dp")
{
    Write-Host -----------------------------------------------
    Write-Host ic_c_notification_24dp
    Write-Host -----------------------------------------------

    $Sizes = @( "xxhdpi", "xhdpi", "hdpi", "mdpi" )
    $DPIs =  @( 288,      192,     144,    96     )
    for ($i = 0; $i -lt $Sizes.Length; $i++)
    {
        & $Inkscape `
            --without-gui `
            --export-png="$Destination\drawable-$($Sizes[$i])\ic_c_notification_24dp.png" `
            --export-area-page `
            --export-dpi=$($DPIs[$i]) `
            "$Resources\ic_notification.svg"
    }
}
