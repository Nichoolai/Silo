#!/bin/sh
set -eu
command -v mvn >/dev/null 2>&1 || { echo "Maven is not installed or not on PATH."; exit 1; }
mvn -q -DskipTests package dependency:copy-dependencies
mkdir -p data
java -Dsilo.data=data -cp "target/classes:target/dependency/*" com.silo.ui.SiloApp
