package de.mirb.pg.jee.ws.client.boundary;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 */
@Path("/motds")
public class MotdResource {

  @XmlRootElement
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class MotdTickerResponse {
    int amount;
    List<String> messages;

    public MotdTickerResponse() { }

    public MotdTickerResponse(List<String> messages) {
      this.amount = messages.size();
      this.messages = messages;
    }
  }

  @Inject MotdTicker ticker;

  @GET
  @Path("/latest")
  @Produces(MediaType.APPLICATION_JSON)
  public String getLatestMotd() {
    Optional<String> lastmotd = ticker.getLastMessage();
    return lastmotd.orElse("No MOTD available");
  }

  @GET
  @Path("/")
  @Produces(MediaType.APPLICATION_JSON)
  public MotdTickerResponse getMotds() {
    return new MotdTickerResponse(ticker.getAllMessages());
  }
}
