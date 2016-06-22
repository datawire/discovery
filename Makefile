QUARKINSTALLER=https://raw.githubusercontent.com/datawire/quark/develop/install.sh
QUARKBRANCH=1.0.133

all: test

.ALWAYS:

test: checkEnv python-deps quark test-common test-ec2

checkEnv:
	@which pip >/dev/null 2>&1 || { \
		echo "Could not find pip -- is the correct venv active?" >&2 ;\
		exit 1 ;\
	}
	@which quark >/dev/null 2>&1 || { \
		echo "Could not find quark -- is the correct venv active?" >&2 ;\
		echo "(use 'make install-quark' to initialize things in a new venv)" >&2 ;\
		exit 1 ;\
	}
	# @which npm >/dev/null 2>&1 || { \
	# 	echo "Could not find npm -- is it installed?" >&2 ;\
	# 	echo "(if not, check out https://docs.npmjs.com/getting-started/installing-node)" >&2 ;\
	# 	exit 1 ;\
	# }

python-deps:
	pip install pytest

install-quark:
	curl -sL "${QUARKINSTALLER}" | bash -s -- ${QUARKINSTALLARGS} ${QUARKBRANCH}

quark: .ALWAYS
	quark install --python quark/discovery-2.0.0.q quark/datawire_introspection.q

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
