all: quark

.ALWAYS:

quark: .ALWAYS
	quark install --python quark/discovery-2.0.0.q quark/intro.q

discoball:
	./gradlew clean build :discovery-web:buildDockerImage

discostart:
	docker run --name disco -p 52689:52689 \
		-v $$(pwd)/discovery-web/config:/opt/discovery/config datawire/discovery:2.0.0

discostop:
	docker stop disco
	docker rm disco

clobber:
	-find . -name '*.qc' -print0 | xargs -0 rm -f
