package daas 0.1.0;

/*

  HostInfo Resolution
  -------------------

  Two pieces of information need to be determined! The first it the host
  address which is dynamically allocated by the infrastructure service.

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

    // Simplest usage assumes $HOST_PROVIDER environment variable is set.
    
    HostInfo hostInfo = HostInfo.resolve();

    hostInfo.address(); // returns the host IP or DNS name as a String
    hostInfo.port();    // returns the service port as an Integer 
    
    // Use an alternative to the $HOST_PROVIDER variable.

    HostInfo hostInfo = HostInfo.resolve("SNOWFLAKE_HOST_PROVIDER")
    
    // Dynamically resolve the Service Host but programmatically configure port

    HostInfo hostInfo = HostInfo.resolver()
                                .portProvider(new Port(8080))
                                .resolve();
                                
    // Statically configure both the host and the port
    
    HostInfo hostInfo = HostInfo.resolver()
                                .addressProvider(new Address("10.1.2.3"))
                                .portProvider(new Port(8080))
                                .resolve();

    // Set a custom address provider
    HostInfo hostInfo = HostInfo.resolver()
                                .addressProvider(new SpecialSnowflakeAddressResolver())
                                .resolve()
    
    // Set a custom provider chain for ports (similar one would exist for addresses).
    HostInfo hostInfo = HostInfo.resolver()
                                .portProviderChain([
                                  new EnvironmentVariablePortResolver("PORT0"),
                                  new CustomAPIPortResolver(),
                                ])
                                .resolve()

*/

namespace daas {

  class HostInfo 
  {
    static HostInfoResolver DEFAULT_RESOLVER = HostInfoResolver();
  
    String address = null;
    int port = null;
    
    HostInfo(String address, int port) 
    {
      self.address = address;
      self.port = port;
    }
  
    static HostInfo resolve() 
    {
      return DEFAULT_RESOLVER.resolve();
    }
    
    static HostInfoResolver resolver() 
    {
      return HostInfoResolver();
    }
  }
  
  class HostInfoResolver extends Resolver<HostInfo> {
  
    List<Resolver<String>> addressResolvers = [null];
    List<Resolver<int>>    portResolvers = [null];
  
    HostInfo resolve() 
    {
      String address = resolveAddress();
      int port = resolvePort();
      
      if (address == null || port == null) 
      {
        // TODO: What to do in the exceptional case without exceptions?
      }
      
      return new HostInfo(address, port);
    }
    
    HostInfoResolver portProviderChain(List<Resolver<int>> resolvers) 
    {
      portResolvers = resolvers;
      return self;
    }
    
    HostInfoResolver addressProviderChain(List<Resolver<String>> resolvers)
    {
      addressResolvers = resolvers;
      return self;
    }
    
    HostInfoResolver portProvider(Resolver<int> provider) 
    {
      return self;
    }
    
    HostInfoResolver addressProvider(Resolver<String> provider) 
    {
      return self;
    }
    
    String resolveAddress() 
    {
      String result = null;
      int idx = 0;
      
      while(idx < addressResolvers.size()) 
      {
        Resolver<String> resolver = addressResolvers[0];
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
      // TODO: What to do on error case where var is not castable to int?
      return null;
    }
  }
}