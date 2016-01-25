/*
 * Copyright 2016 Datawire. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * The official Datawire Connect implementation for Datawire Hub.
 */

@version("1.0.0")
package hub {

  @doc("Contains Hub domain model interfaces and classes.")
  package model {
    @doc("Interface for health checks")
    interface HealthCheck {
      bool check();
    }
  }

  package message {

    @doc("The base message type")
    class HubMessage {

      @doc("ID of the message; Not used currently")
      int id = 0;

      @doc("The message type")
      String type = "";

      @doc("The time the message was created")
      long timestamp = 0;

      HubMessage(String type) {
        self.type = type;
        self.timestamp = now();
      }

      void dispatch(HubHandler handler) {
        handler.onHubMessage(self);
      }

      JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json["type"] = self.type;
        json["time"] = self.timestamp;
        return json;
      }

      String toString() {
        return "HubMessage(id=" + id.toString() + ", type=" + type + ", timestamp=" + timestamp.toString() + ")"
      }
    }

    interface MessageFactory {
      message.HubMessage build(String type, JSONObject json);
    }

    class DefaultMessageFactory extends Factory {
      message.HubMessage build(String type, JSONObject json) {
          if (type == "connected")    { return new message.Connected(); }
          if (type == "disconnected") { return new message.Disconnected(); }
          if (type == "sync")         { return new message.Synchronize(json); }

          return new message.HubMessage(type);
      }
    }
  }

  @doc("Contains Hub client interfaces and classes")
  package client {

    class Hub {
      bool    secure = true;

      String  host = "";
      String  port = 443;

      String  token = "";

      HubOptions(String host, int port, String token) {
        this.token = token;
      }

      String buildUrl() {
        String scheme = "https";
        int port = self.port;

        if (secure) {
          if (port == null || port < 1 || port > 65535) {
            port = 443;
          }
        } else {
          scheme = "http";
          if (port == null || port < 1 || port > 65535) {
            port = 80;
          }
        }

        return scheme + "://" + host + ":" + post.toString();
      }
    }

    class HubConnection extends WSHandler, HTTPHandler {

      Runtime         runtime;
      WebSocket       socket;

      GatewayOptions  gateway;

      HubConnection(Runtime runtime, GatewayOptions gateway) {
        this.runtime = runtime;
        this.gateway = gateway;
      }

      void connect() {

      }

      void disconnect() {
        if (socket != null) {
          socket.close();
        }
      }

      bool isConnected() {
        return socket != null;
      }

      void onWSConnected(WebSocket socket) {
        socket = socket;
        message.HubMessage msg = self.buildMessageOfType("connected", new JSONObject());
        msg.dispatch(handler);
      }

      void onWSClosed(WebSocket socket) {
        socket = null;
        message.HubMessage msg = self.buildMessageOfType("disconnected", new JSONObject());
        msg.dispatch(handler);
      }
    }

    class GatewayOptions {
      bool    secure = true;
      bool    authenticate = true;

      String  gatewayHost = "hub-gw.datawire.io";
      int     gatewayPort = 443;
      String  gatewayConnectorPath = "/v1/connect"

      String  token = "";

      GatewayOptions(String token) {
        this.token = token;
      }

      String buildUrl() {
        String scheme = "https";
        int gatewayPort = self.gatewayPort;

        if (secure) {
          if (gatewayPort == null || gatewayPort < 1 || gatewayPort > 65535) {
            gatewayPort = 443;
          }
        } else {
          scheme = "http";
          if (gatewayPort == null || gatewayPort < 1 || gatewayPort > 65535) {
            gatewayPort = 80;
          }
        }

        return scheme + "://" + gatewayHost + ":" + gatewayPort.toString() + gatewayConnectorPath;
      }
    }
  }
}