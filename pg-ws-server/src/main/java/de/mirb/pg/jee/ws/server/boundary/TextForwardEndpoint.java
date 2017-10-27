package de.mirb.pg.jee.ws.server.boundary;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
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
@ServerEndpoint("/ws/text")
public class TextForwardEndpoint {

  private static final Logger LOG = LoggerFactory.getLogger(TextForwardEndpoint.class.getName());
  public static final String FWD_SOCKET_HOST = "localhost";
  public static final int FWD_SOCKET_PORT = 35672;

  private Session session;

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

  @OnMessage
  public void handleMessage(String message, Session session) {
    LOG.info("Received text message [{}]...", message);
    forward(message);
//    response();
//    close();
  }

  private void close() {
    if (isSessionReady()) {
      try {
        this.session.close();
      } catch (IOException e) {
        e.printStackTrace();
        LOG.error("Exception occurred: " + e.getMessage(), e);
      }
    }
  }

  private void forward(String content) {
    try {
      ByteBuffer buffer = ByteBuffer.wrap(content.getBytes(StandardCharsets.ISO_8859_1));
      Socket s = new Socket(FWD_SOCKET_HOST, FWD_SOCKET_PORT);
      WritableByteChannel out = Channels.newChannel(s.getOutputStream());
      out.write(buffer);

      ReadableByteChannel in = Channels.newChannel(s.getInputStream());
      ByteBuffer read = ByteBuffer.allocate(1024);
      int r = in.read(read);
      while(r > 0) {
        // handle read more then buffer size
        r = in.read(read);
      }
      wsResponse("Response", new String(read.array()));
    } catch (IOException e) {
      e.printStackTrace();
      LOG.error("Exception occurred: " + e.getMessage(), e);
    }
  }


  public void wsResponse(String content, String postfix) {
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
