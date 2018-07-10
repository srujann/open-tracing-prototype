package apps;

import com.google.common.collect.ImmutableMap;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import io.opentracing.Scope;
import io.opentracing.Tracer;
import io.opentracing.contrib.ClientTracingInterceptor;
import io.opentracing.contrib.OperationNameConstructor;
import kitchen.PrepareFoodRequest;
import kitchen.PrepareFoodResponse;
import kitchen.PrepareFoodServiceGrpc;
import lib.Tracing;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

public class Restaurant extends Application<ApplicationConfiguration> {

  private final Tracer tracer;

  private Restaurant(Tracer tracer) {
    this.tracer = tracer;
  }

  public static void main(String[] args) throws Exception {
    Tracer tracer = Tracing.init("restaurant");
    new Restaurant(tracer).run(args);
  }

  @Override
  public void run(ApplicationConfiguration configuration, Environment environment) throws Exception {
    environment.jersey().register(new RestaurantResource());
    environment.getApplicationContext().setContextPath("/restaurant");
  }

  private String prepareFoodRequest(String foodItem) {
    ManagedChannel channel = null;
    try {
      channel = ManagedChannelBuilder.forAddress("localhost", 8084).usePlaintext(true).build();
      ClientTracingInterceptor tracingInterceptor = new ClientTracingInterceptor.Builder(tracer).withOperationName(new OperationNameConstructor() {
        @Override
        public <ReqT, RespT> String constructOperationName(MethodDescriptor<ReqT, RespT> method) {
          return method.getFullMethodName();
        }
      }).build();
      PrepareFoodServiceGrpc.PrepareFoodServiceBlockingStub stub =
              PrepareFoodServiceGrpc.newBlockingStub(tracingInterceptor.intercept(channel));
      PrepareFoodResponse response = stub.prepare(PrepareFoodRequest.newBuilder().setFoodItem(foodItem).build());
      String status = response.getStatus();
      return status;
    } finally {
      if (channel != null) {
        channel.shutdown();
      }
    }
  }

  @Path("/restaurant/order")
  @Produces(MediaType.TEXT_PLAIN)
  public class RestaurantResource {

    @GET
    public String order(@QueryParam("foodItem") String foodItem, @Context HttpHeaders httpHeaders) throws InterruptedException {
      try (Scope scope = Tracing.startServerSpan(tracer, httpHeaders, "order")) {
        String customer = scope.span().getBaggageItem("customer");
        if (customer == null) {
          customer = "Unknown";
        }

        System.out.println("Sending prepare food request to kitchen service");
        String prepareFoodStatus = prepareFoodRequest(foodItem);
        System.out.println(prepareFoodStatus);
        scope.span().log(ImmutableMap.of("event", "restaurant-prepare-food-status", "value", prepareFoodStatus));
        String orderStatus = String.format("Ordered foodItem: %s for customer: %s is ready for delivery",
                foodItem, customer);
        System.out.println(orderStatus);
        scope.span().log(ImmutableMap.of("event", "order-status", "value", orderStatus));
        return orderStatus;
      }
    }
  }
}
