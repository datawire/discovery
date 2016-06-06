This example illustrates how to take the hello world example from the
most popular python http client library (requests) along with the
hello world example from one of the most popular python server
libraries (flask) and modify both the client and server to use
datawire's service discovery and client side load balancing.

In order to run this you will need the requests and flask library:

    pip install requests flask

You will also need the discovery and introspection APIs:

    quark install --python ../../quark/discovery-2.0.0.q ../../quark/intro.q

The hackish way to get this running:

- fire up the Dockerized discoball (cf ../README.md)
- use `docker-machine env default` to get what the host sees as the IP address of the discoball
- edit `client` and `server` to connect to the discoball by IP address
- run the server:

        DATAWIRE_TOKEN=fakeToken DATAWIRE_ROUTABLE_HOST=127.0.0.1 python server

- run the client:

        DATAWIRE_TOKEN=fakeToken python client
