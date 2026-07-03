/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

#pragma once
#include <vector>
#include <cstdint>

namespace avisos {
    namespace hardware_interface {
        class IFrameProvider {
            public:
                virtual ~IFrameProvider() = default;

                virtual const std::vector<uint8_t>& pickFrame() = 0;
                virtual void startStreaming() = 0;
                virtual void stopStreaming() = 0;
        };
    }
}