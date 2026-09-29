# Build em duas etapas: a imagem final nao carrega o Gradle nem o codigo-fonte.
FROM eclipse-temurin:21-jdk AS construcao
WORKDIR /construcao
COPY gradle gradle
COPY gradlew settings.gradle.kts build.gradle.kts ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
# Nao roda como root.
RUN useradd --system --create-home medshare
USER medshare
COPY --from=construcao /construcao/build/libs/*.jar aplicacao.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/aplicacao.jar"]
