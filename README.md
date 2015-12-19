# SonarQube Stash (BitBucket) Plugin [![Build Status](https://travis-ci.org/racodond/sonar-stash.svg)](https://travis-ci.org/racodond/sonar-stash)

**SonarQube is now a real reviewer!**
SonarQube Stash (BitBucket) plugin is a pull request decorator that displays SonarQube issues on pull request.

![Screenshot SonarQube plugin](resources/Stash-plugin-issues.PNG)

After every run, in addition of the diff view, you may access an overview of the SonarQube analysis:

![Screenshot SonarQube plugin](resources/Stash-plugin-overview.PNG)


## Getting Started

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
~> cp target/sonar-stash-plugin-X.X.jar $SONARQUBE_HOME/extensions/plugins
```

#### Configuration on SonarQube server
Go to Settings > Configuration > General Settings > Stash:

![Screenshot SonarQube plugin](resources/Sonar-plugin-configuration.PNG)


## How to run the plugin?

To activate the plugin, add the following options to the SonarQube runner (for instance with SonarQube Scanner):

For SonarQube 5.2+:
```
sonar-runner -Dsonar.analysis.mode=preview -Dsonar.stash.notification=true -Dsonar.stash.project=<PROJECT> -Dsonar.stash.repository=<REPO> -Dsonar.stash.pullrequest.id=<PR_ID> -Dsonar.stash.password=<STASH_PASSWORD>
```

Before SonarQube 5.2:
```
sonar-runner -Dsonar.analysis.mode=incremental -Dsonar.stash.notification=true -Dsonar.stash.project=<PROJECT> -Dsonar.stash.repository=<REPO> -Dsonar.stash.pullrequest.id=<PR_ID> -Dsonar.stash.password=<STASH_PASSWORD>
```

![Screenshot SonarQube plugin](resources/Stash-plugin-logs.PNG)

