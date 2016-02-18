#!/bin/bash

chmod +x gradlew

bash gradlew clean test distTar
bash packer.sh