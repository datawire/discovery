# Makefile for Datawire Cloud infrastructure components
#
# maintainers: plombardi@datawire.io

# ----------------------------------------------------------------------------------------------------------------------
# Project Configuration
#

# Parses gradle.properties for the version=<string> line. Leading and trailing space will be removed
VERSION=$(shell grep version gradle.properties | awk -F= '{print $$2}' | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$$//')
FOUNDATION_AMI=ami-b0227cda

# ----------------------------------------------------------------------------------------------------------------------
# Metadata Configuration
#

BUILDER ?= $(USER)
TRAVIS_BUILD_NUMBER ?= 0
TRAVIS_COMMIT ?=

# ----------------------------------------------------------------------------------------------------------------------
# Toolchain Configuration

# Workspace (temporary files, programs etc.)
WORK_DIR=build
WORK_PATH=$(CURDIR)/$(WORK_DIR)
TEMP_DIR=tmp
TEMP_PATH=$(WORK_PATH)/$(TEMP_DIR)

# Packer.io Configuration
PKR_VERSION=0.8.6
PKR_ARCH=linux_amd64
PKR_ARCHIVE=packer_$(PKR_VERSION)_$(PKR_ARCH).zip
PKR_PATH=$(WORK_PATH)/packer_$(PKR_VERSION)
PACKER=$(PKR_PATH)/packer
PACKER_OPTS=
PKR_DOWNLOAD_URL=https://releases.hashicorp.com/packer/$(PKR_VERSION)/$(PKR_ARCHIVE)

# Terraform.io Configuration
TF_VERSION=0.6.8
TF_ARCH=linux_amd64
TF_ARCHIVE=terraform_$(TF_VERSION)_$(TF_ARCH).zip
TF_PATH=$(WORK_PATH)/terraform_$(TF_VERSION)
TERRAFORM=$(TF_PATH)/terraform
TERRAFORM_OPTS=""
TF_DOWNLOAD_URL=https://releases.hashicorp.com/terraform/$(TF_VERSION)/$(TF_ARCHIVE)

# ----------------------------------------------------------------------------------------------------------------------
# Recipes
#

version:
	@echo "---> Hub Version '$(HUB_VERSION)'"

prepare:
	@echo "---> preparing workspace $(WORK_PATH)"
	mkdir -p $(WORK_PATH)
	mkdir -p $(TEMP_PATH)
	cd $(WORK_PATH)

	@echo "---> downloading terraform $(TF_VERSION)"
	wget -O $(WORK_PATH)/$(TF_ARCHIVE) $(TF_DOWNLOAD_URL)
	mkdir -p $(TF_PATH)
	unzip $(WORK_PATH)/$(TF_ARCHIVE) -d $(TF_PATH)

	@echo "---> downloading packer $(PKR_VERSION)"
	wget -O $(WORK_PATH)/$(PKR_ARCHIVE) $(PKR_DOWNLOAD_URL)
	mkdir -p $(PKR_PATH)
	unzip $(WORK_PATH)/$(PKR_ARCHIVE) -d $(PKR_PATH)

clean:
	@echo "---> removing $(WORK_PATH)"
	rm -rf $(WORK_PATH)/*

apply:
	terraform apply terraform/

destroy:
	terraform destroy -force terraform/
	
get-latest-foundation-ami:
	@echo "---> Querying for latest Foundation AMI"

prepare-variables:
	@echo "---> Preparing variables file for Packer.io"
	@echo "{\"hub_version\": \"$(VERSION)\", \"builder\": \"$(BUILDER)\", \"build_number\": \"$(TRAVIS_BUILD_NUMBER)\", \"commit\": \"$(TRAVIS_COMMIT)\", \"foundation_ami\": \"$(FOUNDATION_AMI)\"}" > $(TEMP_PATH)/packer-variables.json

ami: prepare-variables
	$(PACKER) validate -var-file=$(TEMP_PATH)/packer-variables.json fedora-x86_64-hub.json
	$(PACKER) build $(PACKER_OPTS) -var-file=$(TEMP_PATH)/packer-variables.json fedora-x86_64-hub.json

server-ami: prepare-variables
	$(PACKER) validate -var-file=$(TEMP_PATH)/packer-variables.json fedora-x86_64-hub.json
	$(PACKER) build $(PACKER_OPTS) -only=hub-server -var-file=$(TEMP_PATH)/packer-variables.json fedora-x86_64-hub.json

gateway-ami: prepare-variables
	$(PACKER) validate -var-file=$(TEMP_PATH)/packer-variables.json fedora-x86_64-hub.json
	$(PACKER) build $(PACKER_OPTS) -only=hub-gateway -var-file=$(TEMP_PATH)/packer-variables.json fedora-x86_64-hub.json