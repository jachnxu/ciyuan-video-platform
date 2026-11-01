#!/bin/bash
export ANDROID_HOME=/opt/android-sdk
export ANDROID_SDK_ROOT=/opt/android-sdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
cd /opt/ciyuan/src/android
echo "sdk.dir=/opt/android-sdk" > local.properties
rm -f build.log
echo "BUILD_START $(date)" > build.log
/opt/gradle/gradle-8.2/bin/gradle assembleDebug --console=plain >> build.log 2>&1
echo "GRADLE_EXIT=$?" >> build.log
echo "BUILD_END $(date)" >> build.log
