echo Building m2eclipse-wtp (without running tests) using Eclipse 3.6 target platform
set MAVEN_OPTS=-Xmx512m
mvn clean install -Dmaven.test.skip=true -Dtarget.platform=m2e-wtp-e36