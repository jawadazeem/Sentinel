/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

#pragma once
#include "ITelemetryProvider.hpp"
#include <functional>
#include <mutex>
#include <cstdint>

namespace avisos {
    namespace hardware_interface {
        class LinuxTelemetry : public ITelemetryProvider {
            public:
                LinuxTelemetry() = default;
                ~LinuxTelemetry() override = default;
                void inspectSnapshot(std::function<void(const Snapshot&)> reader) const override;
                void updateMetrics();
            
            private:
                float readCpuTemperature() const;
                int8_t readWifiSignalQuality() const;
                int8_t readBatteryLevel() const;

                mutable std::mutex telemetry_mutex_;
                Snapshot latest_snapshot_;
        };
    }
}