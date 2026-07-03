/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

#pragma once
#include <chrono>
#include <cstdint>

namespace avisos {
  namespace hardware_interface {
    struct Snapshot {
        Snapshot() : timestamp_(std::chrono::system_clock::now()) {}

        Snapshot(int8_t battery, float temp, float pressure, float humidity, 
                bool leak, int8_t signal, const std::chrono::system_clock::time_point& time)
            : battery_percent_(battery),
              temperature_celsius_(temp),
              pressure_kpa_(pressure),
              humidity_percent_(humidity),
              leak_detected_(leak),
              signal_quality_percent_(signal),
              timestamp_(time) {}

        Snapshot(int8_t battery, float temperature, int8_t signal) 
        : battery_percent_(battery),
          temperature_celsius_(temperature),
          signal_quality_percent_(signal),
          timestamp_(std::chrono::system_clock::now()) {}

        /**
         * This constructor will be used by the Pavilion Node since it does not
         * come standard with a battery, barometer, humidity sensor, or water sensor.
         */
        Snapshot(float temperature, int8_t signal) 
        : temperature_celsius_(temperature),
          signal_quality_percent_(signal),
          timestamp_(std::chrono::system_clock::now()) {}

        int8_t battery_percent_ = -1; // -1 signifies an unread or missing battery
        float temperature_celsius_ = 0.0f;
        float pressure_kpa_ = 101.325f;
        float humidity_percent_ = 55.0f;
        bool leak_detected_ = false;
        int8_t signal_quality_percent_ = 0; // Default to no connection

        std::chrono::system_clock::time_point timestamp_;
    };
  }
}

