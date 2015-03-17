#!/bin/bash
hadoop jar ${1}/mrjob-1.0-SNAPSHOT.jar HadoopOCR /tmp/hadoop-ocr-input /tmp/hadoop-ocr-output -Djava.library.path=/usr/lib/jni
