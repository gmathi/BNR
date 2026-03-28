#!/bin/sh
#
# Copyright © 2015-2021 the original authors.
# Gradle wrapper shell script.

##############################################################################
# Gradle startup script for UN*X
##############################################################################
set -e

APP_HOME=$(cd "$(dirname "$0")" && pwd)
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
JAVACMD="${JAVA_HOME:+$JAVA_HOME/bin/}java"

exec "$JAVACMD" $DEFAULT_JVM_OPTS \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"
