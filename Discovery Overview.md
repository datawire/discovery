Datawire Connect: Discovery Design Overview
===========================================

One of the foundational building blocks of Datawire Connect is "discovery as a service": an easy way for providers to register that they exist, and for consumers to find providers for the services they need. Discovery as a service comprises several things all working together:

- The **Discovery Service** (the _discoball_) handles tracking which services are running.
    - There will always be more than one discoball instance.
    - The discoball instances share a distributed map of which services are registered.
    - Discoballs are _not_ strongly consistent; they offer eventual consistency.
- A **Discovery Client** (a _disco client_) connects to the discoball to exchange information about running services.
    - disco clients will generally use a Datawire-supplied library to manage communications with the discoball and client-side load balancing
    - communications between the discoball and the disco client happens primarily over websockets
- **Service Providers** are disco clients that actually provide microservices that others can use.
    - A given provider must _register_ itself with the discoball.
    - A given service may (and usually will) have multiple providers.
    - The discoball calls a collection of providers of the same service a **cluster**.
    - Each provider is reachable at a specific **node** within the cluster.
    - Nodes are identified by the base URI of the service provided at the node.
    - A given service may (and often will) make multiple **resources** available for use.
    - Resources are identified by adding `query` information to the base URI of the node.
- **Service Consumers** are disco clients that want to use microservices.
    - A given consumer must use the discoball to _resolve_ a service name into the URI of a node providing the service.

{ need an example here }

In many systems, the discoball would contain most of the complexity and the disco client would be fairly dumb. In Datawire Connect, these roles are reversed: the discoball has no persistent state, and the disco client manages a fairly complex set of behaviors. This is a design decision motivated by the need to allow the disco client to make good decisions even when the discoball isn't present: the discoball could restart at any time, for example, or temporarily be on the wrong side of a network partition. Disco clients have to recover gracefully from these scenarios and more, both in that they have to continue behaving reasonably themselves during the time that the discoball is unreachable, and in that they have to be polite to the discoball during recovery:

- No matter how many disco clients there are, they shouldn't stomp the discoball to death.
- A disco client that has valid data should be able to use that data even if it can't talk to the discoball.
- A disco client shouldn't use bad data when the discoball has better data.
- A disco client must gracefully handle the discoball vanishing and returning.
- A disco client mustn't assume that the discoball's data are always perfect.

Some of these are clearly contradictory. Sadly, this is the nature of disco.

Normal Operations
-----------------

The normal state of operations is that multiple discoballs are running and multiple disco clients have websocket connections to the discoballs. The set of connected clients is constantly in flux as new clients arrive and old clients drop off, but for the moment we assume that the set of services being provided isn't changing: for every service that the system as whole expects to be running, a cluster is registered with the discoballs.

When a new client connects, both the client and the discoball MUST send `OPEN` messages to each other. When the connection is closed, the party closing the connection MUST send a `CLOSE` message describing why the connection is closing. The connection SHOULD be closed immediately after sending the `CLOSE` message; the second party MUST NOT attempt to reply to `CLOSE`.

### Discoball during normal operations

After `OPEN` is sent, the discoball MUST send the current set of active providers to the new client, with one `ACTIVE` message for each active node. If a client connects before any providers have registered themselves, the discoball MUST send `CLEAR` with an empty node list to indicate that the table is completely empty.

Thereafter, the discoball MUST send updates to the client:

- When a new node appears, the discoball MUST send an `ACTIVE` message for that node to the client
- When a node unregisters itself, the discoball MUST send a `CLEAR` message for that node to the client
- When a node is unregistered because it reached the inactivity timeout, the discoball MUST send an `EXPIRE` message for that node to the client

If the discoball needs to shut down for some reason, it MUST send `CLOSE` to all connected clients (usually `CLOSE: Parting Friends`) before ceasing operations.

### Client during normal operations

After `OPEN` is sent, the client MAY register services by sending one `ACTIVE` message to the discoball for each service it provides. If the client registers a service, it MUST continue sending `ACTIVE` at intervals less than the discoball's inactivity timeout in order to keep its service registered.

The client MUST pay attention to `ACTIVE`, `CLEAR`, and `EXPIRE` messages from the discoball to maintain its own copy of the routing table. When the client wants to resolve a service, it MUST use the local table rather than trying to ask the discoball directly. If the discoball hasn't yet sent a routing table, the client MAY wait for routing table updates before finishing resolution.

The client MUST send `CLOSE` before disconnecting from the discoball.

**NOTE WELL**: if the provider set is stable, a consumer will receive _no messages_. This is not a bug; it simply reflects that no changes to the consumer's tables are needed.

Convergence
-----------

The discoball could restart at any time for any of a variety of reasons (upgrade, crash, whatever). When this happens, it comes up believing that no providers exist at all, and waits for providers to reconnect and re-register their services.

There can be a great many providers already running when the discoball restarts. Not all of them can - or should - re-register immediately: there will be a delay during which the discoball has only partial information because it's still rebuilding its routing table. This is the _convergence period_; we expect that it will probably be small numbers of minutes. 

A client that was connected to the discoball before the restart SHOULD be careful not to discard valid routing information from before the restart until the connection from the discoball is known to be good _and_ the convergence period ends:

- When the discoball disconnects, the client MUST:
  - Remember that the system is now converging.
  - Make certain that no convergence timer is running.
