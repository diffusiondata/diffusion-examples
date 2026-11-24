
$SourceDir = $PSScriptRoot
$TargetDir = "$SourceDir\target"
$CMakeBuildType = if ($env:CMAKE_BUILD_TYPE) { $env:CMAKE_BUILD_TYPE } else { "Release" }

function build($architecture, $platform) {

    $cmakeDir = "$TargetDir\cmake-$architecture"

    Write-Output "[$architecture] Configuring"
    & cmake "-S $SourceDir" "-A $platform" "-B $cmakeDir" "-D ARCHITECTURE=$architecture" "-D TARGET=$TargetDir" "-D OPERATING_SYSTEM=windows" "-D CMAKE_BUILD_TYPE=$CMakeBuildType"

    Write-Output "[$architecture] Building"
    & cmake --build "$cmakeDir" --config "$CMakeBuildType"
}

build "x86" "Win32"
build "x64" "x64"
