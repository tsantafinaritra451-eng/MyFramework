#!/bin/bash

TOMCAT_LIB="/home/tsanta/tomcat/lib" 

JAR_NAME="framework.jar"
BIN_DIR="bin"
SRC_DIR="src/main/java"



echo "============= COMPIServletExceptionLATION DU FRAMEWORK ============="

# 1. Nettoyage
rm -rf $BIN_DIR
rm -f $JAR_NAME
mkdir $BIN_DIR

# 2. Compilation
echo "compilation"
find $SRC_DIR -name "*.java" > sources.txt
javac -cp "$TOMCAT_LIB/*" -d $BIN_DIR @sources.txt

if [ $? -ne 0 ]; then
    echo " Erreur de compilation !"
    rm sources.txt
    exit 1
fi
rm sources.txt

# 3. Création du JAR
echo "[2/3] Création du fichier $JAR_NAME..."
jar cf $JAR_NAME -C $BIN_DIR .

if [ $? -ne 0 ]; then
    echo " Erreur lors de la création du JAR !"
    exit 1
fi



echo " Framework packagé avec succès en $JAR_NAME !"
echo "===================================================="