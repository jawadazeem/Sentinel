# Build everything first so common-lib is ready
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /app
COPY . .
RUN mvn clean install

# Only take the Node Service into the final container
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /app/avisos-node-service/target/node-service.jar node-service.jar
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "node-service.jar"]
