#!/bin/sh
# Same as build-and-run.bat for Linux/macOS.
set -e
mkdir -p out
javac -encoding UTF-8 -d out $(find src/main/java -name '*.java')
java -Dsilo.data=data -cp out com.silo.ui.SiloApp
