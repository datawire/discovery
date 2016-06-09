all: quark

.ALWAYS:

quark: .ALWAYS
	quark install --python quark/discovery-2.0.0.q quark/intro.q

docker:
	./gradlew clean build :discovery-web:buildDockerImage

discoball:
	docker run --name disco -p 52689:52689 \
		-v $$(pwd)/discovery-web/config:/opt/discovery/config datawire/discovery:2.0.0

clean:
	./gradlew clean
