FROM eclipse-temurin:17

# Setup dependencies
RUN apt-get install -y curl
RUN curl -fL https://github.com/coursier/launchers/raw/master/cs-x86_64-pc-linux.gz | gzip -d > cs && chmod +x cs
RUN ./cs setup -y

# Setup code
COPY . ./

# Build application
RUN ./cs launch sbt -- assembly
