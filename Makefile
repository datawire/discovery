# Makefile: discovery

VERSION=$(shell cat VERSION)

.PHONY: all

all: clean build

build:
	# Produces a language build artifact (e.g.: .jar, .whl, .gem).
	./gradlew build test shadowJar
 
docker: build
	# Produces a Docker image.
	docker build -t datawire/discovery:$(VERSION) .

docker-no-jar-build:
	# Produces a Docker image but do not trigger recompilation and packaging of the source.
	docker build -t datawire/discovery:$(VERSION) .

clean:
	# Clean previous build outputs (e.g. class files) and temporary files.
	./gradlew clean
 
compile:
	# Compile code (may do nothing for interpreted languages).
	./gradlew build

run-docker: docker
	# Run the service or application in docker.
	( \
		docker run -it --rm --name datawire-discovery \
		-p 5000:5000 \
		-v $$(pwd)/discovery-web/config:/opt/discovery/config \
		datawire/discovery:$(VERSION) \
	)

run-docker-no-jar-rebuild:
	# Run the service or application in docker.
	( \
		docker run -it --rm --name datawire-discovery \
		-p 5000:5000 \
		-v $$(pwd)/discovery-web/config:/opt/discovery/config \
		datawire/discovery:$(VERSION) \
	)
 
test:
	./gradlew test
 
unit-test:
	./gradlew test

version:
	@echo VERSION