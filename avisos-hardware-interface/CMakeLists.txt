cmake_minimum_required(VERSION 3.14)

project(edge_node LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(FetchContent)

# spdlog
FetchContent_Declare(
    spdlog
    GIT_REPOSITORY https://github.com/gabime/spdlog.git
    GIT_TAG v1.15.0
)

# nlohmann/json
FetchContent_Declare(
    json
    GIT_REPOSITORY https://github.com/nlohmann/json.git
    GIT_TAG v3.11.3
)

# cpp-httplib
FetchContent_Declare(
    httplib
    GIT_REPOSITORY https://github.com/yhirose/cpp-httplib.git
    GIT_TAG v0.15.3
)

FetchContent_MakeAvailable(
    spdlog
    json
    httplib
)

find_package(OpenCV REQUIRED)

set(SOURCES
    src/main.cpp
    src/LinuxTelemetry.cpp
    src/OpenCVFrameProvider.cpp
    src/HttpController.cpp
)

add_executable(edge_node ${SOURCES})

target_include_directories(edge_node
    PRIVATE
        include
        src
)

target_link_libraries(edge_node PRIVATE
    ${OpenCV_LIBS}
    nlohmann_json::nlohmann_json
    spdlog::spdlog_header_only
    httplib
)

if(UNIX)
    target_link_libraries(edge_node PRIVATE pthread)
endif()