if(WIN32)
    # Windows (32-bit or 64-bit)
    add_library(pcre SHARED IMPORTED)
    add_library(pcre_posix SHARED IMPORTED)

    if(CMAKE_SIZEOF_VOID_P MATCHES 8)
        set(ARCHITECTURE "x64")
    elseif(CMAKE_SIZEOF_VOID_P MATCHES 4)
        set(ARCHITECTURE "x86")
    endif()

    cmake_path(SET PCRE_ROOT_PATH ${DIFFUSION_ROOT_FOLDER}/dependencies/pcre/${ARCHITECTURE})
    cmake_path(SET PCRE_LIB_PATH ${PCRE_ROOT_PATH}/lib/pcre3.lib)
    cmake_path(SET PCRE_POSIX_LIB_PATH ${PCRE_ROOT_PATH}/lib/pcreposix3.lib)
    cmake_path(SET PCRE_INCLUDE_PATH ${PCRE_ROOT_PATH}/include)
    cmake_path(SET PCRE_BIN_PATH ${PCRE_ROOT_PATH}/bin)
    cmake_path(SET PCRE_DLL_PATH ${PCRE_BIN_PATH}/pcre3.dll)
    cmake_path(SET PCRE_POSIX_DLL_PATH ${PCRE_BIN_PATH}/pcreposix3.dll)

    set_target_properties(pcre PROPERTIES
        IMPORTED_IMPLIB ${PCRE_LIB_PATH}
        IMPORTED_LOCATION ${PCRE_LIB_PATH}
        INTERFACE_INCLUDE_DIRECTORIES ${PCRE_INCLUDE_PATH}
    )

    set_target_properties(pcre_posix PROPERTIES
        IMPORTED_IMPLIB ${PCRE_POSIX_LIB_PATH}
        IMPORTED_LOCATION ${PCRE_POSIX_LIB_PATH}
        INTERFACE_INCLUDE_DIRECTORIES ${PCRE_INCLUDE_PATH}
    )

    get_target_property(pcre_INTERFACE_INCLUDE_DIRECTORIES pcre INTERFACE_INCLUDE_DIRECTORIES)
    get_target_property(pcre_posix_INTERFACE_INCLUDE_DIRECTORIES pcre_posix INTERFACE_INCLUDE_DIRECTORIES)

    set(INCLUDE_DIRECTORIES ${INCLUDE_DIRECTORIES} ${pcre_INTERFACE_INCLUDE_DIRECTORIES})
    set(INCLUDE_DIRECTORIES ${INCLUDE_DIRECTORIES} ${pcre_posix_INTERFACE_INCLUDE_DIRECTORIES})

    set(DEPENDENCIES ${DEPENDENCIES} pcre pcre_posix)

    list(APPEND WINDOWS_PATH ${PCRE_DLL_PATH} ${PCRE_POSIX_DLL_PATH})

    message(STATUS "")
    message(STATUS "PCRE has been imported from ${PCRE_ROOT_PATH}")
    message(STATUS "    - ${pcre_INTERFACE_INCLUDE_DIRECTORIES}")
    message(STATUS "    - ${pcre_posix_INTERFACE_INCLUDE_DIRECTORIES}")
    message(STATUS "")

elseif(UNIX)
    # Unix (MacOS or Linux)

    if (CMAKE_HOST_SYSTEM_NAME MATCHES "Darwin")
        # MacOS
        message(STATUS "HOMEBREW_ROOT_PATH is ${HOMEBREW_ROOT_PATH}")

        execute_process(
            COMMAND ${HOMEBREW_ROOT_PATH}/bin/brew --prefix pcre
            RESULT_VARIABLE BREW_PCRE
            OUTPUT_VARIABLE BREW_PCRE_PREFIX
            OUTPUT_STRIP_TRAILING_WHITESPACE
        )

        if (BREW_PCRE EQUAL 0 AND EXISTS "${BREW_PCRE_PREFIX}")
            message(STATUS "Found PCRE installed by Homebrew at ${BREW_PCRE_PREFIX}")

            add_library(pcre SHARED IMPORTED)
            add_library(pcre_posix SHARED IMPORTED)

            cmake_path(SET PCRE_LIB_PATH ${BREW_PCRE_PREFIX}/lib/libpcre.a)
            cmake_path(SET PCRE_POSIX_LIB_PATH ${BREW_PCRE_PREFIX}/lib/libpcreposix.a)
            cmake_path(SET PCRE_INCLUDE_PATH ${BREW_PCRE_PREFIX}/include)

            set_target_properties(pcre PROPERTIES
                IMPORTED_IMPLIB ${PCRE_LIB_PATH}
                IMPORTED_LOCATION ${PCRE_LIB_PATH}
                INTERFACE_INCLUDE_DIRECTORIES ${PCRE_INCLUDE_PATH}
            )

            set_target_properties(pcre_posix PROPERTIES
                IMPORTED_IMPLIB ${PCRE_POSIX_LIB_PATH}
                IMPORTED_LOCATION ${PCRE_POSIX_LIB_PATH}
                INTERFACE_INCLUDE_DIRECTORIES ${PCRE_INCLUDE_PATH}
            )

            get_target_property(pcre_INTERFACE_INCLUDE_DIRECTORIES pcre INTERFACE_INCLUDE_DIRECTORIES)
            get_target_property(pcre_posix_INTERFACE_INCLUDE_DIRECTORIES pcre_posix INTERFACE_INCLUDE_DIRECTORIES)

            set(INCLUDE_DIRECTORIES ${INCLUDE_DIRECTORIES} ${pcre_INTERFACE_INCLUDE_DIRECTORIES})
            set(INCLUDE_DIRECTORIES ${INCLUDE_DIRECTORIES} ${pcre_posix_INTERFACE_INCLUDE_DIRECTORIES})

            set(DEPENDENCIES ${DEPENDENCIES} pcre pcre_posix)

            message(STATUS "")
            message(STATUS "PCRE has been imported from ${BREW_PCRE_PREFIX} for architecture ${ARCHITECTURE}")
            message(STATUS "    - ${pcre_INTERFACE_INCLUDE_DIRECTORIES}")
            message(STATUS "    - ${pcre_posix_INTERFACE_INCLUDE_DIRECTORIES}")
            message(STATUS "")

        else()
            message(STATUS "Unable to find PCRE in this machine for architecture ${ARCHITECTURE}")

        endif()

    else()
        # Linux
        set(ADDITIONAL_LD_FLAGS ${ADDITIONAL_LD_FLAGS} "-lpcre")

    endif()

endif()

