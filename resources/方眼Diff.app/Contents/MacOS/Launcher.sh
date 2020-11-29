#!/bin/bash

APP_ROOT=$(cd $(dirname $0); cd ..; pwd)

$APP_ROOT/PlugIns/jre-min/bin/java -Xms256m -Xmx2048m -jar $APP_ROOT/Java/xyz.hotchpotch.hogandiff.jar $@ &
