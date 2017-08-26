$Inkscape = "C:\Program Files\Inkscape\inkscape.com"
$Resources = "$PSScriptRoot"
$Destination = Join-Path $PSScriptRoot "..\app\src\main\res"


Write-Host -----------------------------------------------
Write-Host ic_c_launcher_48dp
Write-Host -----------------------------------------------

$Sizes = @( "xxxhdpi", "xxhdpi", "xhdpi", "hdpi", "mdpi" )
foreach ($size in $Sizes)
{
    & $Inkscape `
        --without-gui `
        --export-png="$Destination\drawable-$size\ic_c_launcher_48dp.png" `
        --export-id="e-$size" `
        --export-dpi=96 `
        "$Resources\ic_launcher.svg"
}

Write-Host -----------------------------------------------
Write-Host ic_c_launcher_80dp
Write-Host -----------------------------------------------

$Sizes = @( "xxhdpi", "xhdpi", "hdpi", "mdpi" )
foreach ($size in $Sizes)
{
    & $Inkscape `
        --without-gui `
        --export-png="$Destination\drawable-$size\ic_c_launcher_96dp.png" `
        --export-id="e-$size" `
        --export-dpi=192 `
        "$Resources\ic_launcher.svg"
}
