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

FROM eclipse-temurin:17 AS server
WORKDIR /app
COPY --from=build /app/target/mindmap.jar /app/target/mindmap.jar
COPY public /app/public
ENTRYPOINT ["java", "-jar", "/app/target/mindmap.jar", "mindmap.Server", "/data"]

FROM eclipse-temurin:17 AS grapher
WORKDIR /app
COPY --from=build /app/target/mindmap.jar /app/target/mindmap.jar
COPY public /app/public
ENTRYPOINT ["java", "-jar", "/app/target/mindmap.jar", "mindmap.Grapher", "/data", "/generated"]

FROM ruby:3.1-buster as generator-build
WORKDIR /app
RUN gem install bundler
COPY jekyll/Gemfile* /app/
RUN bundle install

FROM ruby:3.1-buster as generator
WORKDIR /app
COPY --from=generator-build /usr/local/bundle /usr/local/bundle
COPY jekyll /app
COPY jekyll/_layouts jekyll/_includes /data/
ENTRYPOINT ["bundle", "exec", "jekyll", "build", "--config", "_config.yml", "--watch", "--incremental", "-s", "/data", "-d", "/generated/_site"]
