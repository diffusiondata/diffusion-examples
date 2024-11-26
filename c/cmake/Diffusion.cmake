add_library(diffusion SHARED IMPORTED)

cmake_path(SET DIFFUSION_INCLUDE_PATH ${DIFFUSION_ROOT_FOLDER}/include)

if(WIN32)
    # Windows (32-bit or 64-bit)
    if(CMAKE_SIZEOF_VOID_P MATCHES 8)
        set(ARCHITECTURE "x64")
    elseif(CMAKE_SIZEOF_VOID_P MATCHES 4)
        set(ARCHITECTURE "x86")
    endif()

    cmake_path(SET DIFFUSION_LIB_PATH ${DIFFUSION_ROOT_FOLDER}/lib/${ARCHITECTURE}/uci.lib)

    set_target_properties(diffusion PROPERTIES
        IMPORTED_IMPLIB ${DIFFUSION_LIB_PATH}
        IMPORTED_LOCATION ${DIFFUSION_LIB_PATH}
        INTERFACE_INCLUDE_DIRECTORIES ${DIFFUSION_INCLUDE_PATH}
    )

elseif(UNIX)
    # Unix (MacOS or Linux)
    cmake_path(SET DIFFUSION_LIB_PATH ${DIFFUSION_ROOT_FOLDER}/lib/${ARCHITECTURE}/libdiffusion.a)

    set_target_properties(diffusion PROPERTIES
        IMPORTED_LOCATION ${DIFFUSION_LIB_PATH}
        INTERFACE_INCLUDE_DIRECTORIES ${DIFFUSION_INCLUDE_PATH}
    )
endif()

get_target_property(diffusion_INCLUDE_DIRECTORIES diffusion INTERFACE_INCLUDE_DIRECTORIES)

set(INCLUDE_DIRECTORIES ${INCLUDE_DIRECTORIES} ${diffusion_INCLUDE_DIRECTORIES})

set(DEPENDENCIES ${DEPENDENCIES} diffusion)

# list(APPEND WINDOWS_PATH ${DIFFUSION_LIB_PATH})

message("")
message("Diffusion has been imported from ${DIFFUSION_ROOT_FOLDER}")
message("    - ${diffusion_INCLUDE_DIRECTORIES}")
message("")

