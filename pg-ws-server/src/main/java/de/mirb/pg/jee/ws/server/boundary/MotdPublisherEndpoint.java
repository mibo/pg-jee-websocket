package de.mirb.pg.jee.ws.server.boundary;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connect to endpoint via <code>ws://localhost:8080/pg-ws-server/motd</code> (when started with default Payara)
 * and a generic Web Socket Client.
 */
@Startup
@Singleton
@ServerEndpoint("/motd")
public class MotdPublisherEndpoint {

  private static final Logger LOG = LoggerFactory.getLogger(MotdPublisherEndpoint.class.getName());

  private Session session;
  private String messageOfTheDay = "Using WebSockets @JEE";
  private static final SimpleDateFormat SDF = new SimpleDateFormat("EEEE (HH:mm:ss.SSS)");

  @OnOpen
  public void open(Session session) {
    LOG.info("Open session ({})...", session.getId());
    this.session = session;
  }

  @OnClose
  public void close(Session session) {
    LOG.info("Close session ({})...", session.getId());
    this.session = null;
  }

  @OnError
  public void onError(Throwable error) {
    LOG.error("Got an error: {}", error.getMessage());
  }

  public void publishMotd(String motd) {
    if(isSessionReady()) {
      try {
        session.getBasicRemote().sendText(motd);
      } catch (IOException e) {
        LOG.error("Error during MOTD publish: " + e.getMessage(), e);
      }
    }
  }

//  @Schedule(hour = "*", minute = "*", second = "*/5")
  public void publish() {
    String date = SDF.format(new Date());
    String motdToPub = "Message of " + date + ": " + messageOfTheDay;
    publishMotd(motdToPub);
    LOG.trace("Published: {}", motdToPub);
  }

  private boolean isSessionReady() {
    return session != null && session.isOpen();
  }
}
