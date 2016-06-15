#!/usr/bin/env python
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

from datawire_introspection import DatawireToken
from datawire_introspection import Platform
import pytest
import os


# # TODO: Fix test after https://github.com/datawire/quark/issues/146 is resolved.
# def test_datawire_getToken_not_set_causes_failure():
#     if 'DATAWIRE_TOKEN' in os.environ:
#         del os.environ['DATAWIRE_TOKEN']

#     with pytest.raises(Exception):
#         DatawireToken.getToken()


def test_datawire_getToken_set():
    os.environ["DATAWIRE_TOKEN"] = "notasecret"
    assert DatawireToken.getToken() == "notasecret"
    del os.environ['DATAWIRE_TOKEN']


# # TODO: Fix test after https://github.com/datawire/quark/issues/146 is resolved.
# def test_DATAWIRE_PLATFORM_TYPE_env_var_not_set_causes_failure_calling_getRoutableHost():
#     with pytest.raises(Exception):
#         Platform.getRoutableHost()


def test_DATAWIRE_ROUTABLE_PORT_env_var_set_and_Platform_getRoutablePort_returns_variable_value():
    os.environ["DATAWIRE_ROUTABLE_PORT"] = "8008"
    assert Platform.getRoutablePort(9000) == 8008
    del os.environ["DATAWIRE_ROUTABLE_PORT"]


def test_Platform_getRoutablePort_uses_provided_service_port_if_nothing_else_resolves():
    assert Platform.getRoutablePort(8008) == 8008


def test_DATAWIRE_ROUTABLE_HOST_env_var_set_and_Platform_getRoutableHost_returns_variable_value():
    os.environ["DATAWIRE_ROUTABLE_HOST"] = "hello.datawire.io"
    assert Platform.getRoutableHost() == "hello.datawire.io"
    del os.environ["DATAWIRE_ROUTABLE_HOST"]


# def test_DATAWIRE_PLATFORM_TYPE_env_var_EC2_causes_failure_when_scope_is_not_set():
#     with pytest.raises(Exception):
#         os.environ["DATAWIRE_PLATFORM_TYPE"] = "ec2"
#         assert Platform.getRoutableHost()
#         del os.environ["DATAWIRE_PLATFORM_TYPE"]
