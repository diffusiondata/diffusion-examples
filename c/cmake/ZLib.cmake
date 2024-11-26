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

    message("")
    message("ZLIB has been imported from ${ZLIB_ROOT_PATH}")
    message("    - ${zlib_INCLUDE_DIRECTORIES}")
    message("")

elseif(UNIX)
    # Unix (MacOS or Linux)
    set(ADDITIONAL_LD_FLAGS ${ADDITIONAL_LD_FLAGS} "-lz")

endif()

