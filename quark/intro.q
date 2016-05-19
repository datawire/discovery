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

import util.aws;
import util.google;
import util.kubernetes;
 
namespace util 
{
  
  class Datawire 
  {
  
    @doc("Returns the Datawire Access Token by inspecting the environment variable DATAWIRE_TOKEN.")
    static String token() 
    {
      // TODO: Waiting on Quark Environment Variable Implementation
      return null;
    }
  }

  class Platform 
  {
    static String AMAZON_EC2_PLATFORM_TYPE            = "EC2";
    static String GOOGLE_COMPUTE_ENGINE_PLATFORM_TYPE = "GCE";
    static String KUBERNETES_PLATFORM_TYPE            = "KUBERNETES";
  
    static String DATAWIRE_PROVIDER_TYPE = null; // todo: replace with environment variable lookup
    
    @doc("Returns the routable hostname or IP for this service instance.")
    @doc("This method always returns the value of the environment variable DATAWIRE_ROUTABLE_HOST if it is defined.")
    static String routableHost() 
    {
      // TODO: Return value of DATAWIRE_ROUTABLE_HOST if defined.
    
      if (DATAWIRE_PROVIDER_TYPE.startsWith(AMAZON_EC2_PLATFORM_TYPE)) 
      {
        List<String> parts = DATAWIRE_PROVIDER_TYPE.split(":");
        
        if(parts.size() == 2)
        {
          return Ec2Host(parts[1]).get();
        }
        else 
        {
          Runtime.fail("Invalid format for DATAWIRE_PROVIDER_TYPE == EC2. Expected EC2:<scope>.");
        }
      }
      
      if (DATAWIRE_PROVIDER_TYPE == GOOGLE_COMPUTE_ENGINE_PLATFORM_TYPE)
      {
        return GoogleComputeEngineHost().get();
      }
      
      if (DATAWIRE_PROVIDER_TYPE == KUBERNETES_PLATFORM_TYPE)
      {
        return KubernetesHost().get();
      }
      
      return null;
    }

    @doc("Returns the routable port number for this service instance or uses the provided port if a value cannot be resolved.")
    @doc("This method always returns the value of the environment variable DATAWIRE_ROUTABLE_PORT if it is defined.")
    static int routablePort(int servicePort) 
    {
      // TODO: Return value of DATAWIRE_ROUTABLE_PORT if defined.    
    
      if (DATAWIRE_PROVIDER_TYPE == KUBERNETES_PLATFORM_TYPE)
      {
        return KubernetesPort().get();
      }
      
      return servicePort;
    }
  }
}
