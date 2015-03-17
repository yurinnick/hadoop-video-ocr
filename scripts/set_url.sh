#!/bin/bash
echo "${1}" > /tmp/input.txt

if ! hadoop fs -test -d /tmp/hadoop-ocr-input; then
    hadoop fs -mkdir -p /tmp/hadoop-ocr-input
fi

hadoop fs -put -f /tmp/input.txt /tmp/hadoop-ocr-input/
