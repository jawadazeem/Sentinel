/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

#include "OpenCVFrameProvider.hpp"
#include <stdexcept>

namespace avisos {
    namespace hardware_interface {
        OpenCVFrameProvider::OpenCVFrameProvider(int camera_index) 
            : camera_index_(camera_index) {
        }

        void OpenCVFrameProvider::startStreaming() {
            video_capture_.open(camera_index_, cv::CAP_V4L2);
        }

        void OpenCVFrameProvider::stopStreaming() {
            if (video_capture_.isOpened()) {
                video_capture_.release();    
            }
            
            return;
        }

        const std::vector<uint8_t>& OpenCVFrameProvider::pickFrame() {
            if (video_capture_.isOpened()) {
                cv::Mat frame;
                video_capture_ >> frame;
                
                if (frame.empty() && getFirstFrameLoaded()) {
                    return frame_buffer_;
                }
                
                frame_buffer_.clear();
                cv::imencode(".png", frame, frame_buffer_);
                is_first_frame_loaded_ = true;
                return frame_buffer_;
            }

            throw std::runtime_error(
                "Cannot pick frame! No frames are in the buffer and video streaming hasn't started yet."
            );
        }

        bool OpenCVFrameProvider::getFirstFrameLoaded() {
            return is_first_frame_loaded_;
        }
    }
}