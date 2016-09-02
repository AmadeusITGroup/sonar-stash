#!/bin/bash
set -ev

if [ -z "${TEST_SUITE}" ]; then
	echo "No \$TEST_SUITE specified, aborting!"
	exit 1

elif [ "unit" = "${TEST_SUITE}" ]; then
	mvn clean test -Pcoverage-per-test

elif [ "integration" = "${TEST_SUITE}" ]; then
	if [ -z "${SONARQUBE_VERSION}" ]; then
		echo "No \$SONARQUBE_VERSION specified, aborting!"
		exit 1

	tail -F "target/fixtures/sonarqube/sonarqube-${SONARQUBE_VERSION}/logs/sonar.log" &
	mvn verify -Dtest.sonarqube.dist.version="${SONARQUBE_VERSION}"
fi
