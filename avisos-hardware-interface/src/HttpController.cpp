/* 
 * (C) Copyright 2026 Jawad Azeem 
 * Apache 2.0 License 
 */

#include "HttpController.hpp"
#include <exception>
#include <vector>
#include <ctime>
#include <httplib.h>
#include <nlohmann/json.hpp>
#include <spdlog/spdlog.h>

namespace avisos {
    namespace controller {

        HttpController::HttpController(
            hardware_interface::ITelemetryProvider& telemetry_provider_param,
            hardware_interface::IFrameProvider& frame_provider_param
        ) : telemetry_provider_(telemetry_provider_param), frame_provider_(frame_provider_param) {}

        void HttpController::start(int port) {
            server_thread_ = std::thread([this, port]() {
                httplib::Server server;

                server.Get("/readings", [this](const httplib::Request&, httplib::Response& res) {
                    spdlog::info("Called /readings");
                    
                    // Fix 2: Wrapped inside the thread-safe inspect lock block
                    telemetry_provider_.inspectSnapshot([&res](const ::avisos::hardware_interface::Snapshot& snapshot) {
                        auto time_t_format = std::chrono::system_clock::to_time_t(snapshot.timestamp_);
                        char time_buffer[32];
                        std::strftime(time_buffer, sizeof(time_buffer), "%Y-%m-%dT%H:%M:%SZ", std::gmtime(&time_t_format));

                        nlohmann::json j;
                        j["batteryPercent"] = snapshot.battery_percent_;
                        j["temperatureCelsius"] = snapshot.temperature_celsius_;
                        j["pressureKpa"] = snapshot.pressure_kpa_;
                        j["humidityPercent"] = snapshot.humidity_percent_;
                        j["leakDetected"] = snapshot.leak_detected_;
                        j["signalQualityPercent"] = snapshot.signal_quality_percent_;
                        j["timestamp"] = std::string(time_buffer);

                        res.set_content(j.dump(), "application/json");
                    });
                });

                server.Get("/frame", [this](const httplib::Request&, httplib::Response& res) {
                    spdlog::info("Called /frame");
                    try {
                        const auto& bytes = frame_provider_.pickFrame();
                        res.set_content(reinterpret_cast<const char*>(bytes.data()), bytes.size(), "image/png");
                    } catch (const std::exception& e) {
                        res.status = 404;
                        res.set_content(
                            R"({"error":"no frames loaded. Please try again in a few seconds"})",
                            "application/json"
                        );
                        spdlog::warn("Frame request failed: {}", e.what());
                    }
                });

                server.Get("/health", [](const httplib::Request&, httplib::Response& res) {
                    res.set_content(R"({"status":"UP"})", "application/json");
                });

                spdlog::info("HTTP server listening on port {}", port);
                server.listen("0.0.0.0", port);
            });
        }

        void HttpController::stop() {
            if (server_thread_.joinable()) {
                server_thread_.join();
            }
        }

    }
}