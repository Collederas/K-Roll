FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S kroll && adduser -S kroll -G kroll

COPY build/libs/kroll-*.jar app.jar
RUN chown -R kroll:kroll /app

USER kroll
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
