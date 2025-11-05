FROM amazoncorretto:25-alpine AS builder
WORKDIR application
ARG JAR_FILE=target/openleap-gateway-exec.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM amazoncorretto:25-alpine
WORKDIR application
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/application/ ./
ENTRYPOINT exec java org.springframework.boot.loader.launch.JarLauncher
