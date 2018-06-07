package apps;

import com.google.common.collect.ImmutableMap;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import io.opentracing.Scope;
import io.opentracing.Tracer;
import lib.Tracing;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.Random;

public class Dasher extends Application<Configuration> {

  private final Random random = new Random();
  private final Tracer tracer;

  private Dasher(Tracer tracer) {
    this.tracer = tracer;
  }

  public static void main(String[] args) throws Exception {
    System.setProperty("dw.server.applicationConnectors[0].port", "8083");
    System.setProperty("dw.server.adminConnectors[0].port", "9083");
    Tracer tracer = Tracing.init("dasher");
    new Dasher(tracer).run(args);
  }

  @Override
  public void run(Configuration configuration, Environment environment) throws Exception {
    environment.jersey().register(new DasherResource());
    environment.getApplicationContext().setContextPath("/dasher");
  }

  @Path("/dasher/deliver")
  @Produces(MediaType.TEXT_PLAIN)
  public class DasherResource {

    @GET
    public String deliver(@QueryParam("foodItem") String foodItem, @Context HttpHeaders httpHeaders)
            throws InterruptedException {
      try (Scope scope = Tracing.startServerSpan(tracer, httpHeaders, "deliver")) {
        String customer = scope.span().getBaggageItem("customer");
        if (customer == null) {
          customer = "Unknown";
        }

        for (int i = 0; i < random.nextInt(10); i++) {
          System.out.println(String.format("%s out for delivery for customer: %s", foodItem, customer));
          Thread.sleep(1000);
        }

        String deliveryStatus = String.format("Delivered: %s for customer: %s", foodItem, customer);
        scope.span().log(ImmutableMap.of("event", "delivery-status", "value", deliveryStatus));
        return deliveryStatus;
      }
    }
  }
}
