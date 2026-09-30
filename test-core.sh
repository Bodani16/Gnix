#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p build/core-tests
java -m jdk.compiler/com.sun.tools.javac.Main -Xlint:all -d build/core-tests app/src/main/java/com/gnix/app/*.java tests/com/gnix/app/CoreChecks.java
java -cp build/core-tests com.gnix.app.CoreChecks
