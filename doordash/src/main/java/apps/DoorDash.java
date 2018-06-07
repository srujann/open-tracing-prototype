package apps;

import com.google.common.collect.ImmutableMap;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import io.opentracing.Scope;
import io.opentracing.Tracer;
import lib.Tracing;
import okhttp3.OkHttpClient;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import static lib.Utils.getHttp;

public class DoorDash extends Application<Configuration> {

  private final OkHttpClient client;
  private final Tracer tracer;

  private DoorDash(Tracer tracer) {
    this.tracer = tracer;
    this.client = new OkHttpClient();
  }

  public static void main(String[] args) throws Exception {
    System.setProperty("dw.server.applicationConnectors[0].port", "8081");
    System.setProperty("dw.server.adminConnectors[0].port", "9081");
    Tracer tracer = Tracing.init("doordash");
    new DoorDash(tracer).run(args);
  }

  @Override
  public void run(Configuration configuration, Environment environment) throws Exception {
    environment.jersey().register(new DasherResource());
    environment.getApplicationContext().setContextPath("/doordash");
  }

  @Path("/doordash/order")
  @Produces(MediaType.TEXT_PLAIN)
  public class DasherResource {

    @GET
    public String orderAndDeliver(@QueryParam("foodItem") String foodItem,
                                  @QueryParam("customer") String customer,
                                  @Context HttpHeaders httpHeaders)
            throws InterruptedException {
      try (Scope scope = tracer.buildSpan("order-and-deliver").startActive(true)) {
        scope.span().setTag("foodItem", foodItem);
        scope.span().setBaggageItem("customer", customer);
        scope.span().log(ImmutableMap.of("event", "http-response-order", "value", orderFood(foodItem)));
        scope.span().log(ImmutableMap.of("event", "http-response-deliver", "value", deliverFood(foodItem)));
        return "Yay!!! Ordered and Delivered - foodItem: " + foodItem + " for customer: " + customer;
      }
    }

    private String orderFood(String foodItem) {
      try (Scope scope = tracer.buildSpan("orderFood").startActive(true)) {
        String orderStatus = getHttp(tracer, client, 8082,"restaurant/order", "foodItem", foodItem);
        scope.span().log(ImmutableMap.of("event", "order-status", "value", orderStatus));
        return orderStatus;
      }
    }

    private String deliverFood(String foodItem) {
      try (Scope scope = tracer.buildSpan("deliverFood").startActive(true)) {
        String deliveryStatus = getHttp(tracer, client, 8083, "dasher/deliver", "foodItem", foodItem);
        scope.span().log(ImmutableMap.of("event", "delivery-status", "value", deliveryStatus));
        return deliveryStatus;
      }
    }
  }
}
