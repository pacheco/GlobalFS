#!/bin/bash

PACKAGES=(
    zookeeper
    zookeeperd
    dtach
    fuse
    libfuse2
    libfuse-dev
    maven
    fabric
    dstat
    python-pip
    openjdk-7-jdk
    golang
    zenity
    build-essential
    zlib1g-dev
    libbz2-dev
    libsnappy-dev
)

sudo apt-get install ${PACKAGES[@]}
sudo pip install boto
