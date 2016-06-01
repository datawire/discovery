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

import quark.os;

namespace util
{
  namespace internal
  {
    interface Supplier<T> 
    {
      
      @doc("Gets a value")
      T get();
       
      /* BUG (compiler) -- Issue # --> https://github.com/datawire/quark/issues/143
      @doc("Gets a value or if null returns the given alternative.")
      T orElseGet(T alternative) 
      {
        T result = get();
        if (result != null) 
        {
          return result;
        }
        else
        {
          return alternative;
        }
      }
      */

    }

    class EnvironmentVariable extends Supplier<String>
    {
      String variableName;

      EnvironmentVariable(String variableName)
      {
        self.variableName = variableName;
      }

      bool isDefined()
      {
        return get() != null;
      }

      String get()
      {
        return Environment.getEnvironment()[variableName];
      }

      // TODO: Remove once Issue #143 --> https://github.com/datawire/quark/issues/143 is resolved.
      String orElseGet(String alternative)
      {
        String result = get();
        if (result != null)
        {
          return result;
        }
        else
        {
          return alternative;
        }
      }
    }
  }
  
  namespace aws
  {
    class Ec2Host extends internal.Supplier<String>
    {

      //static String METADATA_HOST = util.internal.EnvironmentVariable("DATAWIRE_METADATA_HOST_OVERRIDE").orElseGet("169.254.169.254");
      
      String scope;

      Ec2Host(String scope) {
        self.scope = toUpperCase(scope);
      }

      static String metadataHost() {
        return util.internal.EnvironmentVariable("DATAWIRE_METADATA_HOST_OVERRIDE").orElseGet("169.254.169.254");
      }

      String get() 
      {
        if (scope == "INTERNAL")
        {
          return url_get("http://" + metadataHost() + "/latest/meta-data/local-hostname");
        }
        
        if (scope == "PUBLIC")
        {
          return url_get("http://" + metadataHost() + "/latest/meta-data/public-hostname");
        }

        return null;
      }
    }
  }

  /* TODO(plombardi): Implement once we have a ComputeEngine account.
  namespace google
  {
    class GoogleComputeEngineHost extends internal.Supplier<String>
    {
      static String METADATA_HOST = "metadata.google.internal";

      String get() 
      {
        return null;
      }
    }
  }
  */
  
  namespace kubernetes
  {
    class KubernetesHost extends internal.Supplier<String>
    {
      String get()
      {
        return null;
      }
    }
    
    class KubernetesPort extends internal.Supplier<int>
    {
      int get()
      {
        return null;
      }
    }
  }
}
