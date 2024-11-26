cmake_minimum_required(VERSION 3.16)

project(diffusion_sdk_c_examples)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_BUILD_TYPE "Release")
set(CMAKE_OSX_ARCHITECTURES "${ARCHITECTURE}")

cmake_path(SET DEPENDENCIES_FOLDER ${CMAKE_CURRENT_SOURCE_DIR}/dependencies)
cmake_path(SET CMAKE_SCRIPTS_FOLDER ${CMAKE_CURRENT_SOURCE_DIR}/cmake)
cmake_path(SET EXAMPLES_SOURCE_FOLDER ${CMAKE_CURRENT_SOURCE_DIR}/examples)
cmake_path(SET RESOURCES_SOURCE_FOLDER ${CMAKE_CURRENT_SOURCE_DIR}/resources)
cmake_path(SET UTILS_SOURCE_FOLDER ${EXAMPLES_SOURCE_FOLDER}/utils)

cmake_path(SET CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${TARGET}/lib/${ARCHITECTURE})
cmake_path(SET CMAKE_LIBRARY_OUTPUT_DIRECTORY ${TARGET}/bin/${ARCHITECTURE})
cmake_path(SET CMAKE_RUNTIME_OUTPUT_DIRECTORY ${TARGET}/bin/${ARCHITECTURE})

# =============================================================================
# Dependencies

list(APPEND CMAKE_MODULE_PATH "${CMAKE_SCRIPTS_FOLDER}")
set(INCLUDE_DIRECTORIES "")
set(ADDITIONAL_LD_FLAGS "")

# PThread
set(THREADS_PREFER_PTHREAD_FLAG ON)
find_package(Threads)

# APR
cmake_path(SET APR_ROOT_PATH ${DEPENDENCIES_FOLDER}/${OPERATING_SYSTEM}/apr)
include(${CMAKE_SCRIPTS_FOLDER}/APR.cmake)

# Diffusion - C SDK
cmake_path(SET DIFFUSION_ROOT_FOLDER ${TARGET}/client)
include(${CMAKE_SCRIPTS_FOLDER}/Diffusion.cmake)

# PCRE
include(${CMAKE_SCRIPTS_FOLDER}/PCRE.cmake)

# ZLib
include(${CMAKE_SCRIPTS_FOLDER}/ZLib.cmake)

# Operating System dependencies
include(${CMAKE_SCRIPTS_FOLDER}/OperatingSystem.cmake)


# =============================================================================
# Examples Support
set(INCLUDE_DIRECTORIES ${INCLUDE_DIRECTORIES} ${UTILS_SOURCE_FOLDER})


# =============================================================================
# Examples
include(${EXAMPLES_SOURCE_FOLDER}/examples.cmake)




