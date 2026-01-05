# Sentinel Security Hub
#### Author: Jawad Azeem
#### Project Documentation: www.jawadazeem.com/sentinel
A robust, command-based security orchestration system designed to manage a fleet of IoT sensors (Motion, Smoke, Thermal, etc.). Sentinel ensures strict enforcement of security protocols through centralized state management and defensive programming.

## Architectural Design

The system implements several high-level design patterns to solve the complexities of IoT device management:

### 1. Command Pattern

Every system action is encapsulated as an object. This allows the `SecurityHub` to queue, prioritize, and audit actions without needing to know the internal execution details of specific tasks.

### 2. Centralized Gatekeeping (The Hub)

Logic for command validation resides exclusively within the `SecurityHub`. By centralizing the "Security Policy," the system prevents commands from being executed on `FAULTY` or non-operational hardware, ensuring the integrity of the security perimeter.

### 3. Open-Closed Principle

The system utilizes a category-based command structure. The `SecurityHub` can distinguish between "System Commands" (Diagnostics, Resets) and "Operational Commands" (Alarms) through the `Command` interface, allowing the system to be extended with new command types without modifying the core Hub logic.

---

## Key Components

### Core

* **SecurityHub**: The central orchestrator and singleton instance. It manages the task queue, device registry, and global arming status.
* **HubStatus**: Defines the operational modes of the hub (ARMED, DISARMED).

### Devices

* **Device Interface**: Defines the contract for all hardware sensors, including health checks and subscriber management.
* **BaseDevice**: An abstract layer providing shared state logic (failure counts, status tracking) to minimize code duplication across sensor types.
* **Motion/Smoke Devices**: Concrete implementations containing hardware-specific logic.

### Commands

* **SystemDiagnosticCommand**: Triggers device self-checks and health reporting.
* **SystemResetCommand**: A privileged command that bypasses standard status checks to recover faulty devices.
* **TriggerAlarm/Panic**: Operational commands for notifying subscribers of security events.

---

## Technical Features

* **Priority Processing**: `PanicCommand` utilizes a Double-Ended Queue (Deque) to bypass the standard processing order, ensuring life-safety events are prioritized.
* **Defensive Programming**: The Hub implements null-safety checks and status validation to prevent runtime exceptions during command dispatching.
* **Subscriber Model**: Implements the Observer pattern to decouple sensors from notification services like the Police Station or Mobile Applications.

---

## Project Structure

```text
src/
└── sentinel/
    ├── core/          # Central Hub and State Management
    ├── commands/      # Command Interface and Implementations
    ├── devices/       # Device Interfaces and Hardware Models
    ├── factory/       # Creational Logic for Sensor Provisioning
    ├── logger/        # Multi-channel Logging Infrastructure
    └── observers/     # Alert Subscribers and Notification Links

```# Sentinel
# Sentinel
