This example illustrates how to take the hello world example from the
most popular python http client library (requests) along with the
hello world example from one of the most popular python server
libraries (flask) and modify both the client and server to use
datawire's service discovery and client side load balancing.

In order to run this you will need the requests and flask library:

    pip install requests flask

You will also need the discovery and introspection APIs:

    quark install --python ../../quark/discovery-2.0.0.q ../../quark/intro.q
