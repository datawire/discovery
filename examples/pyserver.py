# Python Hello DWC server

import sys

# Import Discovery
from daas import Discovery

# This is our service.
def start_providing_service(port):
  pass

######## MAINLINE

def main():
  # This fires up whatever is needed to actually respond to requests.
  # Assume it's long-running.
  start_providing_service(8910)

  # Register with the discoball. This way of calling register will look
  # up the token in your default Datawire state. You should be able to pass
  # in a token directly, or possibly a Datawire state object, etc.
  Discovery.register("my_service", "http://localhost:8910/my_service")

  # ...done.

if __name__ == '__main__':
  main()
  
