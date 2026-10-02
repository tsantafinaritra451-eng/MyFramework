#!/bin/bash

TOMCAT_LIB="/home/tsanta/tomcat/lib"
FRAMEWORK_LIB="lib"

JAR_NAME="framework.jar"
BIN_DIR="bin"
SRC_DIR="src/main/java"

echo "Compilation du framework..."

rm -rf $BIN_DIR
rm -f $JAR_NAME
mkdir -p $BIN_DIR

find $SRC_DIR -name "*.java" > sources.txt

javac -parameters -cp "$TOMCAT_LIB/*:$FRAMEWORK_LIB/*" -d $BIN_DIR @sources.txt

if [ $? -ne 0 ]; then
    echo "Erreur de compilation !"
    rm -f sources.txt
    exit 1
fi
rm -f sources.txt

echo "Creation du JAR..."
jar cf $JAR_NAME -C $BIN_DIR .

if [ $? -ne 0 ]; then
    echo "Erreur lors de la creation du JAR !"
    exit 1
fi

echo "Framework package avec succes : $JAR_NAME"