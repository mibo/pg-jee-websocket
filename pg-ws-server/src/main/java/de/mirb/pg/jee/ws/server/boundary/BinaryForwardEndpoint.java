package de.mirb.pg.jee.ws.server.boundary;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
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
 * Connect to endpoint via <code>ws://localhost:8080/pg-ws-server/ws/bin</code> (when started with default Payara)
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
  private SocketChannel socket;

  @OnOpen
  public void open(Session session) {
    LOG.info("Open session ({})...", session.getId());
    this.session = session;
//    session.getContainer().setDefaultMaxBinaryMessageBufferSize(10);
    // start each session with a new socket connection
    this.socket = null;
  }

  @OnClose
  public void close(Session session) {
    LOG.info("Close session ({})...", session.getId());
    close();
  }

  @OnError
  public void onError(Throwable error) {
    LOG.error("Got an error: {}", error.getMessage());
    close();
  }

  @OnMessage
  public void handleMessage(ByteBuffer message, Session session) {
    LOG.info("Received binary message...");
    String messageContent = readAsString(message);
    LOG.info("Content (size={}bytes): {}", messageContent.length(), messageContent);
    forwardSc(message);
//    response();
//    close();
  }

  private String readAsString(ByteBuffer byteBuffer) {
    ByteBuffer b = byteBuffer.duplicate();
    byte[] tmp = new byte[b.limit()];
    b.get(tmp);
    return new String(tmp, StandardCharsets.US_ASCII);
  }

  private void close() {
    if (isSessionReady()) {
      try {
        this.session.close();
      } catch (IOException e) {
        e.printStackTrace();
        LOG.error("Failed to close session: " + e.getMessage(), e);
      } finally {
        this.session = null;
      }
    }
    //
    if(socket != null) {
      try {
        socket.close();
      } catch (IOException e) {
        e.printStackTrace();
        LOG.error("Failed to close socket: " + e.getMessage(), e);
      } finally {
        socket = null;
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
      LOG.info("Start forward...");
      SocketChannel socket = grantSocketChannel();

      while(content.hasRemaining()) {
        LOG.info("Write content...");
        socket.write(content);
      }

      ByteBuffer read = ByteBuffer.allocate(1024*64);
      LOG.info("Read response...");
      int readCount = socket.read(read);
      if(readCount == 0) {
        LOG.info("Re-Read...");
        silentSleep(100);
        // handle read more then buffer size
        readCount = socket.read(read);
      }
      LOG.info("Read ({}bytes)...", readCount);
      LOG.info("Write ws response...");
      wsResponse(read);
      LOG.info("...wrote ws response.");
    } catch (IOException e) {
      e.printStackTrace();
      LOG.error("Exception occurred: " + e.getMessage(), e);
    }
  }

  private void silentSleep(int sleepMs) {
    try {
      Thread.sleep(sleepMs);
    } catch (InterruptedException e) {
      //e.printStackTrace();
    }
  }

  private synchronized SocketChannel grantSocketChannel() throws IOException {
    if(socket == null) {
      LOG.info("Create new socket...");
      socket = SocketChannel.open();
      socket.configureBlocking(false);
      SocketAddress sa = new InetSocketAddress(FWD_SOCKET_HOST, FWD_SOCKET_PORT);
      LOG.info("Connect to ({}:{})...", FWD_SOCKET_HOST, FWD_SOCKET_PORT);
      socket.connect(sa);
      while(!socket.finishConnect()) {
        silentSleep(100);
      }
      LOG.info("Successful connected to ({}:{})...", FWD_SOCKET_HOST, FWD_SOCKET_PORT);
    }
    return socket;
  }

  public void wsResponse(ByteBuffer buffer) {
    wsSyncResponse(buffer);
//    wsAsyncResponse(buffer);
  }

  public void wsSyncResponse(ByteBuffer buffer) {
    if (isSessionReady()) {
      try {
        buffer.flip();
        LOG.info("Send response (content='{}')...", readAsString(buffer));
        final RemoteEndpoint.Basic remote = session.getBasicRemote();
        remote.sendBinary(buffer);
        LOG.info("...successful send response.");
      } catch (IOException e) {
        //we ignore this
        e.printStackTrace();
        LOG.error("Exception occurred: " + e.getMessage(), e);
      }
    }
  }

  public void wsAsyncResponse(ByteBuffer buffer) {
    if (isSessionReady()) {
      buffer.flip();
      LOG.info("Send async response (content='{}')...", readAsString(buffer));
      //        final RemoteEndpoint.Basic remote = session.getBasicRemote();
      RemoteEndpoint.Async asremote = session.getAsyncRemote();
      asremote.sendBinary(buffer);
      //        remote.sendBinary(buffer);
      LOG.info("...successful send response.");
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
