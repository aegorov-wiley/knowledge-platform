# Multi-stage build. Context is the monorepo root so the Maven wrapper and all
# module POMs are available for a reactor build of this single service.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY document-service/pom.xml document-service/pom.xml
COPY indexing-service/pom.xml indexing-service/pom.xml
COPY rag-service/pom.xml rag-service/pom.xml
RUN chmod +x mvnw && ./mvnw -B -q -pl rag-service -am dependency:go-offline

COPY rag-service/src rag-service/src
RUN ./mvnw -B -q -pl rag-service -am clean package -DskipTests

FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app
RUN useradd --system --uid 10001 spring
COPY --from=build /workspace/rag-service/target/*.jar app.jar
USER spring
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
