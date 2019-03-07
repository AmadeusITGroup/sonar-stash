#!/bin/sh
# Exit on failure
set -e

# We don't want to run X times the same analysis because of the matrix configuration
if [ "${SQ_RUN}" != "yes" ]; then
	echo "Duplicated run detected, skipping the SonarQube analysis..."
	exit 0
fi

echo "Starting analysis by SonarQube..."
env
env -u SONAR_TOKEN mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent install sonar:sonar -B -e -V
