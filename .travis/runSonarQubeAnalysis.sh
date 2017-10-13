#!/bin/sh
# Exit on failure
set -e

#
# SOURCE: https://github.com/bellingard/multi-language-project/blob/master/runSonarQubeAnalysis.sh
#

# This assumes that the 2 following variables are defined:
# - SONAR_HOST_URL => should point to the public URL of the SQ server)
# - SONAR_TOKEN    => token of a user who has the "Execute Analysis" permission on the SQ server

# We don't want to run X times the same analysis because of the matrix configuration
if [ "${SQ_RUN}" != "yes" ]; then
	echo "Duplicated run detected, skipping the SonarQube analysis..."
	exit 0
fi


# And run the analysis
# It assumes that the project uses Maven and has a POM at the root of the repo
if [ "$TRAVIS_BRANCH" = "master" ] && [ "${TRAVIS_PULL_REQUEST}" = "false" ]; then
	# => This will run a full analysis of the project and push results to the SonarQube server.
	#
	# Analysis is done only on master so that build of branches don't push analyses to the same project and therefore "pollute" the results
	echo "Starting analysis by SonarQube..."
	mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent package sonar:sonar -B -e -V \
		-Dsonar.host.url=$SONAR_HOST_URL \
		-Dsonar.organization=$SONAR_ORGA \
		-Dsonar.login=$SONAR_TOKEN

elif [ "${TRAVIS_PULL_REQUEST}" != "false" ] && [ -n "${GITHUB_TOKEN-}" ]; then
	# => This will analyse the PR and display found issues as comments in the PR, but it won't push results to the SonarQube server
	#
	# For security reasons environment variables are not available on the pull requests
	# coming from outside repositories
	# http://docs.travis-ci.com/user/pull-requests/#Security-Restrictions-when-testing-Pull-Requests
	# That's why the analysis does not need to be executed if the variable GITHUB_TOKEN is not defined.
	echo "Starting Pull Request analysis by SonarQube..."
	mvn clean package sonar:sonar -B -e -V \
		-Dsonar.host.url=$SONAR_HOST_URL \
		-Dsonar.login=$SONAR_TOKEN \
		-Dsonar.organization=$SONAR_ORGA \
		-Dsonar.analysis.mode=preview \
		-Dsonar.github.oauth=$GITHUB_TOKEN \
		-Dsonar.github.repository=$TRAVIS_REPO_SLUG \
		-Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST

else
	# When neither on master branch nor on a non-external pull request => nothing to do
	#
	# However, it is good to know why we are here
	echo "No SonarQube anaysis necessary in this case (current branch: ${TRAVIS_BRANCH} & PR context: ${TRAVIS_PULL_REQUEST})..."

	# It is useful to know what is the status of the secure entries (can explain why it was not started)
	if [ -n "${GITHUB_TOKEN-}" ]; then
		echo "\t=> GITHUB_TOKEN is defined"
	else
		echo "\t=> GITHUB_TOKEN is NOT defined"
	fi

	if [ -n "${SONAR_TOKEN-}" ]; then
		echo "\t=> SONAR_TOKEN is defined"
	else
		echo "\t=> SONAR_TOKEN is NOT defined"
	fi

fi
