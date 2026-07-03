/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

#pragma once
#include <thread>
#include "IFrameProvider.hpp"
#include "ITelemetryProvider.hpp"

namespace avisos {
    namespace controller {
        class HttpController {
            public:
                HttpController(
                    hardware_interface::ITelemetryProvider& telemetry_provider_param,
                    hardware_interface::IFrameProvider& frame_provider_param
                );

                void start(int port);
                void stop();
            
            private:
                hardware_interface::ITelemetryProvider& telemetry_provider_;
                hardware_interface::IFrameProvider& frame_provider_;
                std::thread server_thread_;
        };
    }
}