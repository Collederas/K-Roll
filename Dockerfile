# STAGE 1: Builder
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace

ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.jvmargs=-Xmx1024m"

COPY gradle/ gradle/
COPY gradlew build.gradle.kts settings.gradle.kts ./

RUN ./gradlew dependencies --console=plain

COPY src/ src/

RUN ./gradlew bootJar -x test --console=plain
RUN java -Djarmode=layertools -jar build/libs/*.jar extract

# STAGE 2: Runtime
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S kroll && adduser -S kroll -G kroll
VOLUME /tmp
WORKDIR /app

COPY --from=builder /workspace/dependencies/ ./
COPY --from=builder /workspace/spring-boot-loader/ ./
COPY --from=builder /workspace/snapshot-dependencies/ ./
COPY --from=builder /workspace/application/ ./

USER kroll
EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
