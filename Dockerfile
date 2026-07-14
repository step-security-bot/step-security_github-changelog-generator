FROM gradle:8.5.0-jdk17-alpine@sha256:e4f3af5b65fb20115bb8d2054fea4d37e0389aa003735ca6d7b035c9c0d9fb70 as build
COPY src /app/src/
COPY config /app/config/
COPY build.gradle settings.gradle gradle.properties /app/
RUN cd /app && gradle -Dorg.gradle.welcome=never --no-daemon bootJar

FROM ghcr.io/bell-sw/liberica-openjre-debian:17.0.10-13@sha256:645e99e736950dec7c37994ebd5d0d9f26b7f62ed8af7310b040872a69e84f19
COPY --from=build /app/build/libs/github-changelog-generator.jar /opt/action/github-changelog-generator.jar
ENTRYPOINT ["java", "-jar", "/opt/action/github-changelog-generator.jar"]
