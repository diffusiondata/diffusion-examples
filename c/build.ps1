
$SourceDir = $PSScriptRoot
$TargetDir = "$SourceDir\target"

function build($architecture) {

    $cmakeDir = "$TargetDir\cmake-$architecture"

    Write-Output "[$architecture] Configuring"
    & cmake "-S $SourceDir" "-A Win32" "-B $cmakeDir" "-D ARCHITECTURE=$architecture" "-D TARGET=$TargetDir" "-D OPERATING_SYSTEM=windows"

    Write-Output "[$architecture] Building"
    & cmake --build "$cmakeDir" --config "Release"
}

build "x86"
build "x64"
