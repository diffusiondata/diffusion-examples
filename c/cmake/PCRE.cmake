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

    message("")
    message("PCRE has been imported from ${PCRE_ROOT_PATH}")
    message("    - ${pcre_INTERFACE_INCLUDE_DIRECTORIES}")
    message("    - ${pcre_posix_INTERFACE_INCLUDE_DIRECTORIES}")
    message("")

elseif(UNIX)
    # Unix (MacOS or Linux)
    set(ADDITIONAL_LD_FLAGS ${ADDITIONAL_LD_FLAGS} "-lpcre")

endif()

