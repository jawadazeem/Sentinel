cmake_minimum_required(VERSION 3.14)
project(SpdlogExample LANGUAGES CXX)

# Force C++11 or newer (spdlog requires at least C++11)
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(FetchContent)
FetchContent_Declare(
    spdlog
    GIT_REPOSITORY https://github.com/gabime/spdlog.git
    GIT_TAG        v1.15.0
)
FetchContent_MakeAvailable(spdlog)
add_executable(my_program Main.cpp)
target_link_libraries(my_program PRIVATE spdlog::spdlog_header_only)