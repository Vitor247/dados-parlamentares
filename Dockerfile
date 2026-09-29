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

# JVM ajustada para instâncias pequenas (hospedagem gratuita: 512MB e 0,1 CPU):
# - MaxRAMPercentage: heap proporcional à memória do container, com folga para o resto da JVM
# - SerialGC: coletor mais leve, adequado a um único núcleo
# - TieredStopAtLevel=1: menos compilação JIT na partida (a partida importa mais que o pico aqui)
# Medido com --memory=512m --cpus=0.1: partida de 159 s para 71 s, memória de 310 para 280 MB.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"
# Mesmo fuso usado pelo Hibernate (application.yml). O banco já é gravado nesse fuso,
# mas sem isso o container roda em UTC e os horários que não passam pelo banco
# (resumo da ingestão, timestamp dos erros, logs) sairiam 3h à frente.
ENV TZ=America/Sao_Paulo

# CDS (Class Data Sharing): uma execução de treino durante o build grava as classes já
# processadas em application.jsa, e a JVM de produção as carrega prontas. O treino sobe o
# contexto do Spring e sai (spring.context.exit=onRefresh) SEM banco: Flyway desligado e
# Hibernate sem consultar metadados JDBC.
RUN java -XX:ArchiveClassesAtExit=application.jsa -Dspring.context.exit=onRefresh \
      -jar parlamento-0.0.1-SNAPSHOT.jar \
      --spring.flyway.enabled=false \
      --spring.jpa.hibernate.ddl-auto=none \
      --spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect \
      --spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-XX:SharedArchiveFile=application.jsa", "-jar", "parlamento-0.0.1-SNAPSHOT.jar"]
