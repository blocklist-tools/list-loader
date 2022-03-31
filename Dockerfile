FROM maven:3.8-openjdk-17-slim as mvn
COPY . /opt/list-loader
WORKDIR "/opt/list-loader/"
RUN mvn clean package \
    && ls /opt/list-loader/target/ \
    && mv /opt/list-loader/target/blocklist-loader*shaded.jar /opt/list-loader/app.jar


FROM openjdk:17-jdk-bullseye
ARG JAR_FILE=target/*.jar
COPY --from=mvn /opt/list-loader/app.jar /opt/list-loader/app.jar
WORKDIR "/opt/list-loader/"
ENTRYPOINT ["java","-jar","app.jar"]
