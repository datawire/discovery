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

namespace util
{
  namespace internal
  {
    interface Supplier<T> 
    {
      
      @doc("Gets a value")
      T get();
       
      /* BUG (compiler) -- Issue # --> https:// 
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
  }
  
  namespace aws
  {
    class Ec2Host extends internal.Supplier<String>
    {
      static String METADATA_HOST = "169.254.169.254";
      
      String scope;

      Ec2Host(String scope) {
        self.scope = scope;
      }

      String get() 
      {
        if (scope == "internal")
        {
          return url_get("http://" + METADATA_HOST + "/latest/meta-data/local-hostname");
        }
        
        if (scope == "public")
        {
          return url_get("http://" + METADATA_HOST + "/latest/meta-data/hostname");
        }

        return null;
      }
    }
  }
  
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
