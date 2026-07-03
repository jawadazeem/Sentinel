/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

#include "LinuxTelemetry.hpp"
#include "OpenCVFrameProvider.hpp"
#include "HttpController.hpp"
#include <chrono>
#include <thread>
#include <iostream>
#include <spdlog/spdlog.h>

int main() {
    spdlog::info("Initializing Automated Alarm System Node...");

    avisos::hardware_interface::LinuxTelemetry telemetry_provider;
    avisos::hardware_interface::OpenCVFrameProvider frame_provider(0); // 0 = Default camera

    avisos::controller::HttpController http_server(telemetry_provider, frame_provider);

    frame_provider.startStreaming();
    http_server.start(8084);

    spdlog::info("Edge node successfully running. Press Ctrl+C to terminate.");

    while (true) {
        telemetry_provider.updateMetrics(); // Pull fresh CPU temp and Wi-Fi link data
        
        std::this_thread::sleep_for(std::chrono::seconds(1));
    }

    http_server.stop();
    frame_provider.stopStreaming();
    
    return 0;
}
