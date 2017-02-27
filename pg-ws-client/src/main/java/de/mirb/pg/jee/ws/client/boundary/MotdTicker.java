package de.mirb.pg.jee.ws.client.boundary;

import de.mirb.pg.jee.ws.client.control.MotdTickerEndpoint;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 */
@Startup
@Singleton
public class MotdTicker {

  private WebSocketContainer container;

  private static final Logger LOG = Logger.getLogger(MotdTicker.class.getName());
  private Endpoint endpoint;

  public MotdTicker() {
    LOG.setLevel(Level.ALL);
  }

  @PostConstruct
  public void connect() {
    LOG.info("Connect...");
    ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
    container = ContainerProvider.getWebSocketContainer();

    try {
      URI uri = new URI("ws://localhost:8080/pg-ws-server/motd");
      LOG.info("Connect to uri (" + uri.toString() + ")...");
      endpoint = new MotdTickerEndpoint();
      container.connectToServer(endpoint, cec, uri);
      LOG.info("Connection successful.");
    } catch (URISyntaxException | DeploymentException | IOException e) {
      e.printStackTrace();
      LOG.warning("Connection failed with message: " + e.getMessage() );
    }
  }
}
