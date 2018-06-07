package lib;

import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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
}
