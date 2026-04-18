#!/bin/sh
#
# Simplified Gradle wrapper launcher script.
#
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
JAVA_EXE=${JAVA_HOME:+$JAVA_HOME/bin/}java

if [ ! -x "$JAVA_EXE" ]; then
  JAVA_EXE=java
fi

exec "$JAVA_EXE" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
