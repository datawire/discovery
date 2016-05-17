package daas 0.1.0;

/*

  HostInfo Resolution
  -------------------

  Two pieces of information need to be determined! The first it the host
  host which is dynamically allocated by the infrastructure service.

  The second piece of information is the service port. For virtual
  machines this is going to be application configuration that is stored on disk 
  somewhere or hard-coded during service initialization.

  For containers the container deployment service is going to select an
  available host port and then map it to a container port. A service listens on 
  the container port and the underlying host system forwards traffic through
  the host port to the container port.

    HOST ADDRESS RESOLUTION
    =======================

    Default Host Address Resolution Chain for VM systems
    -------------------------------------------------------

    1. Query Custom Provider (skip if not configured).
    2. Query VM Metadata Provider (confirmed: EC2, GCE).

    Default Host Address Resolution Chain for Container systems
    --------------------------------------------------------------

    1. Query Custom Provider (skip if not configured).
    2. Inspect Container environment variables.
    3. Inspect Container Engine API service.

    PORT RESOLUTION
    ===============

    Default Port Address Resolution Chain for VM systems
    -------------------------------------------------------

    1. Manual API config (very likely the easiest). Anything beyond that
       gets complicated (e.g. reading the servers config files or requiring
       custom service bootstrapping code to set environment variables.

    Default Port Address Resolution Chain for Container systems
    --------------------------------------------------------------

    1. Query Custom Provider (skip if not configured).
    2. Inspect Container environment variables.
    3. Inspect Container Engine API service.

  Host Platform Detection
  -----------------------

  There is no technically good way to determine the host provider (e.g. EC2 
  vs Kubernetes). Instead we will rely on environment variable injection via
  the $HOST_PLATFORM variable. 
    
  +----------------------------+---------------------+
  | Platform Name              | HOST_PLATFORM value |
  +----------------------------+---------------------+
  | Amazon EC2                 | EC2                 |
  | Amazon EC2 ECS             | ECS                 |
  | Apache Mesos (Marathon)    | MARATHON            |
  | Apache Mesos (Singularity) | SINGULARITY         |
  | Docker Swarm               | SWARM               |
  | Google Compute Engine      | GCE                 |
  | Kubernetes                 | KUBERNETES          |
  +----------------------------+---------------------+

  In the case where $HOST_PLATFORM is already used by a customer we can allow
  them to specify an alternative variable via the API.

  API Sketch:

    // Simplest usage assumes $HOST_PLATFORM environment variable is set.
    
    HostInfo hostInfo = HostInfo.resolve();

    hostInfo.host(); // returns the host IP or DNS name as a String
    hostInfo.port();    // returns the service port as an Integer 
    
    // Use an alternative to the $HOST_PLATFORM variable.

    HostInfo hostInfo = HostInfo.resolve("SNOWFLAKE_HOST_PROVIDER")
    
    // Dynamically resolve the Service Host but programmatically configure port

    HostInfo hostInfo = HostInfo.resolver()
                                .portResolver(Port(8080))
                                .resolve();
                                
    // Statically configure both the host and the port
    
    HostInfo hostInfo = HostInfo.resolver()
                                .hostResolver(Host("10.1.2.3"))
                                .portResolver(Port(8080))
                                .resolve();

    // Set a custom host provider
    HostInfo hostInfo = HostInfo.resolver()
                                .hostResolver(SpecialSnowflakeHostResolver())
                                .resolve()
    
    // Set a custom provider chain for ports (similar one would exist for hostes).
    HostInfo hostInfo = HostInfo.resolver()
                                .portProviderChain([
                                  EnvironmentVariablePort("PORT0"),
                                  CustomAPIPortResolver(),
                                ])
                                .resolve()

*/

namespace daas {

  class HostInfo 
  {
    static HostInfoResolver DEFAULT_RESOLVER = providerResolver();
  
    String host = null;
    int port    = null;
    
    HostInfo(String host, int port) 
    {
      self.host = host;
      self.port = port;
    }
  
    static HostInfo resolve() 
    {
      return DEFAULT_RESOLVER.resolve();
    }
  
