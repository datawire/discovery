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
                                .portProvider(new StaticPortProvider(8080))
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
