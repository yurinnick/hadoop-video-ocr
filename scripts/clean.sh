#!/bin/bash

if hadoop fs -test -d /tmp/hadoop-ocr-output; then
    hadoop fs -rm -r -f /tmp/hadoop-ocr-output
fi
