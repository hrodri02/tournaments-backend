# Build context is the repository root, not mcp-server/, so this image can reuse
# the Maven wrapper that lives there:
#
#   build:
#     context: ../../
#     dockerfile: mcp-server/Dockerfile
FROM eclipse-temurin:25.0.2_10-jdk-jammy AS builder
WORKDIR /opt/app
COPY .mvn/ .mvn
COPY mvnw ./
COPY mcp-server/pom.xml ./pom.xml
RUN ./mvnw dependency:go-offline
COPY mcp-server/src ./src
RUN ./mvnw clean install -DskipTests

FROM eclipse-temurin:25.0.2_10-jre-jammy AS final
WORKDIR /opt/app
EXPOSE 8080
COPY --from=builder /opt/app/target/*.jar /opt/app/*.jar
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/opt/app/*.jar"]