    static HostInfoResolver providerResolver() 
    {
      String provider = EnvironmentVariable("DATAWIRE_HOST_PLATFORM").resolve();
      
      List<Resolver<String>> hostChain = [];
      List<Resolver<int>> portChain = [];
      
      if (provider == "EC2") 
      {
        // ... add specific provider resolver impls; e.g. query metadata service.
        hostChain = [];

        // ... add specific provider resolver impls; e.g. query metadata service.
        portChain = [];
      }
      
      if (provider == "ECS") 
      {
      
      }
      
      if (provider == "MARATHON") 
      {
      
      }
      
      if (provider == "SINGULARITY") 
      {
      
      }
      
      if (provider == "GCE") 
      {
      
      }
      
      if (provider == "KUBERNETES") 
      {
      
      }
      
      if (provider == "SWARM") 
      {
      
      }
      
      return HostInfoResolver().hostResolverChain(hostChain).portResolverChain(portChain);
    }
    
    static HostInfoResolver resolver() 
    {
      return HostInfoResolver();
    }
  }
  
  class HostInfoResolver extends Resolver<HostInfo> {
  
    List<Resolver<String>> hostResolvers = [null];
    List<Resolver<int>>    portResolvers = [null];
  
    HostInfo resolve() 
    {
      String host = resolveHost();
      int port = resolvePort();
      
      if (host == null || port == null) 
      {
        // TODO: What to do in the exceptional case without exceptions?
      }
      
      return new HostInfo(host, port);
    }
    
    HostInfoResolver portResolverChain(List<Resolver<int>> resolvers) 
    {
      portResolvers = resolvers;
      return self;
    }
    
    HostInfoResolver hostResolverChain(List<Resolver<String>> resolvers)
    {
      hostResolvers = resolvers;
      return self;
    }
    
    @doc("Set a custom port provider, for example, one that communicates with a custom metadata service.")
    @doc("The custom provider set by this method is always prepended to ANY chain. If a custom chain order is needed then use portResolverChain.")
    HostInfoResolver portResolver(Resolver<int> provider) 
    {
      
    
      return self;
    }
    
    @doc("Set a custom host provider, for example, one that communicates with a custom metadata service.")
    @doc("The custom provider set by this method is always prepended to ANY chain. If a custom chain order is needed then use hostResolverChain.")    
    HostInfoResolver hostResolver(Resolver<String> provider) 
    {
      return self;
    }
    
    String resolveHost() 
    {
      String result = null;
      int idx = 0;
      
      while(idx < hostResolvers.size()) 
      {
        Resolver<String> resolver = hostResolvers[0];
        if (resolver != null) 
        {
          result = resolver.resolve();
        }
        
        idx = idx + 1;
      }
      
      return result;
    }
    
    int resolvePort()
    {
      int result = null;
      int idx = 0;
      
      while(idx < portResolvers.size()) 
      {
        Resolver<int> resolver = portResolvers[0];
        if (resolver != null) 
        {
          result = resolver.resolve();
        }
        
        idx = idx + 1;
      }
      
      return result;
    }
  }
  
  interface Resolver<T> 
  {
    T resolve();
  }
  
  class ValueResolver<T> extends Resolver<T> 
  {
    T value;
    
    ValueResolver(T value)
    {
      self.value = value;
    }
    
    T get()
    {
      return value;
    }
  }
  
  class EnvironmentVariableResolver<T> extends Resolver<T> 
  {
    String variable;
    
    EnvironmentVariableResolver(String variable) 
    {
      self.variable = variable;
    }
  }
  
  class Port extends ValueResolver<int>
  {
    // DO NOT IMPLEMENT
  }
  
  class Host extends ValueResolver<String>
  {
    // DO NOT IMPLEMENT
  }
  
  class EnvironmentVariable extends EnvironmentVariableResolver<String>
  {    
    EnvironmentVariable(String variable) 
    {
      super(variable);
    }
    
    String get() 
    {
      // TODO: Insert quark code to resolve an environment variable. Would love to see this as a built-in or keyword (e.g. env("NAME", "DEFAULT"))
      return null;
    }    
  }
  
  class EnvironmentVariablePort extends EnvironmentVariableResolver<int> 
  {
    int get() 
    {
      // TODO: Quark support for environment variables
      // TODO: What to do on error case where var is not castable to int?
      return null;
    }
  }
  
  class EnvironmentVariableHost extends EnvironmentVariableResolver<String> 
  {
    String get() 
    {
      // TODO: Quark support for environment variables
      return null;
    }
  }
}