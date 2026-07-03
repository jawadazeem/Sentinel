/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

#pragma once
#include "Snapshot.hpp"
#include <functional>

namespace avisos {
    namespace hardware_interface {
        class ITelemetryProvider {
            public:
                virtual ~ITelemetryProvider() = default;
                virtual void inspectSnapshot(std::function<void(const Snapshot&)> reader) const = 0;
        };
    }
}