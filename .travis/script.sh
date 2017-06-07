#!/bin/bash
set -ev

if [ -z "${TEST_SUITE}" ]; then
	echo "No \$TEST_SUITE specified, aborting!"
	exit 1

elif [ "unit" = "${TEST_SUITE}" ]; then
	mvn -e test -Pcoverage-per-test

elif [ "integration" = "${TEST_SUITE}" ]; then
	if [ -z "${SONARQUBE_VERSION}" ]; then
		echo "No \$SONARQUBE_VERSION specified, aborting!"
		exit 1
	fi

	tail -F "target/fixtures/sonarqube/sonarqube-${SONARQUBE_VERSION}/logs/sonar.log" &
	# otherwise the rails bundled with sonarqube tries to load test.yml which does
	# not exist
	export RAILS_ENV=production
	mvn -e verify -Dtest.sonarqube.dist.version="${SONARQUBE_VERSION}"
fi
