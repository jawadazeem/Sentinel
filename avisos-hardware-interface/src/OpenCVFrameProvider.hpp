/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

#pragma once
#include <cstdint>
#include <vector>
#include "IFrameProvider.hpp"
#include <opencv2/opencv.hpp>

namespace avisos {
    namespace hardware_interface {
        class OpenCVFrameProvider : public IFrameProvider {
            public:
                OpenCVFrameProvider(int camera_index_);
                ~OpenCVFrameProvider() override = default;
                void startStreaming() override;
                const std::vector<uint8_t>& pickFrame() override;
                void stopStreaming() override;
            
            private:
                int camera_index_ = 0; // usually 0 for the default camera
                std::vector<uint8_t> frame_buffer_;
                cv::VideoCapture video_capture_;
                bool getFirstFrameLoaded();
                bool is_first_frame_loaded_ = true; // true by default
        };
    }
}