add_library(apr STATIC IMPORTED)
add_library(apr-util STATIC IMPORTED)

if(WIN32)
    cmake_path(SET APR_PATH ${APR_ROOT_PATH}/${ARCHITECTURE})
    cmake_path(SET APR_LIB_PATH ${APR_PATH}/lib/apr.lib)
    cmake_path(SET APR_UTIL_LIB_PATH ${APR_PATH}/lib/aprutil.lib)
    cmake_path(SET APR_INCLUDE_PATH ${APR_PATH}/include)

    set_target_properties(apr PROPERTIES
        IMPORTED_IMPLIB ${APR_LIB_PATH}
        IMPORTED_LOCATION ${APR_LIB_PATH}
        INTERFACE_INCLUDE_DIRECTORIES ${APR_INCLUDE_PATH}
    )

    set_target_properties(apr-util PROPERTIES
        IMPORTED_IMPLIB ${APR_UTIL_LIB_PATH}
        IMPORTED_LOCATION ${APR_UTIL_LIB_PATH}
        INTERFACE_INCLUDE_DIRECTORIES ${APR_INCLUDE_PATH}
    )

elseif(UNIX)
    cmake_path(SET APR_LIB_PATH ${APR_ROOT_PATH}/lib/${ARCHITECTURE}/libapr.a)
    cmake_path(SET APR_UTIL_LIB_PATH ${APR_ROOT_PATH}/lib/${ARCHITECTURE}/libaprutil.a)
    cmake_path(SET APR_INCLUDE_PATH ${APR_ROOT_PATH}/include)

    set_target_properties(apr PROPERTIES
        IMPORTED_IMPLIB ${APR_LIB_PATH}
        IMPORTED_LOCATION ${APR_LIB_PATH}
        INTERFACE_INCLUDE_DIRECTORIES ${APR_INCLUDE_PATH}
    )

    set_target_properties(apr-util PROPERTIES
        IMPORTED_IMPLIB ${APR_UTIL_LIB_PATH}
        IMPORTED_LOCATION ${APR_UTIL_LIB_PATH}
        INTERFACE_INCLUDE_DIRECTORIES ${APR_INCLUDE_PATH}
    )
endif()

get_target_property(apr_INCLUDE_DIRECTORIES apr INTERFACE_INCLUDE_DIRECTORIES)

set(INCLUDE_DIRECTORIES ${INCLUDE_DIRECTORIES} ${apr_INCLUDE_DIRECTORIES})

set(DEPENDENCIES ${DEPENDENCIES} apr apr-util)

# list(APPEND WINDOWS_PATH ${APR_LIB_PATH} ${APR_UTIL_LIB_PATH})

message("")
message("APR has been imported from ${APR_ROOT_PATH}")
message("    - ${apr_INCLUDE_DIRECTORIES}")
message("")
