/*
 * Copyright 2015 Datawire. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.datawire.hub.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.datawire.hub.agent.config.HubAgentConfiguration;
import io.datawire.hub.agent.model.LaunchHub;
import io.datawire.hub.config.ConfigurationException;
import io.datawire.hub.config.ConfigurationFactory;
import io.datawire.hub.config.FileConfigurationSource;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static spark.Spark.*;

/**
 * A microservice that is responsible for configuring and starting a tenant's Hub instance.
 *
 * @author plombardi@datawire.io
 * @since 1.0
 */


public class HubAgent {

  private final static Logger LOG = LoggerFactory.getLogger(HubAgent.class);

  private final static String JSON_CONTENT_TYPE = "application/json; charset=utf-8";

  private final ObjectMapper mapper;
  private final HubAgentConfiguration config;

  HubAgent(ObjectMapper mapper, HubAgentConfiguration config) {
    this.mapper = mapper;
    this.config = config;
  }

  public void listen() {
    ipAddress(config.getListener().getBindAddress());
    port(config.getListener().getPort());

    get("/health", (req, resp) -> "OK");

    post("/hub", JSON_CONTENT_TYPE, (req, resp) -> {
      LaunchHub launchHub = mapper.readValue(req.body(), LaunchHub.class);
      LOG.info("Received launch request (tenant: {})", launchHub.getTenantId());

      // TODO (plombardi):
      // 1. Rewrite the Hub Server configuration with the tenant ID in the LaunchHub object.
      // 2. Actually Launch the Hubs
      configureHub(launchHub.getTenantId());
      launchHub();

      resp.status(204);
      return resp;
    });
  }

  private void configureHub(String tenantId) {
    LOG.debug("Configuring Hub for tenant");
  }

  private void launchHub() {
    LOG.debug("Launching Hub");
  }

  private static HubAgent buildAgent(Namespace args) throws ConfigurationException, IOException {
    ObjectMapper mapper = new ObjectMapper();
    ConfigurationFactory<HubAgentConfiguration> factory = new ConfigurationFactory<>(HubAgentConfiguration.class, mapper);
    HubAgentConfiguration configuration = factory.build(new FileConfigurationSource((File) args.get("config")));
    return new HubAgent(mapper, configuration);
  }

  private static ArgumentParser buildArgParser(String version) {
    ArgumentParser parser = ArgumentParsers.newArgumentParser("hub-agent")
        .description("Hub launch and configuration agent")
        .version("${prog} $version");

    parser.addArgument("--version").action(Arguments.version());
    parser.addArgument("config").type(Arguments.fileType().verifyCanRead());
    return parser;
  }

  public static void main(String... arguments) throws Exception {
    ArgumentParser parser = buildArgParser("VERSION TODO");
    try {
      Namespace args = parser.parseArgs(arguments);
      HubAgent agent = buildAgent(args);
      agent.listen();
    } catch (ArgumentParserException ex) {
      parser.handleError(ex);
    }
  }
}
