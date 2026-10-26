#!/bin/sh
# Runs the JUnit 5 tests without Maven. Needs junit-platform-console-standalone.jar (download from Maven Central) in ./lib.
set -e
JAR=lib/junit-platform-console-standalone.jar
[ -f "$JAR" ] || { echo "Put junit-platform-console-standalone-1.10.x.jar at $JAR (or just use: mvn test)"; exit 1; }
mkdir -p out out-test
javac -d out $(find src/main/java -name '*.java')
javac -d out-test -cp "out:$JAR" $(find src/test/java -name '*.java')
java -jar "$JAR" execute -cp out:out-test --scan-classpath out-test --details=summary
