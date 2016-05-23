#!/usr/bin/env bash
#
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

sudo -s dnf -y install unzip python2 python-pip python-virtualenv
yes | sudo -s pip install --upgrade pip
yes | sudo -s pip install --upgrade wheel pytest
curl -L https://raw.githubusercontent.com/datawire/quark/master/install.sh | bash -s

cd /tmp
tar -xvzf introspection.tar.gz
cd -