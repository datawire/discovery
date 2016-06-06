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

ln -s /opt/discovery/discovery-web-*-fat.jar /opt/discovery/discovery-web.jar

mkdir -p /var/log/discovery
chown -R ${APP_USER}:${APP_USER} /var/log/${APP_NAME}

mkdir -p /etc/${APP_NAME}
chown -R ${APP_USER}:${APP_USER} /etc/${APP_NAME}

mkdir -p /run/discovery
chown -R ${APP_USER}:${APP_USER} /run/${APP_NAME}

ln -s /etc/nginx/sites-available/${APP_NAME}.conf /etc/nginx/sites-enabled/${APP_NAME}.conf
rm -f /etc/nginx/sites-enabled/default
nginx -t

chmod +x /opt/${APP_NAME}/bin/discovery.sh

systemctl daemon-reload
systemctl restart nginx