# -*- mode: ruby -*-
# vi: set ft=ruby :

$script = <<SCRIPT
#!/usr/bin/env bash

export DEBIAN_FRONTEND=noninteractive
sleep 5s

apt-get update
apt-get install gdebi -y
SCRIPT

Vagrant.configure(2) do |config|
  #config.vm.box = "ubuntu/xenial64"
  config.vm.box = "bento/ubuntu-16.04"
  config.vm.provision "shell", inline: $script
end