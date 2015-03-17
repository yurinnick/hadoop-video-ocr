Hadoop OCR
==========

MapReduce job for text recognition from YouTube videos.

# Installation

Tested on Ubuntu 14.04, but highly recommend to use Ubuntu 14.10.

- If you are using Ubutnu 14.04 or lower, add Ubuntu 14.10 repository to sources.list. We need it to install latest opencv 2.4.9 libraries.

 ```
 echo 'deb http://mirrors.kernel.org/ubuntu utopic main universe' >> /etc/apt/sources.list
 sudo apt-get update
 ```
- Install tesseract

    ```sudo apt-get install tesseract-ocr```

- Install libopencv-dev 2.4.9 
    
    ```sudo apt-get install libopencv-dev```

- Install Hadoop 2.6.0, [CDH5 recommended](http://www.cloudera.com/content/cloudera/en/documentation/core/latest/topics/cdh_qs_yarn_pseudo.html)
- Checkout reposotory and build jar with ```mvn clean install```
- Create initial HDFS structure
    
    ```
    sudo -u hdfs hadoop fs -mkdir /user/ubuntu
    sudo -u hdfs hadoop fs -chown ubutnu /user/ubutnu
    ```
- Create input and set input file

    ```
    scripts/set_url.sh <youtube_url>
    ```

- Run MapReduce job

    ```
    scripts/clean.sh && scripts/run.sh <absolute_path_to_jar> && scripts/result.sh
    ``` 
    
       
    
