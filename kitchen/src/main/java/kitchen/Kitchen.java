package kitchen;

import com.google.common.collect.ImmutableMap;
import io.grpc.Context;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.opentracing.References;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.OpenTracingContextKey;
import io.opentracing.contrib.ServerTracingInterceptor;
import lib.GrpcServiceConfiguration;
import lib.Tracing;
import lib.Utils;

import java.io.IOException;
import java.util.Random;

public class Kitchen {
  private final Tracer tracer = Tracing.init("Kitchen");

  public Kitchen(GrpcServiceConfiguration configuration) throws IOException, InterruptedException {
    ServerTracingInterceptor tracingInterceptor = new ServerTracingInterceptor(tracer);
    ServerBuilder builder = ServerBuilder.forPort(8084).
        addService(tracingInterceptor.intercept(new PrepareFoodServiceImpl()));
    Utils.configureWavefrontGrpcSdk(builder, configuration,
        PrepareFoodServiceGrpc.getServiceDescriptor());
    Server kitchen = builder.build();
    System.out.println("Starting Kitchen server ...");
    kitchen.start();
    System.out.println("Kitchen server started");
    kitchen.awaitTermination();
  }

  public static void main(String[] args) throws InterruptedException, IOException {
    GrpcServiceConfiguration configuration = Utils.scenarioFromFile(args[0]);
    new Kitchen(configuration);
  }

  public class PrepareFoodServiceImpl extends kitchen.PrepareFoodServiceGrpc.PrepareFoodServiceImplBase {

    private final Random random = new Random();

    @Override
    public void prepare(kitchen.PrepareFoodRequest request,
                        io.grpc.stub.StreamObserver<kitchen.PrepareFoodResponse> responseObserver) {
      // Warming up ...
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      Span prepareFoodSpan = OpenTracingContextKey.activeSpan();

      // Just realized - you need to cut some spare vegetables for next time ...
      new Thread(new CutSpareVegetables(prepareFoodSpan, () -> {
        for (int i = 0; i < random.nextInt(15); i++) {
          try {
            System.out.println("Cutting some spare vegetables just as a backup");
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
        String cutSpareVegetablesStatus = "Done with cutting some extra vegetables as a backup!";
        System.out.println(cutSpareVegetablesStatus);
        Span currentSpan = OpenTracingContextKey.activeSpan();
        currentSpan.log(ImmutableMap.of("event", "cut-spare-vegetables", "value", cutSpareVegetablesStatus));
      })).start();

      for (int i = 0; i < random.nextInt(5); i++) {
        System.out.println("Your foodItem: " + request.getFoodItem() + " is being prepared ...");
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      // Start with the cleanup process ...
      new Thread(new KitchenCleanupAfterCooking(prepareFoodSpan, () -> {
        for (int i = 0; i < random.nextInt(10); i++) {
          try {
            System.out.println("Cleaning after cooking ...");
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
        String cleanupAfterCookingStatus = "Done with cleaning after cooking!";
        System.out.println(cleanupAfterCookingStatus);
        Span currentSpan = OpenTracingContextKey.activeSpan();
        currentSpan.log(ImmutableMap.of("event", "cleanup-after-cooking", "value", cleanupAfterCookingStatus));
      })).start();

      // Wait for some more time ...
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      String prepareFoodStatus = "Your foodItem: " + request.getFoodItem() + " is ready!";
      System.out.println(prepareFoodStatus);

      prepareFoodSpan.log(ImmutableMap.of("event", "kitchen-prepare-food-status", "value", prepareFoodStatus));
      kitchen.PrepareFoodResponse response = kitchen.PrepareFoodResponse.newBuilder().setStatus(prepareFoodStatus).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    }

    class CutSpareVegetables implements Runnable {

      private final Runnable work;
      private final Span parentSpan;

      CutSpareVegetables(Span parentSpan, Runnable work) {
        this.parentSpan = parentSpan;
        this.work = work;
      }

      @Override
      public void run() {
        // TODO - right now child-of and follows-from does not make a lot of difference, figure out why ??
        /*
         * This should be child-of relationship instead of follows-from (since one of the thread can start cutting of
         * spare vegetables as soon chef starts preparing food)
         */
        Span cleanupSpan = tracer.buildSpan("backup-cut-spare-vegetables").asChildOf(parentSpan).start();
        // create a new context with the child span as the active span
        Context contextWithNewSpan = Context.current().withValue(OpenTracingContextKey.getKey(), cleanupSpan);
        // wrap the original work and run it
        Runnable tracedWork = contextWithNewSpan.wrap(this.work);
        tracedWork.run();
        cleanupSpan.finish();
      }
    }

    class KitchenCleanupAfterCooking implements Runnable {

      private final Runnable work;
      private final Span parentSpan;

      KitchenCleanupAfterCooking(Span parentSpan, Runnable work) {
        this.parentSpan = parentSpan;
        this.work = work;
      }

      @Override
      public void run() {
        /*
         * This should be follows-from instead of child-of relationship (since the cleanup happens after preparing food)
         */
        Span cleanupSpan = tracer.buildSpan("after-cooking-cleanup").
                addReference(References.FOLLOWS_FROM, parentSpan.context()).start();
        // create a new context with the child span as the active span
        Context contextWithNewSpan = Context.current().withValue(OpenTracingContextKey.getKey(), cleanupSpan);
        // wrap the original work and run it
        Runnable tracedWork = contextWithNewSpan.wrap(this.work);
        tracedWork.run();
        cleanupSpan.finish();
      }
    }
  }
}
