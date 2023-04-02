FROM eclipse-temurin:17 AS build
WORKDIR /dep
# Setup dependencies
RUN apt-get install -y curl
RUN curl -fL https://github.com/coursier/launchers/raw/master/cs-x86_64-pc-linux.gz | gzip -d > cs && chmod +x cs
RUN /dep/cs setup -y
# Setup code
WORKDIR /app
COPY src /app/src
COPY build.sbt /app/build.sbt
COPY project /app/project
# Build application
RUN /dep/cs launch sbt -- assembly

FROM eclipse-temurin:17 AS run
WORKDIR /app
COPY --from=build /app/target/mindmap.jar /app/target/mindmap.jar
ENTRYPOINT ["java", "-jar", "/app/target/mindmap.jar"]
