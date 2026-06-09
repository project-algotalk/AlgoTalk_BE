# interviewService
FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-Dspring.profiles.active=prod","-Duser.timezone=Asia/Seoul","-jar","/app.jar"]