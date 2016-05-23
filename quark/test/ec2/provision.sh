#!/usr/bin/env bash

sudo -s dnf -y install unzip python2 python-pip python-virtualenv
yes | sudo -s pip install --upgrade pip
yes | sudo -s pip install wheel
curl -L https://raw.githubusercontent.com/datawire/quark/master/install.sh | bash -s

cd /tmp
tar -xvzf intro.tar.gz
cd -