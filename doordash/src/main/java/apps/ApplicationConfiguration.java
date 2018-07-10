package apps;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Dropwizard configuration class
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
public class ApplicationConfiguration extends Configuration {
  @NotEmpty
  private String wavefrontReporterMechanism = "proxy";

  @NotEmpty
  private String host = "localhost";

  @NotEmpty
  private String port = "2878";

  private String wavefrontServer;
  private String token;

  @JsonProperty
  public String getWavefrontReporterMechanism() {
    return wavefrontReporterMechanism;
  }

  @JsonProperty
  public String getHost() {
    return host;
  }

  @JsonProperty
  public String getPort() {
    return port;
  }

  @JsonProperty
  public String getWavefrontServer() {
    return wavefrontServer;
  }

  @JsonProperty
  public String getToken() {
    return token;
  }
}
