#!/bin/sh

# Check if $1 is not set or empty
if [ -z "$1" ]; then
    echo "Error: Need to specify the source code directory as the first argument."
    exit 1
fi

SOURCE=$1
OUT=$2

if [ -z "$2" ]; then
    OUT=$1/bug.
fi

~/llm4pa/joern/joern --script ~/llm4pa/joern/pp.sc --param inputPath=$SOURCE --param outputPath=$OUT

