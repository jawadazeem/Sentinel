/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

#include "LinuxTelemetry.hpp"
#include <fstream>
#include <iostream>
#include <mutex>
#include <string>
#include <chrono>
#include <cstdio>

namespace avisos {
    namespace hardware_interface {
        void LinuxTelemetry::inspectSnapshot(std::function<void(const Snapshot&)> reader) const {
            std::lock_guard<std::mutex> lock(telemetry_mutex_);
            reader(latest_snapshot_);
        }

        /**
         * First writes to stack variables so we aren't blocking on IO
         */
        void LinuxTelemetry::updateMetrics() {
            int8_t wifiSignalQuality = readWifiSignalQuality();
            float cpuTemp = readCpuTemperature();
            int8_t batLevel = readBatteryLevel();
            auto currentTime = std::chrono::system_clock::now();

            {
                std::lock_guard<std::mutex> lock(telemetry_mutex_);
                latest_snapshot_.signal_quality_percent_ = wifiSignalQuality;
                latest_snapshot_.temperature_celsius_ = cpuTemp;
                latest_snapshot_.battery_percent_ = batLevel;
                latest_snapshot_.timestamp_ = currentTime;
            }
        }

        /**
         * It would be better to load the file into a string buffer and extract the text in
         * the current process. Currently, each time a bash command runs, the system forks
         * the current process and spawns a shell environment to execute the command. This
         * approach is significantly slower than the alternative described above.
         */
        float LinuxTelemetry::readCpuTemperature() const {
            const std::string command = "cat /sys/class/thermal/thermal_zone0/temp | awk '{print $1 / 1000}'";
            std::string cpuTemperatureOutputted;
            char buffer[128];

            FILE* pipe = popen(command.c_str(), "r");
            if (!pipe) {
                std::cerr << "Failed to open pipe for reading CPU temperature.\n";
                return -1;
            }

            while (fgets(buffer, sizeof(buffer), pipe) != nullptr) {
                cpuTemperatureOutputted += buffer;
            }

            int returnCode = pclose(pipe);
            std::cout << "CPU Temperature (°C):\n" << cpuTemperatureOutputted;

            return std::stof(cpuTemperatureOutputted);
        }

        /** 
         * Measured as (Link Quality) / 70.
         * For instance, a current link quality of 32 out of 70 means the WiFi
         * signal quality is at 45%
         */
        int8_t LinuxTelemetry::readWifiSignalQuality() const {
            const std::string command = "awk \'/wlo1:/ {print int($3 * 100 / 70)}' /proc/net/wireless";
            std::string signalQualityOutputted;
            char buffer[128];

            FILE* pipe = popen(command.c_str(), "r");
            if (!pipe) {
                std::cerr << "Failed to open pipe for reading WiFI signal quality.\n";
                return -1;
            }

            while (fgets(buffer, sizeof(buffer), pipe) != nullptr) {
                signalQualityOutputted += buffer;
            }

            int returnCode = pclose(pipe);
            std::cout << "WiFi Signal Quality:\n" << signalQualityOutputted;

            return static_cast<int8_t>(std::stoi(signalQualityOutputted));
        }

        int8_t LinuxTelemetry::readBatteryLevel() const {
            const std::string path = "/sys/class/power_supply/BAT0/capacity";
            std::ifstream file(path);

            if (!file.is_open()) {
                std::cerr << "Failed to open " << path << '\n';
                return -1;
            }

            int capacity;
            file >> capacity;
            
            return static_cast<int8_t>(capacity);
        }
    }
}