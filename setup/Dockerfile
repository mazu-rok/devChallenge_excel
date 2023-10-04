FROM maven:3.9.4-eclipse-temurin-17 AS builder

WORKDIR /workspace

COPY ./ /workspace

RUN mvn clean install -DskipTests

FROM openjdk:17 AS final

WORKDIR /workspace

COPY --from=builder /workspace/target/devChallenge.jar /workspace/devChallenge.jar

CMD java -jar ./devChallenge.jar