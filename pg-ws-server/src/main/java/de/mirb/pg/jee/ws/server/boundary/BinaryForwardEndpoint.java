package de.mirb.pg.jee.ws.server.boundary;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

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
@ServerEndpoint("/ws/bin")
public class BinaryForwardEndpoint {

  private static final Logger LOG = LoggerFactory.getLogger(BinaryForwardEndpoint.class.getName());
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
  public void handleMessage(ByteBuffer message, Session session) {
    LOG.info("Received binary message...");
    String messageContent = new String(message.array());
    LOG.info(messageContent);
    forwardSc(message);
//    response();
    close();
  }

  private void close() {
    if (isSessionReady()) {
      try {
        this.session.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

//  private void forward(ByteBuffer content) {
//    try {
//      Socket s = new Socket("localhost", 35672);
//      WritableByteChannel out = Channels.newChannel(s.getOutputStream());
//      out.write(content);
//
//      ReadableByteChannel in = Channels.newChannel(s.getInputStream());
//      ByteBuffer read = ByteBuffer.allocate(1024);
//      int r = in.read(read);
//      while(r > 0) {
//        // handle read more then buffer size
//        r = in.read(read);
//      }
//      wsResponse(read);
//    } catch (IOException e) {
//      e.printStackTrace();
//    }
//  }

  private void forwardSc(ByteBuffer content) {
    try {
      SocketChannel socket = SocketChannel.open();
      socket.configureBlocking(false);
      Selector selector = Selector.open();
      final SelectionKey key = socket.register(selector, SelectionKey.OP_CONNECT, this);
      SocketAddress sa = new InetSocketAddress(FWD_SOCKET_HOST, FWD_SOCKET_PORT);
      socket.connect(sa);
      socket.finishConnect();
      socket.write(content);

      ByteBuffer read = ByteBuffer.allocate(1024);
      int r = socket.read(read);
      while(r > 0) {
        // handle read more then buffer size
        r = socket.read(read);
      }
      wsResponse(read);
    } catch (IOException e) {
      e.printStackTrace();
      LOG.error("Exception occurred: " + e.getMessage(), e);
    }
  }

  public void wsResponse(ByteBuffer buffer) {
    if (isSessionReady()) {
      try {
        buffer.flip();
        final RemoteEndpoint.Basic remote = session.getBasicRemote();
        remote.sendBinary(buffer);
        LOG.info("Send response...");
      } catch (IOException e) {
        //we ignore this
        e.printStackTrace();
        LOG.error("Exception occurred: " + e.getMessage(), e);
      }
    }
  }

  public void response() {
    if (isSessionReady()) {
      try {
        byte[] array = new byte[]{'A', 'M', 'Q', 'P', '3', '1', '1', '1'};
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.flip();
        final RemoteEndpoint.Basic remote = session.getBasicRemote();
        remote.sendBinary(buffer, true);

        LOG.info("Send response...");
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
