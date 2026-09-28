# ---------- build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build

# Dependências primeiro: esta camada só é refeita quando o pom.xml muda.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Garante LF e permissão de execução mesmo se o checkout veio do Windows.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw \
    && ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Os testes precisam de Docker (Testcontainers) e rodam fora da imagem: ./mvnw test
RUN ./mvnw -B -q -DskipTests package \
    && java -Djarmode=tools -jar target/parlamento-*.jar extract --layers --destination extraido

# ---------- runtime ----------
# Alpine: a aplicação ocupa ~60MB; a base Ubuntu sozinha adicionaria ~340MB.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S -G app app

# Camadas do Spring Boot, da que menos muda para a que mais muda:
# a cada mudança de código só a última é reenviada.
COPY --from=build /build/extraido/dependencies/ ./
COPY --from=build /build/extraido/spring-boot-loader/ ./
COPY --from=build /build/extraido/snapshot-dependencies/ ./
COPY --from=build /build/extraido/application/ ./

USER app
EXPOSE 8080

# Limita o heap a uma fração da memória do container, em vez da memória da máquina.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
# Mesmo fuso usado pelo Hibernate (application.yml). O banco já é gravado nesse fuso,
# mas sem isso o container roda em UTC e os horários que não passam pelo banco
# (resumo da ingestão, timestamp dos erros, logs) sairiam 3h à frente.
ENV TZ=America/Sao_Paulo

ENTRYPOINT ["java", "-jar", "parlamento-0.0.1-SNAPSHOT.jar"]
