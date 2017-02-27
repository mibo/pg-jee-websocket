package de.mirb.pg.jee.ws.client.control;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

/**
 */
public class MotdTickerEndpoint extends Endpoint {
  private Session session;
  private String lastMessage;
  private Stack<String> messageStack = new Stack<>();

  private static final Logger LOG = Logger.getLogger(MotdTickerEndpoint.class.getName());

  public MotdTickerEndpoint() {
    LOG.setLevel(Level.ALL);
  }


  @Override
  public void onOpen(Session session, EndpointConfig endpointConfig) {
    LOG.log(Level.INFO, "Open endpoint...");
    this.session = session;

    this.session.addMessageHandler(new MessageHandler.Whole<String>() {
      @Override
      public void onMessage(String message) {
        lastMessage = message;
        messageStack.add(message);
        final String logMessage = "Received MOTD [" + lastMessage + "] (count: " + messageStack.size() + ").";
        LOG.log(Level.INFO, logMessage);
      }
    });

    /* XXX: Lambda does not work
    Caused by: java.lang.NullPointerException
        at org.apache.tomcat.websocket.Util.getGenericType(Util.java:218)
        at org.apache.tomcat.websocket.Util.getMessageType(Util.java:171)
        at org.apache.tomcat.websocket.WsSession.addMessageHandler(WsSession.java:185)
        at de.mirb.pg.jee.ws.client.control.MotdTickerEndpoint.onOpen(MotdTickerEndpoint.java:42)
     */
//    this.session.addMessageHandler((MessageHandler.Whole<String>) message -> {
//      lastMessage = message;
//      messageStack.add(message);
//      final String logMessage = "Received MOTD [" + lastMessage + "] (count: " + messageStack.size() + ").";
//      LOG.log(Level.INFO, logMessage);
//    });
  }


  public Optional<String> getLastMessage() {
    return Optional.ofNullable(lastMessage);
  }

  public int getReceivedMessageCount() {
    return messageStack.size();
  }

  public List<String> getAllMessages() {
    return Collections.list(messageStack.elements());
  }
}
