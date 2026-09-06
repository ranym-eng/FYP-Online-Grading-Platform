FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -Dmaven.test.skip=true -DskipTestDependencies package

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache su-exec \
    && addgroup -S spring \
    && adduser -S spring -G spring \
    && mkdir -p /app/generated \
    && chown -R spring:spring /app/generated
WORKDIR /app
COPY --from=build /workspace/target/fyp-grading-platform-*.jar app.jar
COPY docker-entrypoint.sh /usr/local/bin/fyp-entrypoint
RUN chmod 755 /usr/local/bin/fyp-entrypoint

EXPOSE 8080

ENTRYPOINT ["/usr/local/bin/fyp-entrypoint"]
