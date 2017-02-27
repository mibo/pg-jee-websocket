package de.mirb.pg.jee.ws.server.boundary;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Connect to endpoint via <code>ws://localhost:8080/pg-ws-server/motd</code> (when started with default Payara)
 * and a generic Web Socket Client.
 */
@Startup
@Singleton
@ServerEndpoint("/motd")
public class MotdPublisherEndpoint {

  private static final Logger LOG = Logger.getLogger(MotdPublisherEndpoint.class.getName());

  public MotdPublisherEndpoint() {
    LOG.setLevel(Level.ALL);
  }

  private Session session;
  private String messageOfTheDay = "Using WebSockets @JEE";
  private static final SimpleDateFormat SDF = new SimpleDateFormat("EEEE (HH:mm:ss.SSS)");

  @OnOpen
  public void open(Session session) {
    this.session = session;
  }

  public void publishMotd(String motd) {
    if(isSessionReady()) {
      try {
        session.getBasicRemote().sendText(motd);
      } catch (IOException e) {
        LOG.log(Level.SEVERE, "Error during MOTD publish.", e);
      }
    }
  }

  @Schedule(hour = "*", minute = "*", second = "*/5")
  public void publish() {
    String date = SDF.format(new Date());
    String motdToPub = "Message of " + date + ": " + messageOfTheDay;
    publishMotd(motdToPub);
    LOG.log(Level.INFO, "Published: " + motdToPub);
  }

  private boolean isSessionReady() {
    return session != null && session.isOpen();
  }
}
