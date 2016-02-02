#!/bin/bash -eu

ks_password=$1
key_password=$2

echo "---> generating JCE key store (alg: HMacSHA256)"
keytool\
  -genseckey\
  -keystore keystore-256.jceks\
  -storetype jceks\
  -storepass ${ks_password}\
  -keyalg HMacSHA256\
  -keysize 2048\
  -alias HS256\
  -keypass ${key_password}
