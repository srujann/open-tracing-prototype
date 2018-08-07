package lib;

import com.uber.jaeger.Configuration;
import com.uber.jaeger.Configuration.ReporterConfiguration;
import com.uber.jaeger.Configuration.SamplerConfiguration;
import com.uber.jaeger.samplers.ConstSampler;
import com.wavefront.opentracing.WavefrontTracer;

import com.wavefront.opentracing.reporting.Reporter;
import com.wavefront.opentracing.reporting.WavefrontTraceReporter;
import io.opentracing.Scope;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.tag.Tags;
import okhttp3.Request;

import javax.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class Tracing {
  private Tracing() {
  }

  public static Tracer init(String service) {
//    Reporter reporter = new WavefrontTraceReporter.Builder().
//      buildDirect("http://localhost:8080", "48b27465-75f0-4bd4-86ae-d4ae66f6034f");
    Reporter reporter = new WavefrontTraceReporter.Builder().
            withSource("wavefront-tracer").
            buildDirect("https://dev-2b.corp.wavefront.com", "23b96141-16fe-4905-ac8a-4b092f0e264e");
    return new WavefrontTracer.Builder().withReporter(reporter).build();
  }

  public static Scope startServerSpan(Tracer tracer, javax.ws.rs.core.HttpHeaders httpHeaders, String operationName) {
    // format the headers for extraction
    MultivaluedMap<String, String> rawHeaders = httpHeaders.getRequestHeaders();
    final HashMap<String, String> headers = new HashMap<>();
    for (String key : rawHeaders.keySet()) {
      headers.put(key, rawHeaders.get(key).get(0));
    }

    Tracer.SpanBuilder spanBuilder;
    try {
      SpanContext parentSpanCtx = tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers));
      if (parentSpanCtx == null) {
        spanBuilder = tracer.buildSpan(operationName);
      } else {
        spanBuilder = tracer.buildSpan(operationName).asChildOf(parentSpanCtx);
      }
    } catch (IllegalArgumentException e) {
      spanBuilder = tracer.buildSpan(operationName);
    }
    // TODO could add more tags like http.url
    return spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER).startActive(true);
  }

  public static TextMap requestBuilderCarrier(final Request.Builder builder) {
    return new TextMap() {
      @Override
      public Iterator<Map.Entry<String, String>> iterator() {
        throw new UnsupportedOperationException("carrier is write-only");
      }

      @Override
      public void put(String key, String value) {
        builder.addHeader(key, value);
      }
    };
  }
}
