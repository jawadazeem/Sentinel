/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

#include "HttpController.hpp"
#include <exception>
#include <vector>
#include <httplib.h>
#include <nlohmann/json.hpp>
#include <spdlog/spdlog.h>

 namespace avisos {
    namespace controller {
        HttpController::HttpController(
                    hardware_interface::ITelemetryProvider& telemetry_provider_param,
                    hardware_interface::IFrameProvider& frame_provider_param
        ) : telemetry_provider_(telemetry_provider_param),
            frame_provider_(frame_provider_param) {}

        void HttpController::start(int port) {
            server_thread_ = std::thread([this, port]() {
                httplib::Server server;
                
                server.Get("/readings", [this](const httplib::Request&, httplib::Response& res) {
                    spdlog::info("Called /readings");
                    
                    telemetry_provider_.inspectSnapshot([&res](const hardware_interface::Snapshot& snapshot) {
                        nlohmann::json j;
                        j["battery_percent"] = snapshot.battery_percent_;
                        j["temperature_celsius"] = snapshot.temperature_celsius_;
                        j["pressure_kpa"] = snapshot.pressure_kpa_;
                        j["humidity_percent"] = snapshot.humidity_percent_;
                        j["leak_detected"] = snapshot.leak_detected_;
                        j["signal_quality_percent"] = snapshot.signal_quality_percent_;
                        
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
                        spdlog::info("No frames loaded yet, returning 404");
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