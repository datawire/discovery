#!/bin/bash -eu

ks_password=$1
key_password=$2

echo "---> generating JCE key store"
keytool\
  -genseckey\
  -keystore keystore.jceks\
  -storetype jceks\
  -storepass ${ks_password}\
  -keyalg HMacSHA512\
  -keysize 2048\
  -alias HS512\
  -keypass ${key_password}
