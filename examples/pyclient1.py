# Python Hello DWC client

import sys

# Import Discovery
from daas import Discovery

# This is our function that makes a call to our service.
def call_service(endpoint, *args, **kwargs):
  pass

######## MAINLINE

def main():
  # Where can we find the "my_service" service? As called here, resolve will look
  # up the token for us, and will block until we get something legitimate.

  endpoint = Discovery.resolve("my_service")

  # Make the call.
  call_service(endpoint)

if __name__ == '__main__':
  main()
  
