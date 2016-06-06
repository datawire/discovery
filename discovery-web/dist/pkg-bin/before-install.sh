#!/usr/bin/env bash
set -euo pipefail

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

APP_NAME="discovery"
APP_USER="$APP_NAME"

# Create a service user and group.
if ! id ${APP_USER} > /dev/null 2>&1 ; then
	adduser --home /opt/${APP_NAME} --no-create-home \
		--disabled-password --shell /bin/false \
		--gecos "" \
		${APP_USER}
fi