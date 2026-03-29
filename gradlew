#!/bin/sh
#
# Gradle startup script for UN*X
#

##############################################################################
set -e

APP_HOME=$(cd "$(dirname "$0")" && pwd)
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
JAVACMD="${JAVA_HOME:+$JAVA_HOME/bin/}java"

# JVM options — no embedded quotes, just plain args separated by spaces
DEFAULT_JVM_OPTS="-Xmx64m -Xms64m"

exec "$JAVACMD" $DEFAULT_JVM_OPTS \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"
