FROM maven:3.6-openjdk-14 as mvn
COPY . /opt/list-loader
WORKDIR "/opt/list-loader/"
RUN mvn clean package \
    && ls /opt/list-loader/target/ \
    && mv /opt/list-loader/target/blocklist-loader*shaded.jar /opt/list-loader/app.jar


FROM openjdk:14-jdk-buster
ARG JAR_FILE=target/*.jar
COPY --from=mvn /opt/list-loader/app.jar /opt/list-loader/app.jar
WORKDIR "/opt/list-loader/"
ENTRYPOINT ["java","-jar","app.jar"]