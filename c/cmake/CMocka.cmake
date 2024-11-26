add_library(cmocka-static STATIC IMPORTED)

if(WIN32)
    if(CMAKE_SIZEOF_VOID_P MATCHES 8)
        set(ARCHITECTURE "x64")
    elseif(CMAKE_SIZEOF_VOID_P MATCHES 4)
        set(ARCHITECTURE "x86")
    endif()

    cmake_path(SET CMOCKA_PATH ${CMOCKA_ROOT_PATH}/${ARCHITECTURE})
    cmake_path(SET CMOCKA_LIB_PATH ${CMOCKA_PATH}/lib/cmocka.lib)
    cmake_path(SET CMOCKA_BIN_PATH ${CMOCKA_PATH}/bin)
    cmake_path(SET CMOCKA_DLL_PATH ${CMOCKA_BIN_PATH}/cmocka.dll)
    cmake_path(SET CMOCKA_INCLUDE_PATH ${CMOCKA_PATH}/include)

    set_target_properties(cmocka-static PROPERTIES
        IMPORTED_IMPLIB ${CMOCKA_LIB_PATH}
        IMPORTED_LOCATION ${CMOCKA_LIB_PATH}
        INTERFACE_INCLUDE_DIRECTORIES ${CMOCKA_INCLUDE_PATH}
    )

    # Cmocka is a dynamic library: the .lib looks internally for the .dll
    # Link the executable to the folder that contains the DLL for cmocka
    set(TEST_WORKING_DIRECTORY ${CMOCKA_BIN_PATH})

elseif(UNIX)
    cmake_path(SET CMOCKA_LIB_PATH ${CMOCKA_ROOT_PATH}/lib/${ARCHITECTURE}/libcmocka.a)
    cmake_path(SET CMOCKA_INCLUDE_PATH ${CMOCKA_ROOT_PATH}/include)

    set_target_properties(cmocka-static PROPERTIES
        IMPORTED_IMPLIB ${CMOCKA_LIB_PATH}
        IMPORTED_LOCATION ${CMOCKA_LIB_PATH}
        INTERFACE_INCLUDE_DIRECTORIES ${CMOCKA_INCLUDE_PATH}
    )

    set(TEST_WORKING_DIRECTORY ${CMAKE_RUNTIME_OUTPUT_DIRECTORY})
endif()

get_target_property(cmocka_INCLUDE_DIRECTORIES cmocka-static INTERFACE_INCLUDE_DIRECTORIES)

set(INCLUDE_DIRECTORIES ${INCLUDE_DIRECTORIES} ${cmocka_INCLUDE_DIRECTORIES})

set(DEPENDENCIES ${DEPENDENCIES} cmocka-static)

list(APPEND WINDOWS_PATH ${CMOCKA_DLL_PATH})

message("")
message("CMocka has been imported from ${CMOCKA_ROOT_PATH}")
message("    - ${cmocka_INCLUDE_DIRECTORIES}")
message("")
