package de.mirb.pg.jee.ws.server.boundary;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.RemoteEndpoint;
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
@ServerEndpoint("/ws/bounce/text")
public class TextBounceEndpoint {

  private static final Logger LOG = LoggerFactory.getLogger(TextBounceEndpoint.class.getName());

  private Session session;

  @OnOpen
  public void open(Session session) {
    LOG.info("Open session ({})...", session.getId());
    this.session = session;
  }

  @OnClose
  public void close(Session session) {
    LOG.info("OnClose session ({})...", session.getId());
    close();
  }

  @OnError
  public void onError(Throwable error) {
    LOG.error("Got an error: {}", error.getMessage());
  }

  @OnMessage
  public void handleMessage(String message, Session session) {
    LOG.info("Received text message [{}]...", message);
    sendResponse("BOUNCE", message);
//    close();
  }

  private void close() {
    if (isSessionReady()) {
      LOG.info("Close before ready session ({})...", session.getId());
      try {
        this.session.close();
      } catch (IOException e) {
        e.printStackTrace();
        LOG.error("Exception occurred: " + e.getMessage(), e);
      }
    } else {
      LOG.info("Found not ready session for close attempt.");
    }
    this.session = null;
  }

  public void sendResponse(String content, String postfix) {
    if (isSessionReady()) {
      try {
        final RemoteEndpoint.Basic remote = session.getBasicRemote();
        final String response = content + "::" + postfix;
        remote.sendText(response);
        LOG.info("Send response [{}]...", response);
      } catch (IOException e) {
        //we ignore this
        e.printStackTrace();
        LOG.error("Exception occurred: " + e.getMessage(), e);
      }
    }
  }

  private boolean isSessionReady() {
    return session != null && session.isOpen();
  }
}
