# Multi-stage build. Context is the monorepo root so the Maven wrapper and all
# module POMs are available for a reactor build of this single service.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Copy the wrapper and every POM first so dependency resolution is cached
# independently of source changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY document-service/pom.xml document-service/pom.xml
COPY indexing-service/pom.xml indexing-service/pom.xml
COPY rag-service/pom.xml rag-service/pom.xml
RUN chmod +x mvnw && ./mvnw -B -q -pl document-service -am dependency:go-offline

COPY document-service/src document-service/src
RUN ./mvnw -B -q -pl document-service -am clean package -DskipTests

FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app
RUN useradd --system --uid 10001 spring \
    && mkdir -p /var/lib/knowledge-platform/storage \
    && chown -R spring /var/lib/knowledge-platform
COPY --from=build /workspace/document-service/target/*.jar app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
