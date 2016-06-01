#!/usr/bin/env bash

# Copyright 2015, 2016 Datawire. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
set -euo pipefail

KEYSTORE_PASSWORD="${1:-notasecret}"
KEY_PASSWORD="${2:-notasecret}"
KEY_ALG="${3:-HMacSHA256}"

if [[ "$KEY_ALG" = "HMacSHA256" ]]; then KEY_SIZE=2048; KEY_ALIAS=HS256; fi
if [[ "$KEY_ALG" = "HMacSHA384" ]]; then KEY_SIZE=2048; KEY_ALIAS=HS384; fi
if [[ "$KEY_ALG" = "HMacSHA512" ]]; then KEY_SIZE=2048; KEY_ALIAS=HS512; fi

OUTPUT_PATH="${4:-discovery-web/hmac-$( printf $KEY_ALIAS | tr '[:upper:]' '[:lower:]' ).jceks}"

printf "%s\n" "--> Generating JCE Key Store (alg: $KEY_ALG)"

if [[ -e "$OUTPUT_PATH" ]]; then
    printf "\n%s\n" "    Error: JCE Key Store already exists at '$OUTPUT_PATH'. Remove then retry."
    exit 1
fi

keytool \
  -genseckey \
  -keystore ${OUTPUT_PATH} \
  -storetype jceks \
  -storepass ${KEYSTORE_PASSWORD} \
  -keyalg ${KEY_ALG} \
  -keysize ${KEY_SIZE} \
  -alias ${KEY_ALIAS} \
  -keypass ${KEY_PASSWORD}

printf "%s\n" "--> OK!"
