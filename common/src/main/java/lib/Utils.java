package lib;

import com.wavefront.integrations.Wavefront;
import com.wavefront.integrations.WavefrontDirectSender;
import com.wavefront.integrations.WavefrontSender;
import io.dropwizard.setup.Environment;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.wavefront.sdk.DropwizardApplicationReporter;
import com.wavefront.sdk.WavefrontDropwizardApplicationReporter;
import com.wavefront.sdk.WavefrontJerseyFilter;

import java.io.IOException;

public abstract class Utils {
  public static String getHttp(Tracer tracer, OkHttpClient client, int port, String pathSegments,
                               String param, String value) {
    try {
      HttpUrl url = new HttpUrl.Builder().scheme("http").host("localhost").port(port).
              addPathSegments(pathSegments).addQueryParameter(param, value).build();
      Request.Builder requestBuilder = new Request.Builder().url(url);

      Tags.SPAN_KIND.set(tracer.activeSpan(), Tags.SPAN_KIND_CLIENT);
      Tags.HTTP_METHOD.set(tracer.activeSpan(), "GET");
      Tags.HTTP_URL.set(tracer.activeSpan(), url.toString());
      tracer.inject(tracer.activeSpan().context(), Format.Builtin.HTTP_HEADERS, Tracing.requestBuilderCarrier(requestBuilder));

      Request request = requestBuilder.build();
      Response response = client.newCall(request).execute();
      if (response.code() != 200) {
        throw new RuntimeException("Bad HTTP result: " + response);
      }
      return response.body().string();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void configureWavefrontDropwizardSdk(
          ApplicationConfiguration configuration,
          Environment environment) {
    WavefrontSender wavefrontSender = null;
    switch (configuration.getWavefrontReporterMechanism()) {
      case "proxy":
        wavefrontSender = new Wavefront(configuration.getHost(), Integer.parseInt(configuration.getPort()));
        break;
      case "direct-ingestion":
        wavefrontSender = new WavefrontDirectSender(configuration.getWavefrontServer(), configuration.getToken());
        break;
      default:
        throw new IllegalArgumentException("Invalid Wavefront reporting mechanism: " + configuration.getWavefrontReporterMechanism());
    }

    DropwizardApplicationReporter wfAppReporter = new WavefrontDropwizardApplicationReporter.Builder(wavefrontSender).
            application(configuration.getApplication()).cluster(configuration.getCluster()).
            service(configuration.getService()).shard(configuration.getShard()).
            customTags(configuration.getTags()).build();
    environment.jersey().register(new WavefrontJerseyFilter(wfAppReporter, configuration.getCluster(),
            configuration.getService(), configuration.getShard()));
  }
}
