#!/bin/bash

# file: mkjceks-hs256.sh
#
# Generates a Java Key Store that can be used for JWT signing and verification.

key_store_password=$1
key_password=$2

echo "--> Creating JCE Key Store"
keytool \
  -genseckey \
  -keystore keystore-hs256.jceks \
  -storetype jceks \
  -storepass ${key_store_password} \
  -keyalg HMacSHA256 \
  -keysize 2048 \
  -alias HS256 \
  -keypass ${key_password}

echo "--> Done!"