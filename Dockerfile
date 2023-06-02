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

FROM alpine:3.18 AS npmbuild
RUN apk --no-cache add nodejs npm
WORKDIR /scripts
COPY public/assets/scripts /s
RUN npm install terser
RUN npm run terser public/assets/scripts/network.js /scripts/network.min.js
RUN npm run terser public/assets/scripts/sub-network.js /scripts/sub-network.min.js

FROM eclipse-temurin:17 AS server
WORKDIR /app
COPY --from=build /app/target/mindmap.jar /app/target/mindmap.jar
COPY --from=npmbuild /scripts/network.min.js /app/public/assets/scripts/network.min.js
COPY --from=npmbuild /scripts/sub-network.min.js /app/public/assets/scripts/sub-network.min.js
COPY public /app/public
ENTRYPOINT ["java", "-jar", "/app/target/mindmap.jar", "--class", "mindmap.Server", "--", "--path", "/data"]

FROM eclipse-temurin:17 AS grapher
WORKDIR /app
COPY --from=build /app/target/mindmap.jar /app/target/mindmap.jar
COPY public /app/public
ENTRYPOINT ["java", "-jar", "/app/target/mindmap.jar", "--class", "mindmap.Grapher", "--", "--collection-path", "/data", "--network-path", "/generated"]
