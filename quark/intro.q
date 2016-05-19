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
  
  class Datawire 
  {
  
    @doc("Returns the Datawire Access Token by inspecting the environment variable DATAWIRE_TOKEN.")
    static String token() 
    {
      return null;
    }
  }

  class Platform 
  {
    
    @doc("Returns the routable hostname or IP for this service instance.")
    @doc("This method always returns the value of the environment variable DATAWIRE_ROUTABLE_HOST if it is defined.")
    static String routableHost() 
    {
      return null;
    }

    @doc("Returns the routable port number for this service instance or uses the provided port if a value cannot be resolved.")
    @doc("This method always returns the value of the environment variable DATAWIRE_ROUTABLE_PORT if it is defined.")
    static String routablePort(int servicePort) 
    {
      return null;
    }
  }
}
