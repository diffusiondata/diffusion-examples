#!/bin/bash

ROOT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
SOURCE_DIR="$ROOT_DIR"
TARGET_DIR="$ROOT_DIR/target"


if [ ! -d $TARGET_DIR ]; then
    echo "Creating folder $TARGET_DIR"
    mkdir -p $TARGET_DIR
fi


function build_linux() {
    build_for_unix "x86_64" "linux" false
}


function build_macos() {
    build_for_unix "x86_64" "macos" true
    build_for_unix "arm64" "macos" true
}


function build_for_unix() {
    local architecture=$1
    local operating_system=$2
    local is_macos=$3
    local cmake_dir="$TARGET_DIR/cmake-$operating_system-$architecture"
    echo "About to run cmake for $operating_system / $architecture"
    echo "Build directory: $cmake_dir"

    # Define reusable build steps
    build_commands() {
        set -e
        echo "Running cmake for $operating_system / $architecture"
        echo "Build directory: $cmake_dir"
        echo "---"
        set -x

        if $is_macos ; then
            if [[ "$architecture" == "x86_64" ]]; then
                /usr/local/bin/brew shellenv

            elif [[ "$architecture" == "arm64" ]]; then
                /opt/homebrew/bin/brew shellenv
            fi
        fi

        cmake --fresh \
            -S"$SOURCE_DIR" \
            -B"$cmake_dir" \
            -DTARGET="$TARGET_DIR" \
            -DOPERATING_SYSTEM="$operating_system" \
            -DARCHITECTURE="$architecture" \
            -DARCHFLAGS="-arch ${architecture}" \
            -DCMAKE_OSX_ARCHITECTURES="${architecture}"

        cmake --build "$cmake_dir"
        set +x
    }

    if $is_macos; then
        export SOURCE_DIR TARGET_DIR cmake_dir operating_system architecture is_macos

        # Pass function definition and invoke it inside the new shell
        set -x
        arch "-$architecture" /bin/zsh --login -c "$(declare -f build_commands); source ~/.zshrc && source ~/.profile && source ~/.zprofile && set -a; SOURCE_DIR=${SOURCE_DIR}; is_macos=${is_macos}; TARGET_DIR=${TARGET_DIR}; cmake_dir=${cmake_dir}; operating_system=${operating_system}; architecture=${architecture}; build_commands" || exit 1
        set +x
    else
        build_commands
    fi

}


case "$OSTYPE" in
  solaris*) build_linux ;;
  darwin*)  build_macos ;;
  linux*)   build_linux ;;
  bsd*)     build_linux ;;
  *)        echo "unknown: $OSTYPE"; exit 1 ;;
esac
