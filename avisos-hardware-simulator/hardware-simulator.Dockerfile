# Stage 1: Build the C++ binary
FROM alpine:3.18 AS builder
RUN apk add --no-cache g++ make cmake git boost-dev spdlog-dev

WORKDIR /app
COPY . .

RUN cmake -S . -B build -DCMAKE_BUILD_TYPE=Release && \
    cmake --build build

# Stage 2: Run the binary in an ultra-lean container
FROM alpine:3.18
RUN apk add --no-cache libstdc++ spdlog fmt

WORKDIR /root/
COPY --from=builder /app/build/avisos_hardware_simulator hardware_simulator
COPY resources/frames resources/frames

CMD ["./hardware_simulator"]
