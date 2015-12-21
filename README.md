# SonarQube Stash (BitBucket) plugin [![Build Status](https://travis-ci.org/AmadeusITGroup/sonar-stash.svg)](https://travis-ci.org/AmadeusITGroup/sonar-stash)

**SonarQube is now a real reviewer!**
SonarQube Stash (BitBucket) plugin is a pull-request decorator which allows to integrate SonarQube violations directly into your pull-request.

![Screenshot SonarQube plugin](resources/Stash-plugin-issues.PNG)

After every run, in addition of the diff view, you may access to an overview of your SQ analysis:

![Screenshot SonarQube plugin](resources/Stash-plugin-overview.PNG)


## Getting started

#### Prerequisites
- Git client to checkout the code
- Maven 3.0.5
- JDK 1.7
- SonarQube 4.5.4 (LTS)
- Stash (BitBucket) 3.x

#### To build the plugin
This command generates a jar file.
```
mvn clean install
```

#### To deploy the plugin
Just copy the sonar-stash-plugin jar file to the plugin folder of the expected SonarQube server and restart the SonarQube server. For instance, on Linux platform:
```
~> cp target/sonar-stash-plugin-1.0.jar $SONARQUBE_HOME/extensions/plugins
```

#### Configuration on SonarQube server
Go to Stash general settings screen on SonarQube server to fill:

![Screenshot SonarQube plugin](resources/Sonar-plugin-configuration.PNG)

**Stash base URL** (sonar.stash.url): To define Stash instance.

**Stash base user** (sonar.stash.login): To define user to push violations on Stash pull-request. User must have **REPO_READ permission** for the repository.
**Please notice Stash password needs to be provided to sonar-runner through sonar.stash.password in commandline**.

**Stash issue threshold** (sonar.stash.issue.threshold): To limit the number of issue pushed to Stash.

**Stash timeout** (sonar.stash.timeout): To timeout when Stash Rest api does not replied with expected.

**Stash reviewer approval** (sonar.stash.reviewer.approval): SonarQube is able to approve the pull-request if there is no new issue introduced by the change.   
By default, this feature is deactivated: if activated, **Stash base user must have REPO_WRITE permission for the repositories.** 

![Screenshot SonarQube plugin](resources/Sonar-plugin-approver.PNG)


## How to run the plugin?

To activate the plugin, just add the following options to SonarQube launcher (for instance with sonar-runner):
```
sonar-runner -Dsonar.analysis.mode=incremental -Dsonar.stash.notification -Dsonar.stash.project=<PROJECT> -Dsonar.stash.repository=<REPO> -Dsonar.stash.pullrequest.id=<PR_ID> -Dsonar.stash.password=<STASH_PASSWORD>...
```

![Screenshot SonarQube plugin](resources/Stash-plugin-logs.PNG)

#### Reset comments of previous SonarQube analysis

If needed, you can reset comments published during the previous SonarQube analysis of your pull-request. Please add **sonar.stash.comments.reset** option to your SonarQube analysis. Please notice only comments linked to the **sonar.stash.login** user will be deleted. This reset will be the first action performed by the plugin.
 ```
sonar-runner -Dsonar.analysis.mode=incremental -Dsonar.stash.notification -Dsonar.stash.comments.reset -Dsonar.stash.project=<PROJECT> -Dsonar.stash.repository=<REPO> -Dsonar.stash.pullrequest.id=<PR_ID> -Dsonar.stash.password=<STASH_PASSWORD>...
```
