package discovery 2.0.0;

/* 
 * Copyright 2016 Datawire. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

include util.q;

import util.internal;
import util.aws;
import util.google;
import util.kubernetes;

namespace util 
{
  
  class Datawire 
  {

    static String TOKEN_VARIABLE_NAME = "DATAWIRE_TOKEN";
  
    @doc("Returns the Datawire Access Token by reading the environment variable DATAWIRE_TOKEN.")
    static String getToken()
    {
        String token = EnvironmentVariable(TOKEN_VARIABLE_NAME).get();
        if (token == null)
        {
            Runtime.fail("Environment variable 'DATAWIRE_TOKEN' is not set.");
        }

        return token;
    }
  }

  class Platform 
  {

    static String PLATFORM_TYPE                  = EnvironmentVariable("DATAWIRE_PLATFORM_TYPE").get();
    static String PLATFORM_TYPE_EC2              = "EC2";
    static String PLATFORM_TYPE_GOOGLE_COMPUTE   = "GoogleCompute";
    static String PLATFORM_TYPE_GOOGLE_CONTAINER = "GoogleContainer";
    static String PLATFORM_TYPE_KUBERNETES       = "Kubernetes";
    static String ROUTABLE_HOST_VARIABLE_NAME    = "DATAWIRE_ROUTABLE_HOST";
    static String ROUTABLE_PORT_VARIABLE_NAME    = "DATAWIRE_ROUTABLE_PORT";

    @doc("Returns the routable hostname or IP for this service instance.")
    @doc("This method always returns the value of the environment variable DATAWIRE_ROUTABLE_HOST if it is defined.")
    static String getRoutableHost()
    {
      if (EnvironmentVariable(ROUTABLE_HOST_VARIABLE_NAME).isDefined())
      {
        return EnvironmentVariable(ROUTABLE_HOST_VARIABLE_NAME).get();
      }
    
      if (PLATFORM_TYPE.startsWith(PLATFORM_TYPE_EC2))
      {
        List<String> parts = PLATFORM_TYPE.split(":");
        
        if(parts.size() == 2)
        {
          return Ec2Host(parts[1]).get();
        }
        else 
        {
          Runtime.fail("Invalid format for DATAWIRE_PLATFORM_TYPE == EC2. Expected EC2:<scope>.");
        }
      }
      
      if (PLATFORM_TYPE == PLATFORM_TYPE_GOOGLE_COMPUTE)
      {
        return GoogleComputeEngineHost().get();
      }
      
      if (PLATFORM_TYPE == PLATFORM_TYPE_KUBERNETES || PLATFORM_TYPE == PLATFORM_TYPE_GOOGLE_CONTAINER)
      {
        return KubernetesHost().get();
      }
      
      return null;
    }

    @doc("Returns the routable port number for this service instance or uses the provided port if a value cannot be resolved.")
    @doc("This method always returns the value of the environment variable DATAWIRE_ROUTABLE_PORT if it is defined.")
    static int getRoutablePort(int servicePort)
    {
      if (EnvironmentVariable(ROUTABLE_PORT_VARIABLE_NAME).isDefined())
      {
        return parseInt(EnvironmentVariable(ROUTABLE_PORT_VARIABLE_NAME).get());
      }

      if (PLATFORM_TYPE == PLATFORM_TYPE_KUBERNETES)
      {
        return KubernetesPort().get();
      }
      
      return servicePort;
    }
  }
}
