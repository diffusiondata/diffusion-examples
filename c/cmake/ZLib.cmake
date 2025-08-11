if(WIN32)
    # Windows (32-bit or 64-bit)
    add_library(zlib SHARED IMPORTED)

    if(CMAKE_SIZEOF_VOID_P MATCHES 8)
        set(ARCHITECTURE "x64")
    elseif(CMAKE_SIZEOF_VOID_P MATCHES 4)
        set(ARCHITECTURE "x86")
    endif()

    cmake_path(SET ZLIB_ROOT_PATH ${DIFFUSION_ROOT_FOLDER}/dependencies/zlib/${ARCHITECTURE})
    cmake_path(SET ZLIB_LIB_PATH ${ZLIB_ROOT_PATH}/lib/zlib.lib)
    cmake_path(SET ZLIB_INCLUDE_PATH ${ZLIB_ROOT_PATH}/include)

    set_target_properties(zlib PROPERTIES
        IMPORTED_IMPLIB ${ZLIB_LIB_PATH}
        IMPORTED_LOCATION ${ZLIB_LIB_PATH}
        INTERFACE_INCLUDE_DIRECTORIES ${ZLIB_INCLUDE_PATH}
    )

    get_target_property(zlib_INCLUDE_DIRECTORIES zlib INTERFACE_INCLUDE_DIRECTORIES)

    set(INCLUDE_DIRECTORIES ${INCLUDE_DIRECTORIES} ${zlib_INCLUDE_DIRECTORIES})

    set(DEPENDENCIES ${DEPENDENCIES} zlib)

    # list(APPEND WINDOWS_PATH ${ZLIB_LIB_PATH})

    include_directories(${zlib_INCLUDE_DIRECTORIES})

    message(STATUS "")
    message(STATUS "ZLIB has been imported from ${ZLIB_ROOT_PATH}")
    message(STATUS "    - ${zlib_INCLUDE_DIRECTORIES}")
    message(STATUS "")

elseif(UNIX)
    # Unix (MacOS or Linux)

    if (CMAKE_HOST_SYSTEM_NAME MATCHES "Darwin")
        # MacOS
        message(STATUS "HOMEBREW_ROOT_PATH is ${HOMEBREW_ROOT_PATH}")

        execute_process(
            COMMAND ${HOMEBREW_ROOT_PATH}/bin/brew --prefix zlib
            RESULT_VARIABLE BREW_ZLIB
            OUTPUT_VARIABLE BREW_ZLIB_PREFIX
            OUTPUT_STRIP_TRAILING_WHITESPACE
        )

        if (BREW_ZLIB EQUAL 0 AND EXISTS "${BREW_ZLIB_PREFIX}")
            message(STATUS "Found ZLIB installed by Homebrew at ${BREW_ZLIB_PREFIX}")

            add_library(zlib SHARED IMPORTED)

            cmake_path(SET ZLIB_LIB_PATH ${BREW_ZLIB_PREFIX}/lib/libz.a)
            cmake_path(SET ZLIB_INCLUDE_PATH ${BREW_ZLIB_PREFIX}/include)

            set_target_properties(zlib PROPERTIES
                IMPORTED_IMPLIB ${ZLIB_LIB_PATH}
                IMPORTED_LOCATION ${ZLIB_LIB_PATH}
                INTERFACE_INCLUDE_DIRECTORIES ${ZLIB_INCLUDE_PATH}
            )

            get_target_property(zlib_INTERFACE_INCLUDE_DIRECTORIES zlib INTERFACE_INCLUDE_DIRECTORIES)

            set(INCLUDE_DIRECTORIES ${INCLUDE_DIRECTORIES} ${zlib_INTERFACE_INCLUDE_DIRECTORIES})

            set(DEPENDENCIES ${DEPENDENCIES} zlib)

            message(STATUS "")
            message(STATUS "ZLIB has been imported from ${BREW_ZLIB_PREFIX} for architecture ${ARCHITECTURE}")
            message(STATUS "    - ${zlib_INTERFACE_INCLUDE_DIRECTORIES}")
            message(STATUS "")

        else()
            message(STATUS "Unable to find ZLIB in this machine for architecture ${ARCHITECTURE}")

        endif()

    else()
        # Linux
        set(ADDITIONAL_LD_FLAGS ${ADDITIONAL_LD_FLAGS} "-lz")

    endif()


endif()

