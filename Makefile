all: quark

.ALWAYS:

quark: .ALWAYS
	quark install --python quark/discovery-2.0.0.q quark/datawire_introspection.q

test: test-common test-ec2

test-common: .ALWAYS
	( cd quark && sh bin/run.sh )

test-ec2: .ALWAYS
	( cd quark && sh bin/run.sh -i ec2 )

discoball:
	./gradlew clean build :discovery-web:buildDockerImage

discostart:
	docker run --name disco -p 52689:52689 \
		-v $$(pwd)/discovery-web/config:/opt/discovery/config datawire/discovery:2.0.0

clean:
	./gradlew clean

clobber: clean
	find . -name '*.qc' -print0 | xargs -0 rm -f

discostop:
	docker stop disco
	docker rm disco
