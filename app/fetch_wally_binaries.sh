#!/usr/bin/env bash
# Downloads and installs the pre-built wally libraries for use by StashWallet-Android
set -e

if [ -d wallycore ]; then
    echo "Found a 'wallycore' folder, exiting now"
    exit 0
fi

# The version of wally to fetch and its sha256 checksum for integrity checking
NAME="wallycore-android-jni"
TARBALL="${NAME}.tar.gz"
URL="https://github.com/ElementsProject/libwally-core/releases/download/release_0.7.8/${TARBALL}"
SHA256="809cc59e5f7d24197557f7fe5f5d8e274f68ee7037d6de5f68e437d5ae8385eb"
# Pre-requisites
function check_command() {
    command -v $1 >/dev/null 2>&1 || { echo >&2 "$1 not found, exiting."; exit 1; }
}
check_command curl
check_command gzip
check_command shasum

# Find out where we are being run from to get paths right
OLD_PWD=$(pwd)
APP_ROOT=${OLD_PWD}
if [ -d "${APP_ROOT}/app" ]; then
    APP_ROOT="${APP_ROOT}/app"
fi

JNILIBSDIR=${APP_ROOT}/src/main/jniLibs
WALLY_JAVA_DIR="${APP_ROOT}/src/main/java/com/blockstream"

# Clean up any previous install
rm -rf wallycore-android-jni* ${APP_ROOT}/src/main/jniLibs ${WALLY_JAVA_DIR}

# Fetch, validate and decompress wally
curl -sL -o ${TARBALL} "${URL}"
echo "${SHA256}  ${TARBALL}" | shasum -a 256 --check
tar xvf ${TARBALL}
rm ${TARBALL}

# Move the libraries and Java wrapper where we need them
mv ${NAME}/lib/ ${APP_ROOT}/src/main/jniLibs
mkdir -p ${WALLY_JAVA_DIR}
mv ${NAME}/src/swig_java/src/com/blockstream/* ${WALLY_JAVA_DIR}

# Cleanup
rm -fr $NAME

