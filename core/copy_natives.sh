#!/bin/sh

OS=""
FILENAME=""
case $(uname | tr '[:upper:]' '[:lower:]') in
  linux* | bsd* | solaris*)
    OS=linux
    FILENAME=libvoicechat_discord.so
    ;;
  darwin*)
    OS=mac
    FILENAME=libvoicechat_discord.dylib
    ;;
  msys* | cygwin*)
    OS=windows
    FILENAME=voicechat_discord.dll
    ;;
  *)
    echo "Unknown OS"
    exit 1
    ;;
esac

ARCH=""
case $(uname -m) in
  aarch64 | arm64)
    ARCH="aarch64"
    ;;
  amd64 | x86_64 | x86-64)
    ARCH="x64"
    ;;
  i386 | i486 | i586 | i686 | x86 | x86_32)
    ARCH="x86"
    ;;
  *)
    echo "Unknown arch"
    exit 1
    ;;
esac

mkdir -p "src/main/resources/$OS-$ARCH"

DIR="${1:-debug}"

echo "Copying target/$DIR/$FILENAME to src/main/resources/natives/$OS-$ARCH"
cp "target/$DIR/$FILENAME" "src/main/resources/natives/$OS-$ARCH"
