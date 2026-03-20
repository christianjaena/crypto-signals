FROM eclipse-temurin:25-jdk

WORKDIR /app
COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-Xmx256m","-Xms128m","-jar","/app/app.jar"]