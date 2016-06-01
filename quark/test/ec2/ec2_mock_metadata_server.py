#!/usr/bin/env python

import logging

from BaseHTTPServer import HTTPServer
from BaseHTTPServer import BaseHTTPRequestHandler

ch = logging.StreamHandler()
ch.setLevel(logging.INFO)
ch.setFormatter(logging.Formatter('-->  %(levelname)s: %(name)s - %(asctime)s - %(message)s'))


class Ec2MockMetaDataServer(BaseHTTPRequestHandler):

    def do_GET(self):
        if self.path == "/latest/meta-data/local-hostname":
            self.send_response(200)
            self.end_headers()
            return "INTERNAL_HOSTNAME"
        if self.path == "/latest/meta-data/public-hostname":
            self.send_response(200)
            self.end_headers()
            return "PUBLIC_HOSTNAME"
        else:
            self.send_response(404)


def run():
    logger.setLevel('INFO')

    server = HTTPServer("localhost", 9000, Ec2MockMetaDataServer)
    server.serve_forever()


if __name__ == "__main__":
    run()
