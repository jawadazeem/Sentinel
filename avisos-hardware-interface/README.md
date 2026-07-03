## Avisos Hardware Interface

- A highly optimized, ultra-lightweight application built in **C++** for deployment on resource-constrained edge devices. Designed with performance and scalability in mind, the architecture efficiently supports **thousands of concurrent edge nodes** while maintaining a minimal system footprint.

- Each edge node communicates directly with a local **Node Service Server**, where telemetry and operational data are aggregated before being securely streamed to the central **Avisos Controller** for processing, monitoring, and orchestration.

### Target Platform

- This application was purpose-built for the **Pavilion Node**, a custom hardware platform running **Ubuntu Server**, ensuring reliable performance and seamless integration within the Avisos edge infrastructure. Unlike the other application components, this runs directly on the hardware and is not containerized.