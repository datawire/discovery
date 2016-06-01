import logging
logging.basicConfig(level=logging.DEBUG)
from util import Platform
host = Platform.getRoutableHost()
print("Host = " + host)