- After a new connection to the discoball is established _and_ a routing message (`ACTIVE` or `CLEAR`) is received from the discoball, the client MUST:
  - Mark all entries in the local routing table as 'old'.
  - Start the convergence timer to fire after the convergence period ends.
    - Until a routing message is received, the client can't be sure that the discoball is functioning.
    - Marking the entries as old is important once convergence ends.
- If the new connection to the discoball fails before the convergence period ends, the client MUST:
  - Clear the 'old' marker on all entries in the local routing table.
  - Remember that the system is no longer converging.
  - Clear the convergence timer.
	- The discoball will only disconnect if something has gone badly wrong, and the system will need to re-converge.
- If a `CLEAR` message with no node information arrives from the discoball, the client MUST ignore it.
  - This message indicates that the discoball has no routing entries yet.
  - Any routing information in the client's local table should be preserved rather than thrown away.
- If an `ACTIVE` message arrives from the discoball, the client MUST ensure that the node arriving in the `ACTIVE` message is not marked 'old'.
  - Any `ACTIVE` message for a node already in the local routing table indicates that that route is known good and can be trusted.
- When the convergence timer fires, the client MUST:
  - expire all nodes still marked 'old'
  - remember that the system has finished converging.
    - Note that the convergence timer can only fire if we still have a good connection to the discoball.

Note that these behaviors apply to _any_ disco client, since all clients are assumed to be consumers at present.

### Cold start

Note that the convergence algorithm also works in the case of cold starting a client: the newly-started client won't have anything in its local routing table, so there will be nothing to mark 'old'.

Likewise, if a discoball is being started in a completely empty environment (e.g. for development), extant clients will effectively treat that as the system converging around the new discoball. This case is relatively unlikely. 

Messages
--------

### `OPEN`: announce connection metadata

`OPEN` announces information about the connection to the other party. Presently, this is limited to the protocol version the sender plans to speak. 

- The sender does NOT need to wait for the other party to respond to `OPEN`, but may immediately continue sending other messages.
- The recipient MUST NOT reply to `OPEN`: it is an announcement, not a negotiation.
- If the protocol version specified by `OPEN` is not acceptable to the recipient, the recipient MUST send a `CLOSE: Protocol Version Mismatch` message, then immediately close the connection.

### `CLOSE`: describe why the connection is closing

`CLOSE` MUST be sent before closing a connection. It carries a `type`, which MUST be one of the possibilities listed below, and some `text` which MAY have more details.

Valid `type`s at present are:

1. `Parting Friends`

`Parting Friends` indicates a normal end to the connection rather than an error. A client would send `Parting Friends` if it had done its job and was exiting; the discoball might send it when shutting down for an upgrade.

2. `Protocol Version Mismatch`

`Protocol Version Mismatch` indicates that the other party announced that it was going to speak a protocol version that the sender of `CLOSE` is unwilling to speak. The `text` MAY indicate a version that the sender is willing to speak.

3. `Panic at the Disco`

`Panic at the Disco` indicates that something unexpected has gone wrong. The `text` field SHOULD contain a human-readable description of what the unexpected thing was.

### `ACTIVE`: a node is actively providing a service

`ACTIVE` is a note that a node with a given `URI` is providing a given `version` of a given `service`. Note that the only the base URI is given: `ACTIVE` does not indicate which resources are available. 

- `service` is the name of the service.
- `version` is the version of the service. Services SHOULD use semantic versioning, though the discoball does not currently interpret the version beyond using string equality to count how many of which version are available.
- `URI` is the base URI for the service. Any syntactically-valid URI is acceptable: the discoball does not interpret the URI in any way.

A client sending `ACTIVE` to the discoball is registering a node that provides a service.

The discoball sending `ACTIVE` to a client is giving the client an updated route map, indicating that a node providing the service is available.

Note that providers are currently assumed to also be consumers. Therefore, a provider sending `ACTIVE` to the discoball should expect to receive a corresponding `ACTIVE` back at some point.

### `CLEAR`: a node is no longer providing its service

`CLEAR` is an indication that a node with a given `URI` is no longer providing the given `version` of the given `service`.

- `service` is the name of the service.
- `version` is the version of the service. Services SHOULD use semantic versioning, though the discoball does not currently interpret the version beyond using string equality to count how many of which version are available.
- `URI` is the base URI for the service. Any syntactically-valid URI is acceptable: the discoball does not interpret the URI in any way.

A client sending `CLEAR` to the discoball is indicating that its node is no longer operating.

The discoball sending `CLEAR` to a client is giving the client an updated route map, indicating that a node has become unavailable.

Note that providers are currently assumed to also be consumers. Therefore, a provider sending `CLEAR` to the discoball should expect to receive a corresponding `CLEAR` back at some point.

### `EXPIRE`: a node has stopped responding

`EXPIRE` is an indication that a node has stopped talking to the discoball entirely, and therefore the given `version` of the given `service` is no longer available at the given `URI`.

- `service` is the name of the service.
- `version` is the version of the service. Services SHOULD use semantic versioning, though the discoball does not currently interpret the version beyond using string equality to count how many of which version are available.
- `URI` is the base URI for the service. Any syntactically-valid URI is acceptable: the discoball does not interpret the URI in any way.

A client MUST NOT send `EXPIRE` to the discoball.

The discoball sending `EXPIRE` to a client is giving the client an updated route map, indicating that a node has become unavailable because it became unresponsive.

